package com.windows2000.gtnuclear.common.metatileentities.storage;

import gregtech.api.GregTechAPI;
import gregtech.api.items.itemhandlers.GTItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.mui.GTGuis;
import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;
import gregtech.api.unification.OreDictUnifier;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.ore.OrePrefix;
import gregtech.api.unification.stack.MaterialStack;
import gregtech.client.renderer.texture.Textures;
import gregtech.client.renderer.texture.cube.SimpleSidedCubeRenderer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.BooleanSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.SlotGroupWidget;
import com.cleanroommc.modularui.widgets.layout.Grid;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.windows2000.gtnuclear.api.nuclear.decay.DecayEngine;
import com.windows2000.gtnuclear.api.nuclear.decay.TimeCompression;
import com.windows2000.gtnuclear.api.util.GTNLLogger;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * First (simple, single-block) implementation of a "decay only happens here" storage machine (e.g. a stand-in for
 * a future spent fuel pool / waste tank), per this mod's design decision (see design memory
 * project_gtnuclear_phase1_scope). Ordinary dust/ingot items sitting in a chest or player inventory never decay --
 * only the composition tracked <i>inside this machine</i> does.
 * <p>
 * Holds a nuclide composition ({@code Map<Element, Double>}, amount in "1 unit per inserted item") plus the world
 * tick it was last advanced to. Inserting an item consumes it and adds its full count to the pool; the pool is
 * advanced to "now" via {@link DecayEngine} (see that class's own JavaDoc for why a single snapshot + timestamp is
 * mathematically sufficient) whenever something actually needs the current state (insertion, extraction, or GUI
 * open) rather than every tick.
 * <p>
 * Deliberately minimal for this first pass: no multiblock structure, no custom texture/model (reuses an existing
 * GT casing texture), single import slot (accepts anything resolving to a registered {@link Element} via
 * {@link OreDictUnifier}), a handful of export slots, and one GUI button to convert the current composition into
 * concrete output items.
 */
public class MetaTileEntityIsotopeStorage extends MetaTileEntity {

    private static final int EXPORT_SLOTS = 9;
    private static final SimpleSidedCubeRenderer BASE_TEXTURE = Textures.STEAM_CASING_STEEL;

    /** Current pool: nuclide -> amount (1 unit per inserted item). Never contains a zero/negative entry. */
    private final Map<Element, Double> composition = new LinkedHashMap<>();
    /** World tick {@link #composition} was last advanced to via {@link DecayEngine}; -1 before first advance. */
    private long lastUpdateTick = -1;

    public MetaTileEntityIsotopeStorage(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityIsotopeStorage(metaTileEntityId);
    }

