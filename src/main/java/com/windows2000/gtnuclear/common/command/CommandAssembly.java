package com.windows2000.gtnuclear.common.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import com.windows2000.gtnuclear.common.item.AssemblyLength;
import com.windows2000.gtnuclear.common.item.GTNLItems;
import com.windows2000.gtnuclear.common.item.ItemControlRod;
import com.windows2000.gtnuclear.common.item.ItemFuelRod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Development command (stage 1-3 (e), 2026-11-21): {@code /gtnuclear assembly <fuel|rod> <type> <length>} gives the
 * player a fuel assembly or control rod cluster of the requested fabricated length -- until the fabrication
 * recipes exist (roadmap J), the creative tab only offers lengths 9/17/35. OP level 2.
 */
public class CommandAssembly extends CommandBase {

    @NotNull
    @Override
    public String getName() {
        return "gtnuclear";
    }

    @NotNull
    @Override
    public String getUsage(@NotNull ICommandSender sender) {
        return "/gtnuclear assembly <fuel|rod> <type> <length 1-" + AssemblyLength.MAX_LAYERS + ">";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                        @NotNull String[] args) throws CommandException {
        if (args.length != 4 || !args[0].equals("assembly")) throw new WrongUsageException(getUsage(sender));
        Item item = findItem(args[1], args[2]);
        if (item == null) throw new WrongUsageException(getUsage(sender) + " -- types: " + typeNames(args[1]));
        int length = parseInt(args[3], 1, AssemblyLength.MAX_LAYERS);
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        ItemStack stack = AssemblyLength.withLength(new ItemStack(item), length);
        if (!player.inventory.addItemStackToInventory(stack)) player.dropItem(stack, false);
        sender.sendMessage(new TextComponentString("Gave " + args[1] + " " + args[2] + ", length " + length));
    }

    @Nullable
    private static Item findItem(@NotNull String kind, @NotNull String type) {
        if (kind.equals("fuel")) {
            for (ItemFuelRod rod : GTNLItems.ALL_FUEL_RODS) if (typeName(rod).equals(type)) return rod;
        } else if (kind.equals("rod")) {
            for (ItemControlRod rod : GTNLItems.ALL_CONTROL_RODS) if (typeName(rod).equals(type)) return rod;
        }
        return null;
    }

    /** The translation key's last segment, e.g. {@code item.gtnuclear.fuel_rod.leu -> leu}. */
    @NotNull
    private static String typeName(@NotNull Item item) {
        String key = item.getTranslationKey();
        return key.substring(key.lastIndexOf('.') + 1);
    }

    @NotNull
    private static List<String> typeNames(@NotNull String kind) {
        List<String> names = new ArrayList<>();
        if (kind.equals("fuel")) GTNLItems.ALL_FUEL_RODS.forEach(rod -> names.add(typeName(rod)));
        if (kind.equals("rod")) GTNLItems.ALL_CONTROL_RODS.forEach(rod -> names.add(typeName(rod)));
        return names;
    }

    @NotNull
    @Override
    public List<String> getTabCompletions(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                                          @NotNull String[] args, @Nullable BlockPos targetPos) {
        return switch (args.length) {
            case 1 -> getListOfStringsMatchingLastWord(args, "assembly");
            case 2 -> getListOfStringsMatchingLastWord(args, "fuel", "rod");
            case 3 -> getListOfStringsMatchingLastWord(args, typeNames(args[1]));
            case 4 -> getListOfStringsMatchingLastWord(args, "9", "17", "35");
            default -> Collections.emptyList();
        };
    }
}
