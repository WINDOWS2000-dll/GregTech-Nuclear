package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

class CMFDSolverTest {

    private static final double EPSILON = 1e-4;

    private static double vacuumBoundaryLoss(double diffusionCoefficient, double nodeSizeCm) {
        double extrapolationDistance = 2.13 * diffusionCoefficient;
        return diffusionCoefficient / (nodeSizeCm / 2.0 + extrapolationDistance);
    }

    @Test
    void emptyGeometryDoesNotCrash() {
        ReactorGeometry geometry = new ReactorGeometry(20.0);
        CMFDResult result = CMFDSolver.solve(geometry);
        assertThat(result.converged(), is(true));
        assertThat(result.fastFlux().size(), is(0));
        assertThat(result.thermalFlux().size(), is(0));
    }

    @Test
    void isolatedFissileNodeLeaksThroughAllSixVacuumFacesInBothGroups() {
        // No neighbors => all 6 faces are vacuum boundaries in both groups. Closed-form two-group k-infinity-with-
        // leakage: solve the thermal balance for phi2 in terms of phi1 (only source is fast->thermal downscatter),
        // substitute into the fast balance (production from both groups' fission, chi1=1), and solve for k.
        ReactorGeometry geometry = new ReactorGeometry(20.0);
        double nodeSize = geometry.nodeSizeCm;

        double fastD = 1.5, fastAbsorption = 0.01, fastNuFission = 0.002;
        double thermalD = 0.3, thermalAbsorption = 0.1, thermalNuFission = 0.15;
        double downscatter = 0.05;

        NeutronGroupConstants fast = new NeutronGroupConstants(fastD, fastAbsorption, fastNuFission);
        NeutronGroupConstants thermal = new NeutronGroupConstants(thermalD, thermalAbsorption, thermalNuFission);
        NodePosition position = new NodePosition(0, 0, 0);
        geometry.put(new ReactorNode(position, fast, thermal, downscatter));

        CMFDResult result = CMFDSolver.solve(geometry);

        double fastBoundaryLoss = 6 * vacuumBoundaryLoss(fastD, nodeSize);
        double thermalBoundaryLoss = 6 * vacuumBoundaryLoss(thermalD, nodeSize);

        // phi2 / phi1 = downscatter / (thermalAbsorption + thermalBoundaryLoss)
        double phi2OverPhi1 = downscatter / (thermalAbsorption + thermalBoundaryLoss);
        double expectedK = (fastNuFission + thermalNuFission * phi2OverPhi1) /
                (fastAbsorption + downscatter + fastBoundaryLoss);

        assertThat(result.converged(), is(true));
        assertThat(result.kEffective(), closeTo(expectedK, EPSILON));
    }

    @Test
    void nonFissileGeometryConvergesTriviallyWithoutCrashing() {
        ReactorGeometry geometry = new ReactorGeometry(20.0);
        NeutronGroupConstants fast = new NeutronGroupConstants(0.16, 0.02, 0.0);
        NeutronGroupConstants thermal = new NeutronGroupConstants(0.3, 0.05, 0.0);
        geometry.put(new ReactorNode(new NodePosition(0, 0, 0), fast, thermal, 0.01));
        geometry.put(new ReactorNode(new NodePosition(1, 0, 0), fast, thermal, 0.01));

        CMFDResult result = CMFDSolver.solve(geometry);

        assertThat(result.converged(), is(true));
        // No fission source anywhere: k is left at its initial guess (1.0), not a meaningful multiplication
        // factor, but must not be NaN/negative/exceptional.
        assertThat(result.kEffective(), closeTo(1.0, EPSILON));
    }

    @Test
    void zeroDownscatterProducesNoThermalFlux() {
        // With no fast->thermal coupling, nothing feeds the thermal group's balance (chi2=0, no other source), so
        // its flux must converge to exactly zero regardless of the fast group's (self-sustaining) fission chain.
        ReactorGeometry geometry = new ReactorGeometry(20.0);
        NeutronGroupConstants fast = new NeutronGroupConstants(1.5, 0.05, 0.1);
        NeutronGroupConstants thermal = new NeutronGroupConstants(0.3, 0.1, 0.15);
        NodePosition position = new NodePosition(0, 0, 0);
        geometry.put(new ReactorNode(position, fast, thermal, 0.0));

        CMFDResult result = CMFDSolver.solve(geometry);

        assertThat(result.converged(), is(true));
        assertThat(result.thermalFlux().get(position), closeTo(0.0, EPSILON));
    }