    @Override
    protected IItemHandlerModifiable createImportItemHandler() {
        return new GTItemStackHandler(this, 1) {

            /**
             * Consumes whatever ends up in the slot, regardless of whether it arrived via {@link #insertItem} (the
             * capability-facing path, used by hoppers/pipes) or a direct {@link #setStackInSlot} (what ModularUI's
             * item slot widget turned out to use for manual player placement -- overriding only
             * {@code insertItem} silently never fired for GUI-driven inserts, see 2026-09-16 manual playtest).
             * {@code onContentsChanged} is called by the base {@link net.minecraftforge.items.ItemStackHandler}
             * from every mutating path, so this fires either way.
             */
            @Override
            public void onContentsChanged(int slot) {
                super.onContentsChanged(slot);
                // Server-authoritative: without this guard, the client's own local MTE instance (every loaded
                // chunk has one per side) independently runs this same code against its own never-decaying
                // `composition` copy (advanceToNow() alone no-ops on the client, but nothing stopped the merge
                // below from running there too) -- found via manual playtest 2026-09-16, see extractComposition's
                // matching guard/JavaDoc.
                World world = getWorld();
                if (world == null || world.isRemote) return;
                ItemStack stack = getStackInSlot(slot);
                if (stack.isEmpty()) return;
                Element element = elementOf(stack);
                if (element == null) return; // not a resolvable nuclide material: leave it in the slot, rejected
                advanceToNow();
                composition.merge(element, (double) stack.getCount(), Double::sum);
                setStackInSlot(slot, ItemStack.EMPTY); // re-enters this method; the empty check above ends it
                markDirty();
            }

            @NotNull
            @Override
            public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                // Still reject anything unresolvable at the capability level (hoppers etc.), so automation gets a
                // normal "slot full/rejected" signal instead of silently absorbing junk items.
                if (!simulate && !stack.isEmpty() && elementOf(stack) == null) return stack;
                return super.insertItem(slot, stack, simulate);
            }
        };
    }

    @Override
    protected IItemHandlerModifiable createExportItemHandler() {
        return new GTItemStackHandler(this, EXPORT_SLOTS);
    }

    /**
     * Advances {@link #composition} from {@link #lastUpdateTick} to the current world tick via {@link DecayEngine},
     * then updates {@link #lastUpdateTick}. A no-op on the client (composition is server-authoritative) or if
     * called twice within the same tick.
     */
    private void advanceToNow() {
        World world = getWorld();
        if (world == null || world.isRemote) return;
        long now = world.getTotalWorldTime();
        if (lastUpdateTick < 0) {
            lastUpdateTick = now;
            return;
        }
        long elapsedTicks = now - lastUpdateTick;
        if (elapsedTicks <= 0) return;
        // Real physical elapsed time (20 ticks/second) -- TimeCompression, not this scaling, is what makes
        // multi-billion-year half-lives observable in a play session; see DecayEngine's/TimeCompression's JavaDoc.
        double elapsedSeconds = elapsedTicks / 20.0;
        Map<Element, Double> advanced = DecayEngine.advance(composition, elapsedSeconds, TimeCompression::compress);
        composition.clear();
        composition.putAll(advanced);
        lastUpdateTick = now;
    }

    /**
     * Converts as much of the current (decayed) composition as fits into {@link #exportItems} into real items.
     * Server-authoritative -- see {@code createImportItemHandler()}'s {@code onContentsChanged} for why this guard
     * is needed (the GUI button's {@code onMousePressed} runs this against whichever side's MTE instance handles
     * the click, and the client's copy must never touch its own un-decaying {@link #composition}).
     */
    private void extractComposition() {
        World world = getWorld();
        if (world == null || world.isRemote) return;
        advanceToNow();
        GTNLLogger.logger.info("[IsotopeStorage] extractComposition: composition={}", formatComposition());
        for (Element element : new ArrayList<>(composition.keySet())) {
            double amount = composition.getOrDefault(element, 0.0);
            int wholeUnits = (int) Math.floor(amount);
            if (wholeUnits <= 0) continue;
            Material material = materialFor(element);
            if (material == null) {
                GTNLLogger.logger.warn("[IsotopeStorage] no Material found for Element {}", element.symbol);
                continue;
            }
            ItemStack output = OreDictUnifier.get(OrePrefix.dust, material, wholeUnits);
            if (output.isEmpty()) {
                GTNLLogger.logger.warn("[IsotopeStorage] OreDictUnifier.get returned EMPTY for material {} (id={})",
                        material.getRegistryName(), material.getId());
                continue;
            }
            GTNLLogger.logger.info("[IsotopeStorage] extracting {} x {} (material id={}) for element {}",
                    wholeUnits, output.getDisplayName(), material.getId(), element.symbol);
            ItemStack remainder = ItemHandlerHelper.insertItemStacked(exportItems, output, false);
            int inserted = wholeUnits - remainder.getCount();
            if (inserted > 0) {
                composition.put(element, amount - inserted);
            }
        }
        markDirty();
    }

    @Nullable
    private static Element elementOf(@NotNull ItemStack stack) {
        MaterialStack materialStack = OreDictUnifier.getMaterial(stack);
        return materialStack == null ? null : materialStack.material.getElement();
    }

    /** Linear search over all registered materials -- fine, only called on manual extraction, not per tick. */
    @Nullable
    private static Material materialFor(@NotNull Element element) {
        for (Material material : GregTechAPI.materialManager.getRegisteredMaterials()) {
            if (element.equals(material.getElement())) return material;
        }
        return null;
    }

    private String formatComposition() {
        if (composition.isEmpty()) return "-";
        Map<String, Double> bySymbol = new TreeMap<>();
        for (Map.Entry<Element, Double> entry : composition.entrySet()) {
            if (entry.getValue() <= 0) continue;
            bySymbol.put(entry.getKey().symbol, entry.getValue());
        }
        return bySymbol.entrySet().stream()
                .map(e -> String.format("%s: %.2f", e.getKey(), e.getValue()))
                .collect(Collectors.joining("\n"));
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        BASE_TEXTURE.render(renderState, translation, pipeline);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        return Pair.of(BASE_TEXTURE.getParticleSprite(), getPaintingColorForRendering());
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
        advanceToNow();
        StringSyncValue compositionText = new StringSyncValue(this::formatComposition);
        syncManager.syncValue("composition", compositionText);

        // A plain ButtonWidget#onMousePressed only ever ran client-side in this setup (found via manual playtest
        // 2026-09-16 -- calling extractComposition() directly from it had no server-side effect once that method
        // was guarded against isRemote). Routing the click through a BooleanSyncValue setter instead works: MUI's
        // sync framework itself propagates a client-side value change to the server and invokes the setter there,
        // exactly like ItemBus's own workingStateValue/collapseStateValue toggle buttons. The getter always
        // reports false so every click is seen as a fresh change (a momentary trigger, not a persisted flag).
        BooleanSyncValue extractTrigger = new BooleanSyncValue(() -> false, val -> {
            if (val) extractComposition();
        });
        syncManager.syncValue("extract_trigger", extractTrigger);

        int width = 176;
        int height = 280;
        return GTGuis.createPanel(this, width, height)
                .child(IKey.lang(getMetaFullName()).asWidget().pos(5, 5))
                .child(new ItemSlot()
                        .slot(SyncHandlers.itemSlot(importItems, 0))
                        .pos(8, 20))
                .child(new Grid()
                        .pos(8, 45).height(18 * 3)
                        .minElementMargin(0, 0)
                        .minColWidth(18).minRowHeight(18)
                        .mapTo(3, EXPORT_SLOTS, index -> new ItemSlot()
                                .slot(SyncHandlers.itemSlot(exportItems, index))))
                .child(new ButtonWidget<>()
                        .pos(8, 104).size(160, 16)
                        .overlay(IKey.lang("gtnuclear.gui.isotope_storage.extract"))
                        .onMousePressed(mouseButton -> {
                            extractTrigger.setBoolValue(true);
                            return true;
                        }))
                .child(IKey.dynamic(compositionText::getStringValue).asWidget()
                        .pos(8, 124))
                .child(SlotGroupWidget.playerInventory(false).left(7).bottom(7));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagList list = new NBTTagList();
        for (Map.Entry<Element, Double> entry : composition.entrySet()) {
            NBTTagCompound entryTag = new NBTTagCompound();
            entryTag.setString("Symbol", entry.getKey().symbol);
            entryTag.setDouble("Amount", entry.getValue());
            list.appendTag(entryTag);
        }
        data.setTag("Composition", list);
        data.setLong("LastUpdateTick", lastUpdateTick);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        composition.clear();
        NBTTagList list = data.getTagList("Composition", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entryTag = list.getCompoundTagAt(i);
            Element element = Elements.get(entryTag.getString("Symbol"));
            if (element != null) {
                composition.put(element, entryTag.getDouble("Amount"));
            }
        }
        lastUpdateTick = data.hasKey("LastUpdateTick") ? data.getLong("LastUpdateTick") : -1;
    }

    @Override
    public void getDrops(@NotNull List<ItemStack> dropsList, @Nullable EntityPlayer harvester) {
        super.getDrops(dropsList, harvester);
        for (Map.Entry<Element, Double> entry : composition.entrySet()) {
            int wholeUnits = (int) Math.floor(entry.getValue());
            if (wholeUnits <= 0) continue;
            Material material = materialFor(entry.getKey());
            if (material == null) continue;
            ItemStack stack = OreDictUnifier.get(OrePrefix.dust, material, wholeUnits);
            if (!stack.isEmpty()) dropsList.add(stack);
        }
    }
}
