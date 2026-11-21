package com.windows2000.gtnuclear.common.item;

/**
 * An item whose single shared "shape" texture (one per {@code Item} subclass, see each implementor's own
 * JavaDoc) is tinted at render time by {@link #getTintColor()} -- the same technique GregTech itself uses for
 * {@code MaterialIconSet}-based items (a grayscale template texture plus a runtime {@code IItemColor} tint,
 * confirmed via {@code MetaPrefixItem#getColorForItemStack}/{@code IGTTool#getColor} in GTST's own source,
 * rather than any pre-baked per-variant texture). Adding a new tinted variant of an existing shape never needs a
 * new texture or model file -- only a new {@link #getTintColor()} value.
 * <p>
 * Registered in bulk against {@code net.minecraftforge.client.event.ColorHandlerEvent.Item} -- see
 * {@code ClientProxy} -- rather than per-class, since every implementor shares the identical
 * {@code tintIndex == 0 ? getTintColor() : 0xFFFFFF} handler body.
 */
public interface ITintedItem {

    /** @return this item's tint color, {@code 0xRRGGBB} (alpha ignored), applied to {@code tintIndex} 0 only. */
    int getTintColor();
}
