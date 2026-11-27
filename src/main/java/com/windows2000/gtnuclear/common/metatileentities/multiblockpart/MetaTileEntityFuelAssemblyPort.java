package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.GTValues;
import gregtech.api.capability.impl.NotifiableItemStackHandler;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.AbilityInstances;
import gregtech.api.metatileentity.multiblock.IMultiblockAbilityPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockControllerBase;
import gregtech.api.mui.GTGuiTextures;
import gregtech.api.mui.GTGuis;
import gregtech.api.unification.Element;
import gregtech.api.unification.Elements;
import gregtech.common.metatileentities.multi.multiblockpart.MetaTileEntityMultiblockNotifiablePart;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.DoubleSyncValue;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.StringSyncValue;
import com.cleanroommc.modularui.value.sync.SyncHandler;
import com.cleanroommc.modularui.value.sync.SyncHandlers;
import com.cleanroommc.modularui.widgets.ProgressWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.slot.ItemSlot;
import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import com.windows2000.gtnuclear.api.nuclear.fuel.FuelComposition;
import com.windows2000.gtnuclear.common.ConfigHolder;
import com.windows2000.gtnuclear.common.item.AssemblyLength;
import com.windows2000.gtnuclear.common.item.GTNLItems;
import com.windows2000.gtnuclear.common.item.ItemBrokenControlRod;
import com.windows2000.gtnuclear.common.item.ItemControlRod;
import com.windows2000.gtnuclear.common.item.ItemFuelRod;
import com.windows2000.gtnuclear.common.metatileentities.multi.MetaTileEntityPressurizedWaterReactor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * One fuel assembly channel's top port (2026-11-21, stage 1-3 of this mod's reactor physics rework, see this mod's
 * design memory) -- replaces the former separate fuel port and control rod controller. A real PWR's rod cluster
 * control assemblies live inside fuel assemblies' guide tubes, so one port holds both:
 * <ul>
 * <li>slot 0: a fuel assembly ({@link ItemFuelRod}),</li>
 * <li>slot 1: a guide-tube insert -- currently a control rod cluster ({@link ItemControlRod}) or a worn-out one
 * ({@link ItemBrokenControlRod}); future burnable absorbers / neutron sources would share this same slot.</li>
 * </ul>
 * plus the drive mechanism's own state: {@link #insertionDepth} (0 = withdrawn, 1 = fully inserted) and
 * {@link #bankId} (0 = unassigned, 1-{@link #BANK_COUNT}). Both are only meaningful while a control rod cluster is
 * present, and are operated from the control console only -- this port's own UI is display-only.
 * <p>
 * Every insertion/extraction path (GUI, hoppers/pipes, {@code setStackInSlot}) goes through
 * {@link FuelAssemblyItemHandler}, which applies {@link FuelAssemblySlotRules}: maintenance lock (until stage 1-4,
 * "formed reactor only while SHUTDOWN"), item kind, fabricated length vs channel height, and the
 * insert-out-before-fuel-out ordering. Internal writes (burnup write-back, wear) bypass the rules.
 */
public class MetaTileEntityFuelAssemblyPort extends MetaTileEntityMultiblockNotifiablePart
                                            implements IMultiblockAbilityPart<IItemHandlerModifiable> {

    /** Number of real banks (1-{@value}); 0 itself means "unassigned", not a bank. */
    public static final int BANK_COUNT = 4;

    /**
     * Sub-integer wear accumulator NBT key on the control rod stack -- vanilla item damage is an integer, but
     * per-tick wear is tiny, so accumulating only in whole-damage-point steps would round every tick's wear away.
     */
    private static final String NBT_WEAR_ACCUMULATOR = "WearAccumulator";

    private final FuelAssemblyItemHandler inventory;
    private double insertionDepth = 1.0;
    private int bankId = 0;
    /** Set while this class itself rewrites a slot (burnup, wear) so the slot rules do not block it. */
    private boolean internalWrite = false;

    public MetaTileEntityFuelAssemblyPort(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, GTValues.IV, false);
        this.inventory = new FuelAssemblyItemHandler(this);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityFuelAssemblyPort(metaTileEntityId);
    }

    // ---------------------------------------------------------------- multiblock wiring

    @Override
    public @NotNull List<MultiblockAbility<?>> getAbilities() {
        return Collections.singletonList(GTNLMultiblockAbility.FUEL_ASSEMBLY_PORT);
    }

    @Override
    public void registerAbilities(@NotNull AbilityInstances abilityInstances) {
        abilityInstances.add(inventory);
    }

    @Override
    public void addToMultiBlock(MultiblockControllerBase controllerBase) {
        super.addToMultiBlock(controllerBase);
        inventory.addNotifiableMetaTileEntity(controllerBase);
    }

    @Override
    public void removeFromMultiBlock(MultiblockControllerBase controllerBase) {
        super.removeFromMultiBlock(controllerBase);
        inventory.removeNotifiableMetaTileEntity(controllerBase);
    }

    /** The reactor this port is currently part of, or {@code null} if not formed into one. */
    @Nullable
    private MetaTileEntityPressurizedWaterReactor reactor() {
        return getController() instanceof MetaTileEntityPressurizedWaterReactor reactor &&
                reactor.isStructureFormed() ? reactor : null;
    }

    // ---------------------------------------------------------------- contents

    @NotNull
    public ItemStack getFuelStack() {
        return inventory.getStackInSlot(FuelAssemblySlotRules.SLOT_FUEL);
    }

    @NotNull
    public ItemStack getInsertStack() {
        return inventory.getStackInSlot(FuelAssemblySlotRules.SLOT_INSERT);
    }

    public boolean hasFuel() {
        return getFuelStack().getItem() instanceof ItemFuelRod;
    }

    /** Whether an intact (not worn-out) control rod cluster is in the guide tubes. */
    public boolean hasControlRod() {
        return getInsertStack().getItem() instanceof ItemControlRod;
    }

    public int getFuelLength() {
        return hasFuel() ? AssemblyLength.get(getFuelStack()) : 0;
    }

    public int getInsertLength() {
        return getInsertStack().isEmpty() ? 0 : AssemblyLength.get(getInsertStack());
    }

    /**
     * The loaded fuel assembly's live (layer-averaged) composition, derived from its own NBT state rather than
     * its fixed enrichment tier -- so performance reflects however much that specific assembly has burned.
     */
    @Nullable
    public FuelComposition getLoadedComposition() {
        ItemStack stack = getFuelStack();
        if (!(stack.getItem() instanceof ItemFuelRod fuelRod) || AssemblyLength.get(stack) <= 0) return null;
        Map<Element, Double> amounts = fuelRod.getIsotopeAmounts(stack);
        if (amounts.isEmpty()) return null;
        return new FuelComposition(fuelRod.initialComposition.id, amounts);
    }

    /** Persists the burnup engine's new composition into the loaded assembly (no-op without fuel). */
    public void setLoadedIsotopeAmounts(@NotNull Map<Element, Double> amounts) {
        ItemStack stack = getFuelStack();
        if (!(stack.getItem() instanceof ItemFuelRod fuelRod)) return;
        fuelRod.setIsotopeAmounts(stack, amounts);
        writeInternally(FuelAssemblySlotRules.SLOT_FUEL, stack);
    }

    /** Fully-inserted worth of the loaded control rod cluster (stage-4 interim), 0 without one. */
    public double getMaxWorthReactivity() {
        return getInsertStack().getItem() instanceof ItemControlRod rod ? rod.maxWorthReactivity : 0.0;
    }

    public double getInsertionDepth() {
        return insertionDepth;
    }

    public void setInsertionDepth(double insertionDepth) {
        double clamped = Math.max(0.0, Math.min(1.0, insertionDepth));
        if (clamped != this.insertionDepth) {
            this.insertionDepth = clamped;
            markDirty();
        }
    }

    public int getBankId() {
        return bankId;
    }

    /** Clamped to {@code [0, BANK_COUNT]}, not wrapped -- callers that want cycling handle the wraparound. */
    public void setBankId(int bankId) {
        this.bankId = Math.max(0, Math.min(BANK_COUNT, bankId));
        markDirty();
    }

    /** Fraction (0-1) of the loaded control rod's wear capacity already used, 0 without one. */
    public double getWearFraction() {
        ItemStack stack = getInsertStack();
        if (!(stack.getItem() instanceof ItemControlRod rod) || rod.getMaxDamage() <= 0) return 0.0;
        NBTTagCompound tag = stack.getTagCompound();
        double accumulated = tag != null ? tag.getDouble(NBT_WEAR_ACCUMULATOR) : 0.0;
        return Math.min(1.0, accumulated / rod.getMaxDamage());
    }

    /**
     * Applies {@code amount} (fractional vanilla-damage units, already scaled by the caller) of wear to the loaded
     * control rod. Once the accumulated total reaches {@code getMaxDamage()}, the stack becomes a
     * {@link GTNLItems#BROKEN_CONTROL_ROD} keeping the original length.
     */
    public void applyWear(double amount) {
        if (amount <= 0) return;
        ItemStack stack = getInsertStack();
        if (!(stack.getItem() instanceof ItemControlRod rod)) return;
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        NBTTagCompound tag = stack.getTagCompound();
        double accumulated = tag.getDouble(NBT_WEAR_ACCUMULATOR) + amount;
        if (accumulated >= rod.getMaxDamage()) {
            ItemStack broken = AssemblyLength.withLength(new ItemStack(GTNLItems.BROKEN_CONTROL_ROD),
                    AssemblyLength.get(stack));
            writeInternally(FuelAssemblySlotRules.SLOT_INSERT, broken);
        } else {
            tag.setDouble(NBT_WEAR_ACCUMULATOR, accumulated);
            stack.setItemDamage((int) accumulated);
            markDirty();
        }
    }

    private void writeInternally(int slot, @NotNull ItemStack stack) {
        internalWrite = true;
        try {
            inventory.setStackInSlot(slot, stack);
        } finally {
            internalWrite = false;
        }
        markDirty();
    }

    // ---------------------------------------------------------------- slot rules

    @NotNull
    private static FuelAssemblySlotRules.ItemKind kindOf(@NotNull ItemStack stack) {
        if (stack.getItem() instanceof ItemFuelRod) return FuelAssemblySlotRules.ItemKind.FUEL_ASSEMBLY;
        if (stack.getItem() instanceof ItemControlRod) return FuelAssemblySlotRules.ItemKind.CONTROL_ROD;
        if (stack.getItem() instanceof ItemBrokenControlRod) return FuelAssemblySlotRules.ItemKind.BROKEN_CONTROL_ROD;
        return FuelAssemblySlotRules.ItemKind.OTHER;
    }

    @NotNull
    private FuelAssemblySlotRules.Context slotContext() {
        MetaTileEntityPressurizedWaterReactor reactor = reactor();
        return new FuelAssemblySlotRules.Context(reactor != null, reactor != null && reactor.isFuelHandlingAllowed(),
                reactor != null ? reactor.getChannelHeightLayers() : 0, !getFuelStack().isEmpty(),
                !getInsertStack().isEmpty());
    }

    private boolean canInsert(int slot, @NotNull ItemStack stack) {
        return FuelAssemblySlotRules.canInsert(slot, kindOf(stack), AssemblyLength.get(stack), slotContext());
    }

    private boolean canExtract(int slot) {
        return FuelAssemblySlotRules.canExtract(slot, slotContext());
    }

    /**
     * Whether the slot rules are enforced for this operation: never for this class's own internal writes, and never
     * on the client (2026-11-27). The server is authoritative -- the client only mirrors what the server's slot sync
     * tells it, and some of what the rules read is not synced (e.g. the reactor's channel height), so judging on the
     * client could only make the display disagree with the real contents.
     */
    private boolean enforceRules() {
        return !internalWrite && !(getWorld() != null && getWorld().isRemote);
    }

    /**
     * Debug log of every refused slot operation with its judgment context, gated by
     * {@code ConfigHolder.reactor.debugFuelAssemblyPortLogging} (added 2026-11-27 while investigating a playtest
     * where items could not be taken out of an unlocked port; that case did not reproduce).
     */
    private void debugDenied(@NotNull String operation, int slot, @NotNull ItemStack stack) {
        if (!ConfigHolder.reactor.debugFuelAssemblyPortLogging) return;
        FuelAssemblySlotRules.Context ctx = slotContext();
        boolean client = getWorld() != null && getWorld().isRemote;
        MetaTileEntityPressurizedWaterReactor reactor = reactor();
        com.windows2000.gtnuclear.api.util.GTNLLogger.logger.info(
                "[FAP-DEBUG] {} side={} pos={} slot={} item={} ctx={} reactorFound={} controller={}",
                operation, client ? "CLIENT" : "SERVER", getPos(), slot, stack, ctx, reactor != null,
                getController() == null ? "null" : getController().getClass().getSimpleName());
        if (reactor != null) {
            com.windows2000.gtnuclear.api.util.GTNLLogger.logger.info(
                    "[FAP-DEBUG]   reactor stage={} lock={} formed={} channelHeight={}",
                    reactor.getOperatingState().stage, reactor.getFuelHandlingLock(), reactor.isStructureFormed(),
                    reactor.getChannelHeightLayers());
        }
    }

    /**
     * Reacts to any slot change: an emptied guide tube resets the drive to fully inserted (so the next rod starts
     * inserted); the core shape is stale. Reset on removal rather than on insertion because Forge's
     * {@code SlotItemHandler#isItemValid} temporarily clears and restores the slot via {@code setStackInSlot} --
     * resetting on "insert into empty" would snap a loaded rod to 1.0 whenever a GUI merely validated a stack.
     */
    private void onSlotChanged(int slot, boolean wasEmpty) {
        if (slot == FuelAssemblySlotRules.SLOT_INSERT && getInsertStack().isEmpty()) {
            insertionDepth = 1.0;
        }
        MetaTileEntityPressurizedWaterReactor reactor = reactor();
        if (reactor != null) reactor.requestShapeRecompute();
        markDirty();
    }

    private final class FuelAssemblyItemHandler extends NotifiableItemStackHandler {

        FuelAssemblyItemHandler(MetaTileEntity metaTileEntity) {
            super(metaTileEntity, 2, metaTileEntity, false);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.isEmpty() || FuelAssemblySlotRules.isKindAccepted(slot, kindOf(stack));
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!stack.isEmpty() && enforceRules() && !canInsert(slot, stack)) {
                debugDenied(simulate ? "insertItem(simulate)" : "insertItem", slot, stack);
                return stack;
            }
            boolean wasEmpty = getStackInSlot(slot).isEmpty();
            ItemStack remainder = super.insertItem(slot, stack, simulate);
            if (!simulate && remainder.getCount() != stack.getCount()) onSlotChanged(slot, wasEmpty);
            return remainder;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (enforceRules() && !canExtract(slot)) {
                debugDenied(simulate ? "extractItem(simulate)" : "extractItem", slot, getStackInSlot(slot));
                return ItemStack.EMPTY;
            }
            ItemStack extracted = super.extractItem(slot, amount, simulate);
            if (!simulate && !extracted.isEmpty()) onSlotChanged(slot, false);
            return extracted;
        }

        /**
         * GUI interactions (ModularUI slots, Forge's {@code SlotItemHandler}) write through here directly rather
         * than through {@link #insertItem} -- a real past bug in this mod (see {@code MetaTileEntityIsotopeStorage})
         * -- so the same rules are enforced here too. Clearing a slot counts as an extraction, filling it as an
         * insertion; anything else (an in-place swap) must satisfy both.
         */
        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            ItemStack current = getStackInSlot(slot);
            if (enforceRules()) {
                boolean removing = !current.isEmpty();
                boolean adding = !stack.isEmpty();
                if (removing && !canExtract(slot)) {
                    debugDenied("setStackInSlot(remove " + current + " -> " + stack + ")", slot, current);
                    return;
                }
                if (adding && removing) {
                    // Evaluate the insertion as if the slot had just been emptied.
                    if (!FuelAssemblySlotRules.canInsert(slot, kindOf(stack), AssemblyLength.get(stack),
                            contextWithSlotCleared(slot))) {
                        debugDenied("setStackInSlot(swap)", slot, stack);
                        return;
                    }
                } else if (adding && !canInsert(slot, stack)) {
                    debugDenied("setStackInSlot(add)", slot, stack);
                    return;
                }
            }
            boolean wasEmpty = current.isEmpty();
            super.setStackInSlot(slot, stack);
            if (!internalWrite) onSlotChanged(slot, wasEmpty);
        }

        @NotNull
        private FuelAssemblySlotRules.Context contextWithSlotCleared(int slot) {
            FuelAssemblySlotRules.Context base = slotContext();
            return new FuelAssemblySlotRules.Context(base.formed(), base.fuelHandlingAllowed(),
                    base.channelHeightLayers(), slot != FuelAssemblySlotRules.SLOT_FUEL && base.fuelPresent(),
                    slot != FuelAssemblySlotRules.SLOT_INSERT && base.insertPresent());
        }
    }

    // ---------------------------------------------------------------- persistence

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("Inventory", inventory.serializeNBT());
        data.setDouble("InsertionDepth", insertionDepth);
        data.setInteger("BankId", bankId);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        inventory.deserializeNBT(data.getCompoundTag("Inventory"));
        if (data.hasKey("InsertionDepth")) insertionDepth = data.getDouble("InsertionDepth");
        bankId = data.getInteger("BankId");
    }

    // ---------------------------------------------------------------- provisional UI (stage 1-3 (d))

    private static final String LANG = "gtnuclear.gui.fuel_assembly_port.";
    private static final int LOCK_UNFORMED = 0;
    private static final int LOCK_OPEN = 1;
    private static final int LOCK_LOCKED = 2;
    private static final int LOCK_HOLD = 3;
    private static final int LOCK_ACCIDENT = 4;

    /** Server-side only: on the client every sync value is overwritten from packets. */
    private boolean isServer() {
        return getWorld() != null && !getWorld().isRemote;
    }

    @NotNull
    private String serverString(@NotNull Supplier<String> supplier) {
        return isServer() ? supplier.get() : "";
    }

    @NotNull
    private String positionText() {
        MetaTileEntityPressurizedWaterReactor reactor = reactor();
        if (reactor == null) return "";
        BlockPos pos = getPos();
        BlockPos controller = reactor.getPos();
        return String.format("(%+d, %+d)", pos.getX() - controller.getX(), pos.getZ() - controller.getZ());
    }

    private int lockState() {
        MetaTileEntityPressurizedWaterReactor reactor = reactor();
        if (reactor == null) return LOCK_UNFORMED;
        return switch (reactor.getFuelHandlingLock()) {
            case NONE -> LOCK_OPEN;
            case HOLD -> LOCK_HOLD;
            case SEVERE_ACCIDENT -> LOCK_ACCIDENT;
            case NOT_REFUELING -> LOCK_LOCKED;
        };
    }

    @NotNull
    private String fuelTranslationKey() {
        ItemStack stack = getFuelStack();
        return stack.getItem() instanceof ItemFuelRod ? stack.getItem().getTranslationKey(stack) + ".name" : "";
    }

    @NotNull
    private String insertTranslationKey() {
        ItemStack stack = getInsertStack();
        return stack.isEmpty() ? "" : stack.getItem().getTranslationKey(stack) + ".name";
    }

    @NotNull
    private String fuelEnrichmentText() {
        return getFuelStack().getItem() instanceof ItemFuelRod fuelRod ?
                FuelAssemblyPortText.enrichment(fuelRod.freshU235Fraction()) : "";
    }

    @NotNull
    private String isotopeText(@Nullable Element element) {
        ItemStack stack = getFuelStack();
        if (element == null || !(stack.getItem() instanceof ItemFuelRod fuelRod)) return "";
        return FuelAssemblyPortText.kilograms(fuelRod.isotopeMassKg(stack, element));
    }

    @NotNull
    private MetaTileEntityPressurizedWaterReactor.PortTelemetry telemetry() {
        MetaTileEntityPressurizedWaterReactor reactor = reactor();
        return reactor != null ? reactor.getPortTelemetry(this) :
                MetaTileEntityPressurizedWaterReactor.PortTelemetry.NONE;
    }

    private double channelHeightForDisplay() {
        MetaTileEntityPressurizedWaterReactor reactor = reactor();
        int height = reactor != null ? reactor.getChannelHeightLayers() :
                Math.max(getFuelLength(), getInsertLength());
        return Math.max(1, height);
    }

    @Override
    public ModularPanel buildUI(PosGuiData guiData, PanelSyncManager syncManager, UISettings settings) {
        StringSyncValue position = new StringSyncValue(() -> serverString(this::positionText));
        IntSyncValue lock = new IntSyncValue(this::lockState);
        StringSyncValue fuelKey = new StringSyncValue(() -> serverString(this::fuelTranslationKey));
        StringSyncValue fuelEnrichment = new StringSyncValue(() -> serverString(this::fuelEnrichmentText));
        StringSyncValue fuelLength = new StringSyncValue(() -> serverString(() -> hasFuel() ?
                FuelAssemblyPortText.length(getFuelLength()) : ""));
        StringSyncValue fuelRemaining = new StringSyncValue(() -> serverString(() -> getFuelStack()
                .getItem() instanceof ItemFuelRod fuelRod ?
                        FuelAssemblyPortText.percent(fuelRod.getFissileCapabilityFraction(getFuelStack())) : ""));
        StringSyncValue u235 = new StringSyncValue(() -> serverString(() -> isotopeText(Elements.U235)));
        StringSyncValue u238 = new StringSyncValue(() -> serverString(() -> isotopeText(Elements.U238)));
        StringSyncValue pu239 = new StringSyncValue(() -> serverString(() -> isotopeText(Elements.Pu239)));
        StringSyncValue localPower = new StringSyncValue(() -> serverString(() -> hasFuel() ?
                FuelAssemblyPortText.relativePower(telemetry().localRelativePower()) : ""));
        StringSyncValue xenon = new StringSyncValue(() -> serverString(() -> hasFuel() ?
                FuelAssemblyPortText.pcm(telemetry().xenonPcm()) : ""));
        StringSyncValue fuelTemperature = new StringSyncValue(() -> serverString(() -> hasFuel() ?
                FuelAssemblyPortText.temperature(telemetry().fuelTemperatureKelvin()) : ""));
        StringSyncValue insertKey = new StringSyncValue(() -> serverString(this::insertTranslationKey));
        StringSyncValue insertLength = new StringSyncValue(() -> serverString(() -> getInsertStack().isEmpty() ? "" :
                FuelAssemblyPortText.length(getInsertLength())));
        StringSyncValue bank = new StringSyncValue(() -> serverString(() -> Integer.toString(bankId)));
        StringSyncValue insertion = new StringSyncValue(
                () -> serverString(() -> FuelAssemblyPortText.percent(insertionDepth)));
        StringSyncValue wear = new StringSyncValue(
                () -> serverString(() -> FuelAssemblyPortText.percent(getWearFraction())));
        DoubleSyncValue fuelBar = new DoubleSyncValue(() -> Math.min(1.0,
                getFuelLength() / channelHeightForDisplay()));
        DoubleSyncValue rodBar = new DoubleSyncValue(() -> hasControlRod() ?
                Math.min(1.0, insertionDepth * getInsertLength() / channelHeightForDisplay()) : 0.0);

        String[] names = { "position", "lock", "fuel_key", "fuel_enrichment", "fuel_length", "fuel_remaining",
                "u235", "u238", "pu239", "local_power", "xenon", "fuel_temperature", "insert_key", "insert_length",
                "bank", "insertion", "wear" };
        SyncHandler[] values = { position, lock, fuelKey, fuelEnrichment, fuelLength, fuelRemaining, u235, u238,
                pu239, localPower, xenon, fuelTemperature, insertKey, insertLength, bank, insertion, wear, fuelBar,
                rodBar };
        String[] extraNames = { "fuel_bar", "rod_bar" };
        for (int i = 0; i < values.length; i++) {
            String name = i < names.length ? names[i] : extraNames[i - names.length];
            syncManager.syncValue("fap_" + name, values[i]);
        }

        ModularPanel panel = GTGuis.createPanel(this, 176, 210)
                .child(IKey.dynamic(() -> IKey.lang(getMetaFullName()).get() + " " + position.getValue())
                        .asWidget().pos(6, 6).scale(0.8f))
                .child(IKey.dynamic(() -> IKey.lang(LANG + switch (lock.getIntValue()) {
                case LOCK_OPEN -> "unlocked";
                case LOCK_LOCKED -> "locked";
                case LOCK_HOLD -> "locked_hold";
                case LOCK_ACCIDENT -> "locked_accident";
                default -> "unformed";
                }).get()).asWidget().pos(120, 6).scale(0.8f)
                        .color(() -> switch (lock.getIntValue()) {
                        case LOCK_OPEN -> 0x2E9E2E;
                        case LOCK_LOCKED, LOCK_HOLD, LOCK_ACCIDENT -> 0xC03030;
                        default -> 0x808080;
                        }))
                .child(new ItemSlot().slot(SyncHandlers.itemSlot(inventory, FuelAssemblySlotRules.SLOT_FUEL))
                        .background(GTGuiTextures.SLOT).pos(7, 18))
                .child(new ItemSlot().slot(SyncHandlers.itemSlot(inventory, FuelAssemblySlotRules.SLOT_INSERT))
                        .background(GTGuiTextures.SLOT).pos(92, 18))
                .child(IKey.lang(LANG + "fuel").asWidget().pos(27, 23).scale(0.8f))
                .child(IKey.lang(LANG + "insert").asWidget().pos(112, 23).scale(0.8f));

        int y = 40;
        panel.child(line(() -> fuelKey.getValue().isEmpty() ? IKey.lang(LANG + "none").get() :
                IKey.lang(fuelKey.getValue()).get() + " " + fuelEnrichment.getValue(), 7, y));
        panel.child(labeled("fuel.length", fuelLength, 7, y += 9));
        panel.child(labeled("fuel.remaining", fuelRemaining, 7, y += 9));
        panel.child(labeled("fuel.u235", u235, 7, y += 9));
        panel.child(labeled("fuel.u238", u238, 7, y += 9));
        panel.child(labeled("fuel.pu239", pu239, 7, y += 9));
        panel.child(labeled("fuel.local_power", localPower, 7, y += 9));
        panel.child(labeled("fuel.xenon", xenon, 7, y += 9));
        panel.child(labeled("fuel.temperature", fuelTemperature, 7, y += 9));

        y = 40;
        panel.child(line(() -> insertKey.getValue().isEmpty() ? IKey.lang(LANG + "none").get() :
                IKey.lang(insertKey.getValue()).get(), 92, y));
        panel.child(labeled("insert.length", insertLength, 92, y += 9));
        panel.child(labeled("insert.bank", bank, 92, y += 9));
        panel.child(labeled("insert.insertion", insertion, 92, y += 9));
        panel.child(labeled("insert.wear", wear, 92, y += 9));

        panel.child(new ProgressWidget().pos(150, 40).size(8, 64).value(fuelBar)
                .texture(GTGuiTextures.PROGRESS_BAR_BOILER_EMPTY_STEEL, GTGuiTextures.PROGRESS_BAR_BOILER_HEAT, 64)
                .direction(ProgressWidget.Direction.UP));
        panel.child(new ProgressWidget().pos(161, 40).size(8, 64).value(rodBar)
                .texture(GTGuiTextures.PROGRESS_BAR_BOILER_EMPTY_STEEL, GTGuiTextures.PROGRESS_BAR_BOILER_HEAT, 64)
                .direction(ProgressWidget.Direction.DOWN));
        return panel.bindPlayerInventory();
    }

    @NotNull
    private static TextWidget<?> line(@NotNull Supplier<String> text, int x, int y) {
        return IKey.dynamic(text).asWidget().pos(x, y).scale(0.7f);
    }

    /** One "label: value" line; blank (hidden) while the value is empty. */
    @NotNull
    private static TextWidget<?> labeled(@NotNull String key, @NotNull StringSyncValue value, int x, int y) {
        return line(() -> value.getValue().isEmpty() ? "" : IKey.lang(LANG + key, value.getValue()).get(), x, y);
    }
}
