package com.jokerhub.paper.plugin.orzmc.events;

import com.jokerhub.paper.plugin.orzmc.OrzMC;
import com.jokerhub.paper.plugin.orzmc.utils.OrzMessageParser;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrzTNTEvent implements Listener {
    
    // 配置相关
    private final FileConfiguration config;
    private List<Region> whiteListRegions = new ArrayList<>();
    private boolean enableTNT = false;
    private boolean enableRespawnAnchor = false;
    private int tntPlaceCooldown = 0; // 秒
    
    // 冷却时间跟踪
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();
    
    public OrzTNTEvent() {
        this.config = OrzMC.getPlugin().getConfig();
        loadConfig();
    }
    
    private void loadConfig() {
        // 从配置文件加载设置
        this.enableTNT = config.getBoolean("tnt.enabled", false);
        this.enableRespawnAnchor = config.getBoolean("respawn-anchor.enabled", false);
        this.tntPlaceCooldown = config.getInt("tnt.place-cooldown", 0);
        
        // 加载白名单区域
        whiteListRegions.clear();
        List<Map<?, ?>> regions = config.getMapList("regions.white-list");
        for (Map<?, ?> regionMap : regions) {
            Region region = new Region(
                ((Number)regionMap.get("minX")).intValue(),
                ((Number)regionMap.get("maxX")).intValue(),
                ((Number)regionMap.get("minY")).intValue(),
                ((Number)regionMap.get("maxY")).intValue(),
                ((Number)regionMap.get("minZ")).intValue(),
                ((Number)regionMap.get("maxZ")).intValue(),
                (String)regionMap.get("world")
            );
            whiteListRegions.add(region);
        }
    }
    
    @EventHandler
    public void onTNTPrime(TNTPrimeEvent event) {
        Block placedBlock = event.getBlock();
        
        // 如果全局禁用TNT且不在白名单区域，取消事件
        if (!enableTNT && !isInWhiteList(placedBlock)) {
            event.setCancelled(true);
            notifyTNTEvent(placedBlock, "TNT被点燃（已禁止）");
            return;
        }
        
        // 即使允许，也记录TNT点燃事件
        notifyTNTEvent(placedBlock, "TNT被点燃");
    }
    
    @EventHandler
    public void onPlaceBlock(BlockPlaceEvent event) {
        Block placedBlock = event.getBlockPlaced();
        Material placedBlockType = placedBlock.getType();
        Player player = event.getPlayer();
        
        // 处理TNT放置
        if (placedBlockType == Material.TNT) {
            handleTNTPlace(event, player, placedBlock);
            return;
        }
        
        // 处理重生锚放置
        if (placedBlockType == Material.RESPAWN_ANCHOR && !enableRespawnAnchor) {
            event.setCancelled(true);
            player.sendMessage(Component.text("重生锚放置已被管理员禁用").color(TextColor.color(0xFF5555)));
            return;
        }
    }
    
    private void handleTNTPlace(BlockPlaceEvent event, Player player, Block placedBlock) {
        // 检查冷却时间
        if (tntPlaceCooldown > 0 && checkCooldown(player)) {
            event.setCancelled(true);
            long remaining = (playerCooldowns.get(player.getUniqueId()) + tntPlaceCooldown * 1000L - System.currentTimeMillis()) / 1000;
            player.sendMessage(Component.text()
                .append(Component.text("放置TNT冷却中，请等待 "))
                .append(Component.text(remaining + "秒").color(TextColor.color(0xFFAA00)))
                .build());
            return;
        }
        
        // 如果全局禁用TNT且不在白名单区域，取消放置
        if (!enableTNT && !isInWhiteList(placedBlock)) {
            event.setCancelled(true);
            player.sendMessage(Component.text("TNT放置已被管理员禁用").color(TextColor.color(0xFF5555)));
            return;
        }
        
        // 记录冷却时间
        if (tntPlaceCooldown > 0) {
            playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        }
        
        // 发送放置通知
        sendPlacementNotification(player, placedBlock, "TNT");
    }
    
    private boolean checkCooldown(Player player) {
        if (!playerCooldowns.containsKey(player.getUniqueId())) {
            return false;
        }
        long lastPlaceTime = playerCooldowns.get(player.getUniqueId());
        return System.currentTimeMillis() - lastPlaceTime < tntPlaceCooldown * 1000L;
    }
    
    @EventHandler
    public void onBlockPreDispense(BlockPreDispenseEvent event) {
        ItemStack itemStack = event.getItemStack();
        Material itemType = itemStack.getType();
        
        // 只处理TNT相关物品
        if (itemType != Material.TNT && itemType != Material.TNT_MINECART) {
            return;
        }
        
        Block dispenser = event.getBlock();
        
        // 如果全局禁用TNT且不在白名单区域，取消发射
        if (!enableTNT && !isInWhiteList(dispenser)) {
            event.setCancelled(true);
            notifyTNTEvent(dispenser, "发射" + itemType.name() + "被禁止");
        }
    }
    
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Block block = event.getBlock();
        notifyExplosionEvent(block.getLocation(), block.getType().name() + "爆炸");
    }
    
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        notifyExplosionEvent(event.getLocation(), event.getEntityType().name() + "爆炸");
    }
    
    // 区域检查方法
    private boolean isInWhiteList(Block block) {
        Location loc = block.getLocation();
        for (Region region : whiteListRegions) {
            if (region.contains(loc)) {
                return true;
            }
        }
        return false;
    }
    
    // 通知方法
    private void notifyTNTEvent(Block block, String message) {
        TextComponent msg = Component.text()
            .append(Component.text("[TNT警报] ").color(TextColor.color(0xFF5555)))
            .append(playerInfo(block)) // 尝试获取放置玩家
            .append(Component.space())
            .append(locationComponent(block))
            .append(Component.space())
            .append(Component.text(message))
            .build();
        
        OrzMC.server().sendMessage(msg);
        OrzMC.sendPublicMessage("[TNT警报] " + locationString(block) + message);
    }
    
    private void notifyExplosionEvent(Location location, String message) {
        TextComponent msg = Component.text()
            .append(Component.text("[爆炸警报] ").color(TextColor.color(0xFFAA00)))
            .append(locationComponent(location))
            .append(Component.space())
            .append(Component.text(message))
            .build();
        
        OrzMC.server().sendMessage(msg);
        OrzMC.sendPublicMessage("[爆炸警报] " + locationString(location) + message);
    }
    
    private void sendPlacementNotification(Player player, Block block, String itemName) {
        TextComponent msg = Component.text()
            .append(playerInfo(player))
            .append(Component.space())
            .append(Component.text("在"))
            .append(locationComponent(block))
            .append(Component.space())
            .append(Component.text("放置了 " + itemName))
            .build();
        
        OrzMC.server().sendMessage(msg);
        OrzMC.sendPublicMessage(OrzMessageParser.playerDisplayName(player) + 
            " 在" + locationString(block) + "放置了 " + itemName);
    }
    
    // 信息构建工具
    private TextComponent playerInfo(Player player) {
        return Component.text(player.getName())
            .color(TextColor.color(0xFF5555))
            .hoverEvent(HoverEvent.showText(Component.text("点击查看玩家信息")))
            .clickEvent(ClickEvent.runCommand("/profile " + player.getName()));
    }
    
    private TextComponent playerInfo(Block block) {
        // 这里可以添加获取最后修改该方块的玩家的逻辑
        // 需要配合区块记录插件使用
        return Component.text("未知玩家").color(TextColor.color(0xAAAAAA));
    }
    
    private TextComponent locationComponent(Block block) {
        return locationComponent(block.getLocation());
    }
    
    private TextComponent locationComponent(Location location) {
        String locString = locationString(location);
        return Component.text(locString)
            .color(TextColor.color(0x55FF55))
            .hoverEvent(HoverEvent.showText(Component.text("点击复制坐标")))
            .clickEvent(ClickEvent.copyToClipboard(locString.trim()));
    }
    
    private String locationString(Block block) {
        return locationString(block.getLocation());
    }
    
    private String locationString(Location location) {
        return String.format(" [%s] %d %d %d ", 
            location.getWorld().getName(),
            location.getBlockX(),
            location.getBlockY(),
            location.getBlockZ());
    }
    
    // 区域内部类
    private static class Region {
        private final int minX, maxX, minY, maxY, minZ, maxZ;
        private final String world;
        
        public Region(int minX, int maxX, int minY, int maxY, int minZ, int maxZ, String world) {
            this.minX = Math.min(minX, maxX);
            this.maxX = Math.max(minX, maxX);
            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);
            this.minZ = Math.min(minZ, maxZ);
            this.maxZ = Math.max(minZ, maxZ);
            this.world = world;
        }
        
        public boolean contains(Location loc) {
            return loc.getWorld().getName().equals(world) &&
                   loc.getX() >= minX && loc.getX() <= maxX &&
                   loc.getY() >= minY && loc.getY() <= maxY &&
                   loc.getZ() >= minZ && loc.getZ() <= maxZ;
        }
    }
}
