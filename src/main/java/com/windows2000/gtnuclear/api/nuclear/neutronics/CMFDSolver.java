package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Coarse-Mesh Finite Difference (CMFD) k-eigenvalue neutron diffusion solver: "1 channel = 1 node", the standard
 * industry lightweight alternative to fine-mesh finite difference (see this mod's design memory for the comparison
 * against ray-traced/point-kernel and fine-mesh alternatives that led here). Solves the steady-state flux
 * <i>shape</i> and k-eff for one snapshot of reactor geometry; this is the "shape" half of the Quasi-Static method
 * this mod uses for time-domain behavior (the "amplitude" half is point kinetics, updated every tick separately --
 * this solver is only invoked when geometry/control-rod/moderator state actually changes, not every tick).
 * <p>
 * <b>Two-group diffusion:</b> fast and thermal neutrons are tracked as separate groups per {@link ReactorNode} (see
 * that class's own JavaDoc for the chi1=1/chi2=0 fission-spectrum assumption and the no-upscattering
 * simplification). This is a deliberate fidelity choice over a cheaper one-group treatment, made explicitly at the
 * user's request -- see this mod's design memory.
 * <p>
 * <b>Node coupling</b> uses the harmonic mean of adjacent nodes' diffusion coefficients divided by the node size
 * (the standard finite-difference diffusion formulation -- analogous to combining thermal/electrical resistances
 * in series): {@code coupling = 2*D_i*D_j / ((D_i+D_j) * nodeSize)}, computed independently per group since each
 * group has its own diffusion coefficient.
 * <p>
 * <b>Boundary condition:</b> vacuum. Any node face with no registered neighbor gets an explicit leakage loss term
 * (see {@link #vacuumBoundaryLoss}) using the standard transport-theory-corrected extrapolation distance
 * (z0 = 2.13*D) rather than simply omitting a coupling term -- omitting it entirely would be equivalent to a
 * reflective (zero-net-current) boundary, not a vacuum one. Computed independently per group.
 * <p>
 * <b>Solve method:</b> power iteration for the eigenvalue (k updated from the ratio of successive total fission
 * sources), combined with a single Jacobi sweep of the loss/coupling system per outer iteration -- a standard
 * "source iteration" scheme for diagonally-dominant diffusion systems, avoiding a separate nested inner solve.
 * Jacobi (all neighbor/self flux reads come from the previous iteration's snapshot, not from values already
 * updated earlier in the same sweep) is used instead of Gauss-Seidel specifically so that symmetric geometries
 * converge to exactly symmetric flux, independent of the arbitrary node iteration order. Each outer iteration
 * sweeps the fast group first (its balance never depends on the thermal group, since there is no upscattering),
 * then the thermal group using the fast group's just-updated flux as its inscatter source -- a directional
 * "group sweep" that stays order-independent because the entire fast-group array is finished before any
 * thermal-group value is touched. Converges when k's relative change drops below {@link #CONVERGENCE_TOLERANCE},
 * capped at {@link #MAX_ITERATIONS} as a fail-safe (logged as non-convergence, best-available result returned
 * rather than hanging) -- see this mod's design memory for why 350 was chosen.
 * <p>
 * <b>Internal representation (2026-10-06, Java 25 upgrade optimization pass):</b> the public {@link #solve}
 * contract is unchanged ({@link ReactorGeometry} in, a {@code Map<NodePosition, Double>}-based {@link CMFDResult}
 * out -- every other call site in this mod reads flux by {@link NodePosition} and stays untouched), but the
 * iteration loop itself -- run up to {@link #MAX_ITERATIONS} times per {@link #solve} call -- operates entirely on
 * flat {@code double[]}/{@code int[]} arrays internally. Node topology (neighbor indices) and per-group coupling
 * coefficients depend only on static node/geometry properties, not on the flux values the iteration actually
 * updates, so both are precomputed exactly once before the loop instead of being recomputed (along with a fresh
 * {@code HashMap} snapshot) on every single iteration as before. Converting to/from the position-keyed map
 * happens exactly once each, at the very start and end of {@link #solve}. This array layout is also what makes
 * {@link #sweepFastGroup}/{@link #sweepThermalGroup}/{@link #totalFissionSource} safe to run as a parallel
 * stream once node count passes {@link #PARALLEL_THRESHOLD_NODES} (see that constant's own JavaDoc).
 * <p>
 * <b>Vector API (SIMD) was deliberately rejected for this sweep (2026-10-06), not merely deferred:</b> each
 * node's neighbor list is a ragged gather over 1-6 non-contiguous indices (vacuum-boundary nodes have fewer
 * than 6 faces), which does not map cleanly onto a dense SIMD lane without either per-node masking or
 * restructuring {@link Topology} into fixed direction-indexed arrays -- extra complexity for a gain that, at
 * this mod's typical few-hundred-to-thousand-node reactor size, is dwarfed by {@code Vector} object overhead.
 * More importantly, {@code jdk.incubator.vector} is still an incubator module on Java 25 (JEP 489, tenth
 * incubation) requiring {@code --add-modules jdk.incubator.vector} at JVM launch; a mod jar has no way to add
 * that flag to a player's own CleanroomMC/Forge launcher profile, so using it here would crash with
 * {@code NoClassDefFoundError} for any player who has not manually edited their launch arguments.
 */
public final class CMFDSolver {

    public static final double CONVERGENCE_TOLERANCE = 1e-6;
    public static final int MAX_ITERATIONS = 350;

    /** Faces per cubic node -- von Neumann (face) connectivity, matching {@link ReactorGeometry}. */
    private static final int FACE_COUNT = 6;
    /**
     * Extrapolation-distance coefficient for the vacuum boundary condition, in units of the diffusion coefficient:
     * {@code z0 = TRANSPORT_EXTRAPOLATION_COEFFICIENT * D}. 2.13 is the standard transport-theory-corrected value
     * (z0 = 0.7104 * transport mean free path = 0.7104 * 3D), the textbook choice for a bare/vacuum diffusion
     * boundary -- see this mod's design memory for why this was chosen over the simpler uncorrected z0 = 2D.
     */
    private static final double TRANSPORT_EXTRAPOLATION_COEFFICIENT = 2.13;
    /**
     * Node count at/above which {@link #sweepFastGroup}/{@link #sweepThermalGroup}/{@link #totalFissionSource}
     * switch to a parallel stream instead of a plain sequential loop (2026-10-06, Java 25 upgrade optimization
     * pass). Jacobi's own contract (every node's update this sweep reads only from a {@code previous*} snapshot,
     * never from another node's value already written this same sweep -- see this class's own top-level JavaDoc)
     * makes the per-node work embarrassingly parallel with no synchronization needed. The threshold exists
     * because that parallelism is not free: splitting work across the shared common {@code ForkJoinPool} and
     * joining the results costs more than this mod's typical reactor (a few hundred to roughly a thousand nodes)
     * would ever recoup from running its own simple per-node arithmetic across multiple cores. 2000 is a
     * conservative, not empirically-tuned, estimate of where that crossover plausibly sits -- intended to help
     * only once a much larger player-built reactor (this mod's own size cap allows up to a 31-diameter, many-
     * layer core, well into the tens of thousands of nodes) makes the solve itself noticeably expensive.
     */
    private static final int PARALLEL_THRESHOLD_NODES = 2000;

    private CMFDSolver() {}

    @NotNull
    public static CMFDResult solve(@NotNull ReactorGeometry geometry) {
        List<ReactorNode> nodes = new ArrayList<>(geometry.getNodes());
        int nodeCount = nodes.size();
        if (nodeCount == 0) {
            return new CMFDResult(0.0, Collections.emptyMap(), Collections.emptyMap(), true, 0);
        }

        Topology topology = Topology.build(geometry, nodes);

        double[] fastFlux = new double[nodeCount];
        double[] thermalFlux = new double[nodeCount];
        Arrays.fill(fastFlux, 1.0);
        Arrays.fill(thermalFlux, 1.0);
        double[] previousFast = new double[nodeCount];
        double[] previousThermal = new double[nodeCount];

        double k = 1.0;
        double previousFissionSource = totalFissionSource(topology, fastFlux, thermalFlux);
        boolean converged = false;
        int iteration = 0;

        for (; iteration < MAX_ITERATIONS; iteration++) {
            System.arraycopy(fastFlux, 0, previousFast, 0, nodeCount);
            sweepFastGroup(topology, fastFlux, previousFast, thermalFlux, k);
            System.arraycopy(thermalFlux, 0, previousThermal, 0, nodeCount);
            sweepThermalGroup(topology, thermalFlux, previousThermal, fastFlux);

            double newFissionSource = totalFissionSource(topology, fastFlux, thermalFlux);
            double newK = previousFissionSource > 0 ? k * (newFissionSource / previousFissionSource) : k;
            double relativeChange = Math.abs(newK - k) / Math.max(Math.abs(k), 1e-12);

            k = newK;
            previousFissionSource = newFissionSource;

            if (relativeChange < CONVERGENCE_TOLERANCE) {
                converged = true;
                iteration++;
                break;
            }
        }

        normalize(fastFlux, thermalFlux);

        Map<NodePosition, Double> fastFluxMap = new HashMap<>(nodeCount * 2);
        Map<NodePosition, Double> thermalFluxMap = new HashMap<>(nodeCount * 2);
        for (int i = 0; i < nodeCount; i++) {
            fastFluxMap.put(nodes.get(i).position(), fastFlux[i]);
            thermalFluxMap.put(nodes.get(i).position(), thermalFlux[i]);
        }
        return new CMFDResult(k, fastFluxMap, thermalFluxMap, converged, iteration);
    }

    /**
     * Everything the iteration loop needs that depends only on static node/geometry properties -- neighbor
     * topology and per-group coupling/self-removal terms -- precomputed exactly once per {@link #solve} call
     * rather than recomputed (and re-looked-up from {@link ReactorGeometry}'s own position-keyed storage) on
     * every one of up to {@link #MAX_ITERATIONS} iterations, as the pre-2026-10-06 implementation did.
     */
    private static final class Topology {

        final int nodeCount;
        final int[][] neighborIndices;
        final double[][] fastCouplings;
        final double[][] thermalCouplings;
        /** {@code absorption + downscatter + couplingSum + boundaryLoss} -- the fast-group Jacobi denominator. */
        final double[] fastSelfTerm;
        /** {@code absorption + couplingSum + boundaryLoss} -- the thermal-group Jacobi denominator. */
        final double[] thermalSelfTerm;
        final double[] fastNuFission;
        final double[] thermalNuFission;
        final double[] downscatter;

        private Topology(int nodeCount, int[][] neighborIndices, double[][] fastCouplings,
                         double[][] thermalCouplings, double[] fastSelfTerm, double[] thermalSelfTerm,
                         double[] fastNuFission, double[] thermalNuFission, double[] downscatter) {
            this.nodeCount = nodeCount;
            this.neighborIndices = neighborIndices;
            this.fastCouplings = fastCouplings;
            this.thermalCouplings = thermalCouplings;
            this.fastSelfTerm = fastSelfTerm;
            this.thermalSelfTerm = thermalSelfTerm;
            this.fastNuFission = fastNuFission;
            this.thermalNuFission = thermalNuFission;
            this.downscatter = downscatter;
        }

        @NotNull
        static Topology build(@NotNull ReactorGeometry geometry, @NotNull List<ReactorNode> nodes) {
            int nodeCount = nodes.size();
            Map<NodePosition, Integer> indexByPosition = new HashMap<>(nodeCount * 2);
            for (int i = 0; i < nodeCount; i++) indexByPosition.put(nodes.get(i).position(), i);

            int[][] neighborIndices = new int[nodeCount][];
            double[][] fastCouplings = new double[nodeCount][];
            double[][] thermalCouplings = new double[nodeCount][];
            double[] fastSelfTerm = new double[nodeCount];
            double[] thermalSelfTerm = new double[nodeCount];
            double[] fastNuFission = new double[nodeCount];
            double[] thermalNuFission = new double[nodeCount];
            double[] downscatter = new double[nodeCount];

            for (int i = 0; i < nodeCount; i++) {
                ReactorNode node = nodes.get(i);
                List<ReactorNode> neighbors = geometry.getNeighbors(node);
                int neighborCount = neighbors.size();
                int[] indices = new int[neighborCount];
                double[] fastC = new double[neighborCount];
                double[] thermalC = new double[neighborCount];
                double fastCouplingSum = 0.0;
                double thermalCouplingSum = 0.0;
                for (int j = 0; j < neighborCount; j++) {
                    ReactorNode neighbor = neighbors.get(j);
                    indices[j] = indexByPosition.get(neighbor.position());
                    double fastCoupling = harmonicCoupling(node.fast().diffusionCoefficient(),
                            neighbor.fast().diffusionCoefficient(), geometry.nodeSizeCm);
                    double thermalCoupling = harmonicCoupling(node.thermal().diffusionCoefficient(),
                            neighbor.thermal().diffusionCoefficient(), geometry.nodeSizeCm);
                    fastC[j] = fastCoupling;
                    thermalC[j] = thermalCoupling;
                    fastCouplingSum += fastCoupling;
                    thermalCouplingSum += thermalCoupling;
                }
                neighborIndices[i] = indices;
                fastCouplings[i] = fastC;
                thermalCouplings[i] = thermalC;

                int vacuumFaces = FACE_COUNT - neighborCount;
                double fastBoundaryLoss = vacuumFaces *
                        vacuumBoundaryLoss(node.fast().diffusionCoefficient(), geometry.nodeSizeCm);
                double thermalBoundaryLoss = vacuumFaces *
                        vacuumBoundaryLoss(node.thermal().diffusionCoefficient(), geometry.nodeSizeCm);

                fastSelfTerm[i] = node.fast().absorption() + node.downscatterCrossSection() + fastCouplingSum +
                        fastBoundaryLoss;
                thermalSelfTerm[i] = node.thermal().absorption() + thermalCouplingSum + thermalBoundaryLoss;
                fastNuFission[i] = node.fast().nuFission();
                thermalNuFission[i] = node.thermal().nuFission();
                downscatter[i] = node.downscatterCrossSection();
            }

            return new Topology(nodeCount, neighborIndices, fastCouplings, thermalCouplings, fastSelfTerm,
                    thermalSelfTerm, fastNuFission, thermalNuFission, downscatter);
        }
    }

    /**
     * Jacobi sweep of the fast group: production comes from total fission source (both groups, chi1=1) divided by
     * k, removal includes both fast absorption and downscatter out to the thermal group. Reads only
     * {@code previousFast} (this iteration's own snapshot, taken before this sweep ran) for both coupling and the
     * node's own self term, and {@code thermalFlux} as it stood at the end of the <i>previous</i> full iteration
     * (not yet updated this iteration), so it never depends on this same outer iteration's not-yet-computed
     * thermal update.
     */
    private static void sweepFastGroup(@NotNull Topology topology, double[] fastFlux, double[] previousFast,
                                       double[] thermalFlux, double k) {
        nodeIndices(topology.nodeCount).forEach(i -> {
            int[] neighbors = topology.neighborIndices[i];
            double[] couplings = topology.fastCouplings[i];
            double neighborFluxSum = 0.0;
            for (int j = 0; j < neighbors.length; j++) {
                neighborFluxSum += couplings[j] * previousFast[neighbors[j]];
            }

            double fissionSource = (topology.fastNuFission[i] * previousFast[i] +
                    topology.thermalNuFission[i] * thermalFlux[i]) / k;
            double denominator = topology.fastSelfTerm[i];
            double newFlux = denominator > 0 ? (fissionSource + neighborFluxSum) / denominator : 0.0;
            fastFlux[i] = Math.max(newFlux, 0.0);
        });
    }

    /**
     * Jacobi sweep of the thermal group: production comes solely from downscatter out of the fast group (no direct
     * fission source, chi2=0), using the fast group's just-completed update for this same outer iteration as the
     * inscatter source. Coupling/self reads for the thermal group itself still use {@code previousThermal} (the
     * snapshot taken before this sweep ran), matching the Jacobi contract.
     */
    private static void sweepThermalGroup(@NotNull Topology topology, double[] thermalFlux, double[] previousThermal,
                                          double[] fastFlux) {
        nodeIndices(topology.nodeCount).forEach(i -> {
            int[] neighbors = topology.neighborIndices[i];
            double[] couplings = topology.thermalCouplings[i];
            double neighborFluxSum = 0.0;
            for (int j = 0; j < neighbors.length; j++) {
                neighborFluxSum += couplings[j] * previousThermal[neighbors[j]];
            }

            double inscatterSource = topology.downscatter[i] * fastFlux[i];
            double denominator = topology.thermalSelfTerm[i];
            double newFlux = denominator > 0 ? (inscatterSource + neighborFluxSum) / denominator : 0.0;
            thermalFlux[i] = Math.max(newFlux, 0.0);
        });
    }

    /**
     * @return an {@link IntStream} over {@code [0, nodeCount)}, parallel once {@code nodeCount} reaches
     *         {@link #PARALLEL_THRESHOLD_NODES} and sequential below it -- see that constant's own JavaDoc.
     */
    @NotNull
    private static IntStream nodeIndices(int nodeCount) {
        IntStream stream = IntStream.range(0, nodeCount);
        return nodeCount >= PARALLEL_THRESHOLD_NODES ? stream.parallel() : stream;
    }

    private static double harmonicCoupling(double diffusionCoefficientA, double diffusionCoefficientB,
                                           double nodeSizeCm) {
        if (diffusionCoefficientA + diffusionCoefficientB <= 0) return 0.0;
        return 2.0 * diffusionCoefficientA * diffusionCoefficientB /
                ((diffusionCoefficientA + diffusionCoefficientB) * nodeSizeCm);
    }

    /**
     * Per-face leakage conductance to a zero-flux exterior at the extrapolated boundary distance
     * {@code nodeSize/2 + z0}: the node's own half-width to reach its physical surface, plus the extrapolation
     * distance beyond it.
     */
    private static double vacuumBoundaryLoss(double diffusionCoefficient, double nodeSizeCm) {
        if (diffusionCoefficient <= 0) return 0.0;
        double extrapolationDistance = TRANSPORT_EXTRAPOLATION_COEFFICIENT * diffusionCoefficient;
        return diffusionCoefficient / (nodeSizeCm / 2.0 + extrapolationDistance);
    }

    private static double totalFissionSource(@NotNull Topology topology, double[] fastFlux, double[] thermalFlux) {
        return nodeIndices(topology.nodeCount)
                .mapToDouble(i -> topology.fastNuFission[i] * fastFlux[i] + topology.thermalNuFission[i] *
                        thermalFlux[i])
                .sum();
    }

    /**
     * Rescales both groups' flux by the same factor so the average <i>total</i> (fast+thermal) flux across all
     * nodes is 1 -- a display/consistency convenience only, preserving each node's fast/thermal spectral ratio.
     * k-eff and the relative flux <i>shape</i> are invariant to any positive rescaling of the flux vectors, since
     * the diffusion equations solved here are linear and homogeneous in flux.
     */
    private static void normalize(double[] fastFlux, double[] thermalFlux) {
        double sum = 0.0;
        for (int i = 0; i < fastFlux.length; i++) sum += fastFlux[i] + thermalFlux[i];
        if (sum <= 0) return;
        double factor = fastFlux.length / sum;
        for (int i = 0; i < fastFlux.length; i++) {
            fastFlux[i] *= factor;
            thermalFlux[i] *= factor;
        }
    }
}
