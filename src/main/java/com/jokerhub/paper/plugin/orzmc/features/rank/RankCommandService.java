package com.jokerhub.paper.plugin.orzmc.features.rank;

import com.jokerhub.paper.plugin.orzmc.features.review.ReviewService;
import com.jokerhub.paper.plugin.orzmc.features.review.ReviewType;
import com.jokerhub.paper.plugin.orzmc.infra.styles.OrzTextStyles;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

/**
 * 权限查询命令服务：/rank（玩家查自己）/rank &lt;玩家&gt;（admin 查指定玩家）。
 *
 * <p>返回：当前权限组 + 在线时长/晋升进度 + 下一步可申请项（由审核注册表反向生成）。
 * 申请/审核命令不再在此（见 {@code ReviewCommandService}）。</p>
 */
public final class RankCommandService {

    private final RankService service;
    private final ReviewService reviewService;
    private final OrzTextStyles styles;

    public RankCommandService(RankService service, ReviewService reviewService, OrzTextStyles styles) {
        this.service = service;
        this.reviewService = reviewService;
        this.styles = styles;
    }

    public sealed interface Result permits Result.Success, Result.Failure {
        record Success(Component message) implements Result {}

        record Failure(Component message) implements Result {}
    }

    /** /rank — 玩家查自己的权限组与进度。 */
    public Result status(Player player) {
        return statusOf(player.getUniqueId());
    }

    /** /rank &lt;玩家&gt; — admin 查指定玩家。 */
    public Result statusOf(UUID playerId) {
        String group = service.currentGroup(playerId);
        long minutes = service.playtimeMinutes(playerId);
        long threshold = service.memberThresholdMinutes();
        String progress = minutes >= threshold ? "✅ 已达标" : "还需 " + (threshold - minutes) + " 分钟";
        String next = nextApplications(playerId);

        Component message = styles.info("你的当前权限组：" + RankService.groupDisplayName(group) + "（" + group + "）\n"
                + "已在线时长：" + minutes + " 分钟 / 晋升会员阈值 " + threshold + " 分钟（" + progress + "）\n"
                + "下一步可申请：" + next);
        return new Result.Success(message);
    }

    /** 反向生成「下一步可申请」：注册表中资格预检通过的类型。 */
    private String nextApplications(UUID playerId) {
        List<String> available = reviewService.registeredTypes().stream()
                .filter(t -> t.isEligible(playerId))
                .map(this::formatAvailableType)
                .collect(Collectors.toList());
        return available.isEmpty() ? "无（当前无可申请项）" : String.join("；", available);
    }

    /** /rank demote <玩家> — admin 降级一级（钳位：builder→member→default，链底 no-op）。 */
    public Result demote(UUID playerId) {
        String target = service.demote(playerId);
        if (target == null) {
            return new Result.Failure(styles.error("该玩家已在最低等级（访客），无法再降级。"));
        }
        return new Result.Success(styles.success("已降级为" + RankService.groupDisplayName(target) + "（" + target + "）。"));
    }

    private String formatAvailableType(ReviewType type) {
        return type.displayName() + "（/apply " + type.commandKey() + "）";
    }
}
