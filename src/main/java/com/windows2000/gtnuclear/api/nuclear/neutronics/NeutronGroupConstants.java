package com.windows2000.gtnuclear.api.nuclear.neutronics;

/**
 * Per-group (fast or thermal) one-node diffusion physics constants for a two-group {@link CMFDSolver}. Two
 * instances (fast, thermal) combine inside a {@link ReactorNode}, linked by that node's downscatter cross-section
 * -- the only coupling between the two groups in this model (thermal-to-fast upscattering is neglected, the
 * standard approximation for non-resonance-heated LWR-class spectra).
 * <p>
 * A record (2026-10-06, Java 25 upgrade pass) -- see {@link NodePosition}'s own JavaDoc for the general
 * reasoning. Component JavaDoc preserved below since records still support per-component doc comments.
 *
 * @param diffusionCoefficient Diffusion coefficient D (cm) for this group. Governs how readily this group's
 *                             neutrons leak between nodes.
 * @param absorption           Macroscopic absorption Sigma_a (cm^-1) for this group -- capture plus any fission
 *                             occurring at this group's energy, excluding downscatter out of the group
 *                             (downscatter is tracked separately on {@link ReactorNode}).
 * @param nuFission            nu*Sigma_f (cm^-1) for this group -- neutron production from fission occurring at
 *                             this group's energy. Zero for non-fissile nodes, or for a group with no fission at
 *                             that energy (e.g. many isotopes have negligible fast-fission cross-section, making
 *                             this zero for the fast group while nonzero for thermal).
 */
public record NeutronGroupConstants(double diffusionCoefficient, double absorption, double nuFission) {}
