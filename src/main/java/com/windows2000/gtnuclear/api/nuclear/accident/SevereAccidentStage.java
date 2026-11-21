package com.windows2000.gtnuclear.api.nuclear.accident;

/**
 * The staged severe accident progression (see this mod's design memory for the full derivation):
 * structural damage or an unresolved post-SCRAM LOCA leads to {@link #UNCONTROLLED_HEATING}; if fuel temperature
 * then exceeds its melting point, {@link #MELTDOWN} follows; sufficient further melt progression or direct
 * molten-fuel/coolant contact triggers {@link #EXPLOSION}.
 */
public enum SevereAccidentStage {
    NORMAL,
    UNCONTROLLED_HEATING,
    MELTDOWN,
    EXPLOSION
}
