package com.windows2000.gtnuclear.api.nuclear.neutronics;

/**
 * Control rod reactivity worth as a function of insertion depth (0.0 = fully withdrawn, 1.0 = fully inserted),
 * using the standard cosine-shaped integral rod worth curve from reactor physics:
 * {@code rho(d) = -(maxWorth / 2) * (1 - cos(pi * d))}.
 * <p>
 * This reproduces the real, well-known shape of a control rod's worth curve: differential worth (the reactivity
 * change per unit of further insertion) is greatest around the rod's mid-travel point and smallest at both
 * extremes (fully withdrawn or fully inserted), unlike a linear approximation. {@code maxWorth} is the total
 * reactivity magnitude of one single fully-inserted rod; each control rod controller's own insertion depth is
 * fed through this same shared shape, and their individual contributions are simply summed by the caller to get
 * the reactor's total control rod reactivity -- mirroring how this mod's fuel-composition-weighted delayed
 * neutron data ({@link PointKineticsConstants}) reuses one shared shape scaled per isotope, rather than each
 * needing its own bespoke curve.
 */
public final class ControlRodWorth {

    private ControlRodWorth() {}

    /**
     * @param insertionDepth clamped to [0.0, 1.0]
     * @param maxWorth       reactivity magnitude of this rod when fully inserted (&gt;= 0)
     * @return the reactivity contribution of one control rod (always &lt;= 0, a control rod is a neutron
     *         absorber)
     */
    public static double reactivity(double insertionDepth, double maxWorth) {
        double clampedDepth = Math.max(0.0, Math.min(1.0, insertionDepth));
        return -(maxWorth / 2.0) * (1.0 - Math.cos(Math.PI * clampedDepth));
    }
}
