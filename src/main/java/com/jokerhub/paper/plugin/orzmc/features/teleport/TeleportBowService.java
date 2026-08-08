package com.jokerhub.paper.plugin.orzmc.features.teleport;

import com.jokerhub.paper.plugin.orzmc.infra.core.OrzConstants;
import com.jokerhub.paper.plugin.orzmc.infra.server.ServerFacade;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class TeleportBowService {
    public static final String name = "传送弓";
    private final OrzTextStyles styles;
    private final TeleportBowTexts texts;
    private final NamespacedKey keyTpBow;

    public TeleportBowService(ServerFacade server, OrzTextStyles styles) {
        this.styles = styles;
        this.texts = new TeleportBowTexts(styles);
        this.keyTpBow = server.key(OrzConstants.TPBOW_KEY);
    }

    public TextComponent prefix() {
        return Component.text("传送弓");
    }

    public void giveAndEquip(Player player) {
        ItemStack teleport_bow = new ItemStack(Material.BOW);
        ItemMeta meta = teleport_bow.getItemMeta();
        meta.addEnchant(Enchantment.INFINITY, 1, true);
        meta.displayName(Component.text(name));
        java.util.ArrayList<Component> loreList = new java.util.ArrayList<>();
        loreList.add(Component.text("可以把你传送到箭落地的位置"));
        meta.lore(loreList);
        meta.getPersistentDataContainer().set(keyTpBow, PersistentDataType.BYTE, (byte) 1);
        teleport_bow.setItemMeta(meta);
        ItemStack prev = player.getInventory().getItemInMainHand();
        if (prev.getType() != Material.AIR) {
            player.getInventory().addItem(prev);
        }
        player.getInventory().setItemInMainHand(teleport_bow);
        ItemStack arrow = new ItemStack(Material.ARROW);
        player.getInventory().addItem(arrow);
        player.sendMessage(styles.success("你获得了" + name));
    }

    public boolean isTPBowArrow(org.bukkit.entity.Projectile proj) {
        if (proj instanceof org.bukkit.entity.Arrow arrow) {
            return arrow.getPersistentDataContainer().has(keyTpBow, org.bukkit.persistence.PersistentDataType.BYTE);
        }
        return false;
    }

    public void markArrow(org.bukkit.event.entity.EntityShootBowEvent event) {
        org.bukkit.inventory.meta.ItemMeta meta =
                event.getBow() != null ? event.getBow().getItemMeta() : null;
        if (meta != null
                && meta.getPersistentDataContainer().has(keyTpBow, org.bukkit.persistence.PersistentDataType.BYTE)) {
            if (event.getProjectile() instanceof org.bukkit.entity.Arrow arrow) {
                // 射线传送：射箭瞬间按玩家视线传送，不依赖箭落地（修复未加载区块问题）
                teleportByRay((org.bukkit.entity.Player) event.getEntity(), arrow);
                // 玩家已传送，移除箭标记避免落地时重复传送
                arrow.getPersistentDataContainer().remove(keyTpBow);
            }
        }
    }

    /**
     * 射线传送：射箭瞬间按玩家视线方向射线检测落点并传送。
     *
     * <p>实测根因（2026-08-08）：原实现依赖箭落地触发 {@code ProjectileHitEvent}，但箭
     * 飞入未加载区块（超出 view-distance）时实体被卸载，hit 事件永不触发，传送弓失效。
     * 预加载方案无效（Paper 会立即卸载视距外区块）。改为发射瞬间按视线射线计算落点，
     * 玩家传送会强制加载目标区块（原版机制），彻底绕开箭事件依赖。
     *
     * <p>射线穿过未加载区块时 {@code rayTraceBlocks} 返回 null，此时沿视线方向逐个
     * 异步加载路径区块并在加载完成回调内重试射线（回调时区块已加载未卸载），
     * 覆盖远距离（出加载区）场景。</p>
     */
    private static final int TRAJECTORY_CHUNKS = 8;

    void teleportByRay(org.bukkit.entity.Player player, org.bukkit.entity.Arrow arrow) {
        org.bukkit.Location eye = player.getEyeLocation();
        org.bukkit.World world = eye.getWorld();
        if (world == null) {
            return;
        }
        org.bukkit.util.Vector dir = eye.getDirection();
        if (dir == null || dir.lengthSquared() < 1.0e-6) {
            return;
        }
        org.bukkit.util.RayTraceResult result =
                world.rayTraceBlocks(eye, dir, 120.0, org.bukkit.FluidCollisionMode.ALWAYS);
        if (result == null || result.getHitBlock() == null) {
            retryWithChunkLoad(player, world, eye, dir, 0);
            return;
        }
        handleRayResult(player, dir, result);
    }

    private void retryWithChunkLoad(
            org.bukkit.entity.Player player,
            org.bukkit.World world,
            org.bukkit.Location eye,
            org.bukkit.util.Vector dir,
            int chunkIndex) {
        if (chunkIndex >= TRAJECTORY_CHUNKS) {
            player.sendMessage(texts.logText("瞄准位置无效，请对准地面!").color(styles.colorError()));
            return;
        }
        org.bukkit.Location step = eye.clone().add(dir.clone().multiply((chunkIndex + 1) * 16.0));
        int cx = step.getBlockX() >> 4;
        int cz = step.getBlockZ() >> 4;
        world.getChunkAtAsync(cx, cz, false, chunk -> {
            // 回调在主线程且区块刚加载（未卸载），立即重试射线
            org.bukkit.util.RayTraceResult result =
                    world.rayTraceBlocks(eye, dir, 120.0, org.bukkit.FluidCollisionMode.ALWAYS);
            if (result == null || result.getHitBlock() == null) {
                retryWithChunkLoad(player, world, eye, dir, chunkIndex + 1);
            } else {
                handleRayResult(player, dir, result);
            }
        });
    }

    private void handleRayResult(
            org.bukkit.entity.Player player, org.bukkit.util.Vector dir, org.bukkit.util.RayTraceResult result) {
        org.bukkit.block.Block hit = result.getHitBlock();
        org.bukkit.Material hitType = hit.getType();
        if (hitType == org.bukkit.Material.WATER) {
            player.sendMessage(texts.logText("箭射进了水里!").color(styles.colorError()));
            return;
        }
        if (hitType == org.bukkit.Material.LAVA) {
            player.sendMessage(texts.logText("箭射进了岩浆里!").color(styles.colorError()));
            return;
        }
        org.bukkit.Location landing = hit.getLocation().add(0.5, 1.0, 0.5).setDirection(dir);
        if (!withinWorldBounds(landing)) {
            player.sendMessage(texts.logText("目标高度不合法!").color(styles.colorError()));
            return;
        }
        org.bukkit.Location safe = findNearestSafe(landing, dir);
        if (safe == null) {
            player.sendMessage(texts.logText("目标位置不可站立!").color(styles.colorError()));
            return;
        }
        player.teleportAsync(safe).thenRun(() -> {
            player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_CAT_PURR, 1.0F, 1.0F);
            player.sendMessage(texts.logText("传送完成!").color(styles.colorSuccess()));
        });
    }

    private static final java.util.EnumSet<org.bukkit.Material> DANGEROUS = java.util.EnumSet.of(
            org.bukkit.Material.LAVA,
            org.bukkit.Material.WATER,
            org.bukkit.Material.MAGMA_BLOCK,
            org.bukkit.Material.CACTUS,
            org.bukkit.Material.FIRE,
            org.bukkit.Material.SOUL_FIRE,
            org.bukkit.Material.CAMPFIRE,
            org.bukkit.Material.SOUL_CAMPFIRE,
            org.bukkit.Material.POWDER_SNOW);

    private boolean withinWorldBounds(org.bukkit.Location loc) {
        if (loc == null) return false;
        org.bukkit.World w = loc.getWorld();
        if (w == null) return false;
        int y = loc.getBlockY();
        int min = w.getMinHeight();
        int max = w.getMaxHeight();
        return y >= min + 1 && y <= max - 2;
    }

    private org.bukkit.Location toBlockCenter(org.bukkit.Location loc, org.bukkit.util.Vector dir) {
        org.bukkit.World w = loc.getWorld();
        if (w == null) return null;
        int bx = loc.getBlockX();
        int by = loc.getBlockY();
        int bz = loc.getBlockZ();
        float yaw = vectorYaw(dir);
        return new org.bukkit.Location(w, bx + 0.5, by, bz + 0.5, yaw, 0f);
    }

    private boolean isStandable(org.bukkit.Location loc) {
        org.bukkit.block.Block foot = loc.getBlock();
        org.bukkit.block.Block head = foot.getRelative(0, 1, 0);
        org.bukkit.block.Block ground = foot.getRelative(0, -1, 0);
        org.bukkit.Material ft = foot.getType();
        org.bukkit.Material ht = head.getType();
        org.bukkit.Material gt = ground.getType();
        if (!ft.isAir() || !ht.isAir()) return false;
        if (DANGEROUS.contains(ft) || DANGEROUS.contains(ht) || DANGEROUS.contains(gt)) return false;
        return gt.isSolid();
    }

    private org.bukkit.Location findStandableAtOrAbove(
            org.bukkit.World world, int bx, int by, int bz, org.bukkit.util.Vector facing) {
        for (int dy = 0; dy <= 1; dy++) {
            org.bukkit.Location loc =
                    new org.bukkit.Location(world, bx + 0.5, by + dy, bz + 0.5, vectorYaw(facing), 0f);
            if (withinWorldBounds(loc) && isStandable(loc)) {
                return loc;
            }
        }
        return null;
    }

    private org.bukkit.Location findNearestSafe(org.bukkit.Location center, org.bukkit.util.Vector facing) {
        org.bukkit.World w = center.getWorld();
        if (w == null) return null;
        int bx = center.getBlockX();
        int by = center.getBlockY();
        int bz = center.getBlockZ();
        final org.bukkit.util.Vector facingNorm = facing.clone().normalize();
        org.bukkit.Location standable = findStandableAtOrAbove(w, bx, by, bz, facingNorm);
        if (standable != null) return standable;
        java.util.List<org.bukkit.Location> candidates = new java.util.ArrayList<>();
        final int radius = 1;
        for (int r = 1; r == radius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    candidates.add(
                            new org.bukkit.Location(w, bx + dx + 0.5, by, bz + dz + 0.5, vectorYaw(facingNorm), 0f));
                }
            }
        }
        candidates.sort((a, b) -> {
            org.bukkit.util.Vector va = a.clone().subtract(center).toVector();
            org.bukkit.util.Vector vb = b.clone().subtract(center).toVector();
            double da = va.normalize().dot(facingNorm);
            double db = vb.normalize().dot(facingNorm);
            return Double.compare(db, da);
        });
        for (org.bukkit.Location cand : candidates) {
            org.bukkit.Location found =
                    findStandableAtOrAbove(w, cand.getBlockX(), cand.getBlockY(), cand.getBlockZ(), facingNorm);
            if (found != null) return found;
        }
        return null;
    }

    private float vectorYaw(org.bukkit.util.Vector v) {
        double yawRad = Math.atan2(-v.getX(), v.getZ());
        return (float) Math.toDegrees(yawRad);
    }

    public void handleArrowHit(org.bukkit.entity.Arrow arrow, org.bukkit.entity.Player player) {
        if (arrow.isInWater()) {
            player.sendMessage(texts.logText("箭射进了水里!").color(styles.colorError()));
            return;
        }
        if (arrow.isInLava()) {
            player.sendMessage(texts.logText("箭射进了岩浆里!").color(styles.colorError()));
            return;
        }
        org.bukkit.Location base = arrow.getLocation();
        org.bukkit.World pw = player.getWorld();
        org.bukkit.World tw = base.getWorld();
        if (!pw.equals(tw)) {
            player.sendMessage(texts.logText("无法跨世界传送!").color(styles.colorError()));
            return;
        }
        org.bukkit.util.Vector dir = arrow.getVelocity();
        org.bukkit.Location center = toBlockCenter(base, dir);
        if (!withinWorldBounds(center)) {
            player.sendMessage(texts.logText("目标高度不合法!").color(styles.colorError()));
            return;
        }
        org.bukkit.Location safe = findNearestSafe(center, dir);
        if (safe == null) {
            player.sendMessage(texts.logText("目标位置不可站立!").color(styles.colorError()));
            return;
        }
        player.teleport(safe);
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_CAT_PURR, 1.0F, 1.0F);
        player.sendMessage(texts.logText("传送完成!").color(styles.colorSuccess()));
    }
}