    @Test
    void leakageReducesKBelowInfiniteMediumValue() {
        // A single fuel node surrounded by vacuum on all sides but one, coupled to a second identical fuel node:
        // even with the shared face's leakage cancelled by symmetry, the remaining 5 vacuum faces per node (in each
        // group) still leak, so k-eff must stay below the coupling-free "no boundary loss at all" ceiling.
        NeutronGroupConstants fast = new NeutronGroupConstants(1.5, 0.01, 0.002);
        NeutronGroupConstants thermal = new NeutronGroupConstants(0.3, 0.1, 0.15);
        double downscatter = 0.05;

        ReactorGeometry geometry = new ReactorGeometry(20.0);
        geometry.put(new ReactorNode(new NodePosition(0, 0, 0), fast, thermal, downscatter));
        geometry.put(new ReactorNode(new NodePosition(1, 0, 0), fast, thermal, downscatter));

        CMFDResult result = CMFDSolver.solve(geometry);

        double phi2OverPhi1NoLeakage = downscatter / thermal.absorption();
        double kInfinityNoLeakageAtAll = (fast.nuFission() + thermal.nuFission() * phi2OverPhi1NoLeakage) /
                (fast.absorption() + downscatter);

        assertThat(result.converged(), is(true));
        assertThat(result.kEffective(), lessThan(kInfinityNoLeakageAtAll));
        assertThat(result.kEffective(), greaterThan(0.0));
    }

    @Test
    void symmetricTwoNodeGeometryHasEqualFluxInBothGroups() {
        ReactorGeometry geometry = new ReactorGeometry(20.0);
        NeutronGroupConstants fast = new NeutronGroupConstants(1.5, 0.01, 0.002);
        NeutronGroupConstants thermal = new NeutronGroupConstants(0.3, 0.1, 0.15);
        NodePosition posA = new NodePosition(0, 0, 0);
        NodePosition posB = new NodePosition(1, 0, 0);
        geometry.put(new ReactorNode(posA, fast, thermal, 0.05));
        geometry.put(new ReactorNode(posB, fast, thermal, 0.05));

        CMFDResult result = CMFDSolver.solve(geometry);

        assertThat(result.fastFlux().get(posA), closeTo(result.fastFlux().get(posB), EPSILON));
        assertThat(result.thermalFlux().get(posA), closeTo(result.thermalFlux().get(posB), EPSILON));
    }

    @Test
    void absorbingNeighborReducesKComparedToFissileNeighbor() {
        NeutronGroupConstants fast = new NeutronGroupConstants(1.5, 0.01, 0.002);
        NeutronGroupConstants thermal = new NeutronGroupConstants(0.3, 0.1, 0.15);
        double downscatter = 0.05;

        ReactorGeometry withFuelNeighbor = new ReactorGeometry(20.0);
        withFuelNeighbor.put(new ReactorNode(new NodePosition(0, 0, 0), fast, thermal, downscatter));
        withFuelNeighbor.put(new ReactorNode(new NodePosition(1, 0, 0), fast, thermal, downscatter));
        double kWithFuelNeighbor = CMFDSolver.solve(withFuelNeighbor).kEffective();

        // A strong neutron absorber (e.g. an inserted control rod) in place of the second fuel node.
        NeutronGroupConstants absorberFast = new NeutronGroupConstants(0.3, 5.0, 0.0);
        NeutronGroupConstants absorberThermal = new NeutronGroupConstants(0.2, 5.0, 0.0);

        ReactorGeometry withAbsorberNeighbor = new ReactorGeometry(20.0);
        withAbsorberNeighbor.put(new ReactorNode(new NodePosition(0, 0, 0), fast, thermal, downscatter));
        withAbsorberNeighbor.put(new ReactorNode(new NodePosition(1, 0, 0), absorberFast, absorberThermal, 0.0));
        double kWithAbsorberNeighbor = CMFDSolver.solve(withAbsorberNeighbor).kEffective();

        assertThat(kWithAbsorberNeighbor, lessThan(kWithFuelNeighbor));
    }
}
