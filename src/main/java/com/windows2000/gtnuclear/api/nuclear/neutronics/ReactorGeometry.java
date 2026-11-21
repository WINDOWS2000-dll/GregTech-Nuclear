package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A sparse collection of {@link ReactorNode}s keyed by position, with 6-neighbor (von Neumann/face-adjacent)
 * connectivity -- the coarse mesh a {@link CMFDSolver} solves over.
 * <p>
 * <b>Boundary condition:</b> a position with no registered node is treated as vacuum -- it simply contributes no
 * coupling term (see {@link CMFDSolver}), which is equivalent to neutrons freely leaking out at that face and
 * never returning. A future "neutron reflector" casing material could special-case this (partial albedo instead of
 * full vacuum) without changing this class's shape.
 */
public final class ReactorGeometry {

    private static final int[][] FACE_OFFSETS = {
            { 1, 0, 0 }, { -1, 0, 0 },
            { 0, 1, 0 }, { 0, -1, 0 },
            { 0, 0, 1 }, { 0, 0, -1 }
    };

    private final Map<NodePosition, ReactorNode> nodes = new LinkedHashMap<>();
    /** Side length of a cubic node (cm) -- used as the coupling distance in the harmonic-mean diffusion formula. */
    public final double nodeSizeCm;

    public ReactorGeometry(double nodeSizeCm) {
        this.nodeSizeCm = nodeSizeCm;
    }

    public void put(@NotNull ReactorNode node) {
        nodes.put(node.position(), node);
    }

    @Nullable
    public ReactorNode get(@NotNull NodePosition position) {
        return nodes.get(position);
    }

    @NotNull
    public Collection<ReactorNode> getNodes() {
        return nodes.values();
    }

    public int size() {
        return nodes.size();
    }

    /**
     * @return the (up to 6) existing neighboring nodes. A face with no registered node there is a vacuum boundary,
     *         not a neighbor -- it contributes nothing to the returned list.
     */
    @NotNull
    public List<ReactorNode> getNeighbors(@NotNull ReactorNode node) {
        List<ReactorNode> result = new ArrayList<>(6);
        for (int[] offset : FACE_OFFSETS) {
            ReactorNode neighbor = nodes.get(node.position().offset(offset[0], offset[1], offset[2]));
            if (neighbor != null) result.add(neighbor);
        }
        return result;
    }
}
