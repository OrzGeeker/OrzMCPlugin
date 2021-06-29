package com.jokerhub.paper.plugin.orzmc.commands;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.NotNull;

public class ArmorStands implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(sender instanceof Player) {
            Player player = (Player) sender;

            // https://haselkern.com/Minecraft-ArmorStand/
            ArmorStand amrorStand = (ArmorStand) player.getWorld().spawnEntity(player.getLocation(), EntityType.ARMOR_STAND);
            amrorStand.setHelmet(new ItemStack(Material.JUNGLE_PLANKS));
            amrorStand.setInvulnerable(true);
            amrorStand.setGlowing(true);
            amrorStand.setItemInHand(new ItemStack(Material.DIAMOND_AXE));
            amrorStand.setArms(true);
            amrorStand.setBodyPose(new EulerAngle(Math.toRadians(131),Math.toRadians(106), Math.toRadians(79)));
            amrorStand.setHeadPose(new EulerAngle(Math.toRadians(90), 0,0));
            amrorStand.setLeftArmPose(new EulerAngle(0,0,Math.toRadians(270)));
            amrorStand.setRightArmPose(new EulerAngle(0,0,Math.toRadians(90)));
        }
        return true;
    }
}
