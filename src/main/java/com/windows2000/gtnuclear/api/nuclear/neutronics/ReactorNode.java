package com.windows2000.gtnuclear.api.nuclear.neutronics;

import org.jetbrains.annotations.NotNull;

/**
 * A single CMFD node: two-group (fast + thermal) diffusion physics parameters for one "1 channel/coarse cell = 1
 * node" position. See {@link CMFDSolver}'s own JavaDoc for how these combine into a k-eigenvalue diffusion solve.
 * <p>
 * Fission neutrons are born fast regardless of which group's flux caused the fission (chi1=1, chi2=0, the standard
 * assumption at this fidelity level), so fission source from either group feeds only the fast group's production
 * term. The fast group loses neutrons to the thermal group via {@code downscatterCrossSection} (moderation); there
 * is no reverse (thermal-to-fast) upscattering term in this model.
 * <p>
 * A record (2026-10-06, Java 25 upgrade pass) -- see {@link NodePosition}'s own JavaDoc for the general
 * reasoning.
 *
 * @param downscatterCrossSection Fast-to-thermal downscatter (moderation) cross-section Sigma_s,1->2 (cm^-1).
 */
public record ReactorNode(@NotNull NodePosition position, @NotNull NeutronGroupConstants fast,
                          @NotNull NeutronGroupConstants thermal, double downscatterCrossSection) {

    public boolean isFissile() {
        return fast.nuFission() > 0 || thermal.nuFission() > 0;
    }
}
