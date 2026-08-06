package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * 晋升命令服务：/apply（member 申请 builder）、/rank（查询自己进度）。
 */
public final class RankCommandService {

    private final RankService service;
    private final OrzTextStyles styles;

    public RankCommandService(RankService service, OrzTextStyles styles) {
        this.service = service;
        this.styles = styles;
    }

    public sealed interface Result permits Result.Success, Result.Failure {
        record Success(Component message) implements Result {}

        record Failure(Component message) implements Result {}
    }

    /** /apply — member 玩家申请晋升 builder。 */
    public Result apply(Player player) {
        UUID id = player.getUniqueId();
        if (service.hasPendingApplication(id)) {
            return new Result.Failure(styles.error("你已提交过申请，请等待管理员审核。"));
        }
        service.applyForBuilder(id);
        return new Result.Success(styles.success("申请已提交，管理员审核通过后将自动晋升为建造者。"));
    }

    /** /rank — 查询自己的晋升进度。 */
    public Result status(Player player) {
        UUID id = player.getUniqueId();
        long minutes = service.playtimeMinutes(id);
        long threshold = service.memberThresholdMinutes();
        String status = minutes >= threshold ? "✅ 已达到晋升条件" : "还需 " + (threshold - minutes) + " 分钟";
        return new Result.Success(styles.info("你的在线时长: " + minutes + " 分钟（晋升会员需 " + threshold + " 分钟）" + status));
    }

    /** /rank approve <name> — 管理员审核通过申请（admin 权限）。 */
    public Result approve(Player admin, String playerName) {
        UUID id = service.resolvePlayerId(playerName);
        if (id == null) {
            return new Result.Failure(styles.error("找不到玩家: " + playerName));
        }
        if (!service.hasPendingApplication(id)) {
            return new Result.Failure(styles.error(playerName + " 没有待审核的申请。"));
        }
        service.reviewApplication(id, true);
        return new Result.Success(styles.success(playerName + " 已晋升为建造者。"));
    }

    /** /rank reject <name> — 管理员拒绝申请。 */
    public Result reject(Player admin, String playerName) {
        UUID id = service.resolvePlayerId(playerName);
        if (id == null) {
            return new Result.Failure(styles.error("找不到玩家: " + playerName));
        }
        service.reviewApplication(id, false);
        return new Result.Success(styles.success("已拒绝 " + playerName + " 的申请。"));
    }
}
