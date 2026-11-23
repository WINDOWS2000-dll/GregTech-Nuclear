package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FuelAssemblyPortTextTest {

    @Test
    void formats() {
        assertEquals("17 (3.66 m)", FuelAssemblyPortText.length(17));
        assertEquals("4.50%", FuelAssemblyPortText.enrichment(0.045));
        assertEquals("82%", FuelAssemblyPortText.percent(0.823));
        assertEquals("12.3 kg", FuelAssemblyPortText.kilograms(12.34));
        assertEquals("7.43 kg", FuelAssemblyPortText.kilograms(7.43));
        assertEquals("0.012 kg", FuelAssemblyPortText.kilograms(0.0123));
        assertEquals("-2850 pcm", FuelAssemblyPortText.pcm(-2850.4));
        assertEquals("1.12", FuelAssemblyPortText.relativePower(1.1234));
        assertEquals("580 K", FuelAssemblyPortText.temperature(580.2));
    }

    @Test
    void unknownValuesShowPlaceholder() {
        assertEquals("---", FuelAssemblyPortText.temperature(Double.NaN));
        assertEquals("---", FuelAssemblyPortText.pcm(Double.NaN));
        assertEquals("---", FuelAssemblyPortText.relativePower(Double.NaN));
    }
}
