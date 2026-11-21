package com.windows2000.gtnuclear.common.metatileentities.multiblockpart;

import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.items.behaviors.AbstractMaterialPartBehavior;
import gregtech.common.items.behaviors.TurbineRotorBehavior;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import com.windows2000.gtnuclear.api.metatileentity.multiblock.GTNLMultiblockAbility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one GT turbine rotor item, reading GT's own rotor data (material-driven {@code RotorProperty} via
 * {@code TurbineRotorBehavior}/{@code AbstractMaterialPartBehavior}) directly rather than reusing GT's own
 * {@code MetaTileEntityRotorHolder} part class -- see this mod's design memory (2026-10) for why: that class
 * unconditionally casts its own controller to {@code MetaTileEntityLargeTurbine} in both {@code update()} and
 * its {@code damageRotor} path, so embedding it in a non-GT-turbine controller would throw
 * {@code ClassCastException} the moment it tried to notify its controller. Every rotor stat this mod's own
 * turbine MTEs need ({@code getRotorEfficiencyPercent}/{@code getRotorPowerPercent}/{@link #damageRotor}) is
 * exposed here purely via GT's own static/stack-level API, with no dependency on that class at all.
 * <p>
 * Deliberately not an {@code IRotorHolder}/{@code MultiblockAbility.ROTOR_HOLDER} -- see
 * {@link GTNLMultiblockAbility#TURBINE_ROTOR_SLOT}'s own JavaDoc for why a separate ability avoids colliding with
 * GT's own turbine structure-pattern checks.
 */
public class MetaTileEntityTurbineRotorHolder extends MetaTileEntityReactorItemPort {

    private static final int UPDATE_ROTOR_RENDER_STATE = 400;
    /**
     * Mirrors GT's own {@code MetaTileEntityRotorHolder} ramp rates -- a brief steam starvation (held EU/t
     * demand, pipe stall, etc.) shouldn't instantly snap the rotor to a stop.
     */
    private static final int SPEED_RAMP_UP_PER_TICK = 1;
    private static final int SPEED_RAMP_DOWN_PER_TICK = 3;
    private static final int MAX_SPEED = 100;

    /** Server-only ramp state backing {@link #isRotorSpinning} -- see {@link #setActiveThisTick}. */
    private int currentSpeed = 0;
    /** Synced to the client purely for {@link #renderMetaTileEntity}'s own spinning-vs-idle sprite choice. */
    private boolean isRotorSpinning = false;
    /**
     * Synced rotor material color for {@link #renderMetaTileEntity}, {@code -1} meaning "no rotor" -- GT's own
     * {@code MetaTileEntityRotorHolder} defines an {@code UPDATE_ROTOR_COLOR} data code for this but never
     * actually sends it (confirmed via source search, 2026-10), so that class's own rotor color only ever
     * updates via initial sync or a player's own GUI being open when the slot changes. This class sends it
     * properly instead, recomputed alongside the spin ramp every tick.
     */
    private int rotorColor = -1;

    public MetaTileEntityTurbineRotorHolder(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityTurbineRotorHolder(metaTileEntityId);
    }

    @Override
    protected boolean isItemAllowed(@NotNull ItemStack stack) {
        return TurbineRotorBehavior.getInstanceFor(stack) != null;
    }

    @NotNull
    @Override
    protected MultiblockAbility<IItemHandlerModifiable> getPortAbility() {
        return GTNLMultiblockAbility.TURBINE_ROTOR_SLOT;
    }

    /** @return {@code TurbineRotorBehavior.getRotorEfficiency}, or 0 with no (valid) rotor loaded. */
    public double getRotorEfficiencyPercent() {
        ItemStack stack = getInsertedItem();
        return TurbineRotorBehavior.getInstanceFor(stack) != null ? TurbineRotorBehavior.getRotorEfficiency(stack) :
                0.0;
    }

    /** @return {@code TurbineRotorBehavior.getRotorPower}, or 0 with no (valid) rotor loaded. */
    public double getRotorPowerPercent() {
        ItemStack stack = getInsertedItem();
        return TurbineRotorBehavior.getInstanceFor(stack) != null ? TurbineRotorBehavior.getRotorPower(stack) : 0.0;
    }

    /** @return the loaded rotor's own material color for display, or {@code null} with no rotor loaded. */
    @Nullable
    public Integer getRotorMaterialColor() {
        ItemStack stack = getInsertedItem();
        if (TurbineRotorBehavior.getInstanceFor(stack) == null) return null;
        return AbstractMaterialPartBehavior.getPartMaterial(stack).getMaterialRGB();
    }

    /** @return {@code TurbineRotorBehavior.getRotorDurabilityPercent}, or 0 with no (valid) rotor loaded. */
    public int getRotorDurabilityPercent() {
        ItemStack stack = getInsertedItem();
        TurbineRotorBehavior behavior = TurbineRotorBehavior.getInstanceFor(stack);
        return behavior != null ? behavior.getRotorDurabilityPercent(stack) : 0;
    }

    /** @return this holder's own ramp-state speed (0-{@link #MAX_SPEED}) -- see {@link #setActiveThisTick}. */
    public int getCurrentSpeed() {
        return currentSpeed;
    }

    public static int getMaxSpeed() {
        return MAX_SPEED;
    }

    /**
     * Wears the loaded rotor by {@code amount} (see {@code TurbineRotorBehavior#applyRotorDamage}'s own
     * contract: consumes the stack outright once its durability is exhausted) -- a no-op with no valid rotor
     * loaded. Unlike GT's own {@code MetaTileEntityRotorHolder}, this calls {@code markDirty()} itself since
     * {@code applyRotorDamage} mutates the stack's own NBT/count without going through
     * {@code onContentsChanged}.
     */
    public void damageRotor(int amount) {
        if (amount <= 0) return;
        ItemStack stack = getInsertedItem();
        TurbineRotorBehavior behavior = TurbineRotorBehavior.getInstanceFor(stack);
        if (behavior == null) return;
        behavior.applyRotorDamage(stack, amount);
        markDirty();
    }

    /**
     * Recomputes and immediately syncs {@link #rotorColor} the moment the loaded item actually changes (insert,
     * extract, or a GUI-driven swap) -- without this, a freshly-inserted rotor would show nothing at all until
     * the owning turbine controller's own tick loop next calls {@link #setActiveThisTick}, which does not
     * happen at all while the turbine itself cannot yet run (no steam, structure not formed, etc.) -- exactly
     * the "insert a rotor, holder still renders empty" symptom this fixes (confirmed via a real playtest,
     * 2026-10). Deliberately does not touch {@link #currentSpeed}/{@link #isRotorSpinning} -- swapping the item
     * does not by itself mean the turbine is (or isn't) actively generating, that's still
     * {@link #setActiveThisTick}'s own job.
     */
    @Override
    protected void onInsertedItemChanged() {
        if (getWorld() == null || getWorld().isRemote) return;
        int previousColor = rotorColor;
        Integer loadedColor = getRotorMaterialColor();
        rotorColor = loadedColor != null ? loadedColor : -1;
        if (rotorColor != previousColor) {
            writeCustomData(UPDATE_ROTOR_RENDER_STATE, this::writeRotorRenderState);
        }
    }

    /**
     * Called once per tick by the owning turbine controller (HP/LP), server-side only, with whether that
     * controller actually generated power this tick -- ramps {@link #currentSpeed} up (while active) or down
     * (while idle) exactly like GT's own {@code MetaTileEntityRotorHolder}, flips {@link #isRotorSpinning} when
     * crossing zero, and recomputes {@link #rotorColor} (cheap, and catches a mid-run rotor swap without needing
     * a separate {@code onContentsChanged} hook). Syncs to the client only when either value actually changed.
     */
    public void setActiveThisTick(boolean active) {
        if (getWorld() == null || getWorld().isRemote) return;
        currentSpeed = active ? Math.min(MAX_SPEED, currentSpeed + SPEED_RAMP_UP_PER_TICK) :
                Math.max(0, currentSpeed - SPEED_RAMP_DOWN_PER_TICK);
        boolean previousSpinning = isRotorSpinning;
        isRotorSpinning = currentSpeed > 0;

        int previousColor = rotorColor;
        Integer loadedColor = getRotorMaterialColor();
        rotorColor = loadedColor != null ? loadedColor : -1;

        if (isRotorSpinning != previousSpinning || rotorColor != previousColor) {
            writeCustomData(UPDATE_ROTOR_RENDER_STATE, this::writeRotorRenderState);
        }
    }

    private void writeRotorRenderState(@NotNull PacketBuffer buf) {
        buf.writeBoolean(isRotorSpinning);
        buf.writeInt(rotorColor);
    }

    /**
     * Mirrors GT's own {@code MetaTileEntityRotorHolder#readFromNBT}-adjacent sync handlers, which read the new
     * field values then <i>always</i> call {@code scheduleRenderUpdate()} -- without it, this block's actual
     * {@code IBlockState} never changes (the rotor's color/spin are plain fields, not block properties), so nothing
     * tells Minecraft's chunk renderer its cached mesh for this position is stale. The fields were updating
     * correctly the whole time; only the chunk's baked render was never invalidated to pick them up, which also
     * explains why an unrelated render-triggering event (breaking/replacing the block) appeared to "fix" it --
     * confirmed root cause, 2026-10, after the field-level sync logic itself was independently verified correct.
     */
    private void readRotorRenderState(@NotNull PacketBuffer buf) {
        isRotorSpinning = buf.readBoolean();
        rotorColor = buf.readInt();
        scheduleRenderUpdate();
    }

    @Override
    public void receiveCustomData(int dataId, @NotNull PacketBuffer buf) {
        super.receiveCustomData(dataId, buf);
        if (dataId == UPDATE_ROTOR_RENDER_STATE) {
            readRotorRenderState(buf);
        }
    }

    @Override
    public void writeInitialSyncData(@NotNull PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        writeRotorRenderState(buf);
    }

    @Override
    public void receiveInitialSyncData(@NotNull PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        readRotorRenderState(buf);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setInteger("CurrentSpeed", currentSpeed);
        data.setBoolean("RotorSpinning", isRotorSpinning);
        data.setInteger("RotorColor", rotorColor);
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        currentSpeed = data.getInteger("CurrentSpeed");
        isRotorSpinning = data.getBoolean("RotorSpinning");
        rotorColor = data.hasKey("RotorColor") ? data.getInteger("RotorColor") : -1;
    }

    /**
     * Same two-layer draw as GT's own {@code MetaTileEntityRotorHolder}: a static ring/base overlay, then the
     * rotor sprite itself (spinning or idle depending on {@link #isRotorSpinning}, tinted by {@link #rotorColor},
     * entirely absent while {@code rotorColor == -1}) -- see this class's own JavaDoc for why this class reads
     * GT's rendering assets ({@code Textures.ROTOR_HOLDER_OVERLAY}/{@code Textures.LARGE_TURBINE_ROTOR_RENDERER})
     * directly rather than needing any dedicated art of its own.
     */
    @SideOnly(Side.CLIENT)
    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        super.renderMetaTileEntity(renderState, translation, pipeline);
        Textures.ROTOR_HOLDER_OVERLAY.renderSided(getFrontFacing(), renderState, translation, pipeline);
        Textures.LARGE_TURBINE_ROTOR_RENDERER.renderSided(renderState, translation, pipeline, getFrontFacing(),
                getController() != null, rotorColor != -1, isRotorSpinning, rotorColor);
    }
}
