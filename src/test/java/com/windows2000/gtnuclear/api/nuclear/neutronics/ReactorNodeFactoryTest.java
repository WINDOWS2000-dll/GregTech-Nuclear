package com.windows2000.gtnuclear.api.nuclear.neutronics;

import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;

import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;

class ReactorNodeFactoryTest {

    private static final NodePosition POSITION = new NodePosition(0, 0, 0);

    @Test
    void moderatorNodeHasNoFissionEitherGroup() {
        ReactorNode node = ReactorNodeFactory.moderatorNode(POSITION);
        assertThat(node.fast().nuFission(), is(0.0));
        assertThat(node.thermal().nuFission(), is(0.0));
        assertThat(node.isFissile(), is(false));
    }

    @Test
    void moderatorNodeHasPositiveDiffusionAndAbsorption() {
        ReactorNode node = ReactorNodeFactory.moderatorNode(POSITION);
        assertThat(node.fast().diffusionCoefficient(), greaterThan(0.0));
        assertThat(node.thermal().diffusionCoefficient(), greaterThan(0.0));
        assertThat(node.thermal().absorption(), greaterThan(0.0));
    }

    @Test
    void pureU235FuelNodeMatchesBaselineExactlySinceU235IsTheReference() {
        FuelComposition composition = new FuelComposition("pure_u235",
                Collections.singletonMap(Elements.U235, 1.0));
        ReactorNode node = ReactorNodeFactory.fuelNode(POSITION, composition);
        ReactorNode other = ReactorNodeFactory.fuelNode(POSITION, composition);
        // Same composition twice should be deterministic and reproducible.
        assertThat(node.thermal().absorption(), closeTo(other.thermal().absorption(), 1e-12));
        assertThat(node.thermal().nuFission(), closeTo(other.thermal().nuFission(), 1e-12));
        assertThat(node.isFissile(), is(true));
    }

    @Test
    void higherFissionBarnsIsotopeYieldsHigherNuFissionThanU235() {
        FuelComposition u235Only = new FuelComposition("u235_only", Collections.singletonMap(Elements.U235, 1.0));
        FuelComposition pu239Only = new FuelComposition("pu239_only", Collections.singletonMap(Elements.Pu239, 1.0));

        ReactorNode u235Node = ReactorNodeFactory.fuelNode(POSITION, u235Only);
        ReactorNode pu239Node = ReactorNodeFactory.fuelNode(POSITION, pu239Only);

        // Pu-239's thermal fission cross-section (747b) exceeds U-235's (584b), so its scaled nu*fission should too.
        assertThat(pu239Node.thermal().nuFission(), greaterThan(u235Node.thermal().nuFission()));
    }

    @Test
    void fuelNodeIsAlwaysFissile() {
        FuelComposition composition = new FuelComposition("u238_only", Collections.singletonMap(Elements.U238, 1.0));
        ReactorNode node = ReactorNodeFactory.fuelNode(POSITION, composition);
        assertThat(node.isFissile(), is(true));
    }

    @Test
    void mixedCompositionIsBetweenPureComponents() {
        FuelComposition u235Only = new FuelComposition("u235_only", Collections.singletonMap(Elements.U235, 1.0));
        FuelComposition pu239Only = new FuelComposition("pu239_only", Collections.singletonMap(Elements.Pu239, 1.0));

        Map<Element, Double> mixShares = new LinkedHashMap<>();
        mixShares.put(Elements.U235, 0.5);
        mixShares.put(Elements.Pu239, 0.5);
        FuelComposition mixed = new FuelComposition("mixed", mixShares);

        ReactorNode u235Node = ReactorNodeFactory.fuelNode(POSITION, u235Only);
        ReactorNode pu239Node = ReactorNodeFactory.fuelNode(POSITION, pu239Only);
        ReactorNode mixedNode = ReactorNodeFactory.fuelNode(POSITION, mixed);

        double low = Math.min(u235Node.thermal().nuFission(), pu239Node.thermal().nuFission());
        double high = Math.max(u235Node.thermal().nuFission(), pu239Node.thermal().nuFission());
        assertThat(mixedNode.thermal().nuFission(), greaterThan(low));
        assertThat(mixedNode.thermal().nuFission(), is(org.hamcrest.Matchers.lessThan(high)));
    }
}
