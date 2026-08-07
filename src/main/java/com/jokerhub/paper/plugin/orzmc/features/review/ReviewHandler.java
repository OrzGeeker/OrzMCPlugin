package com.jokerhub.paper.plugin.orzmc.features.review;

import java.util.UUID;

/**
 * 审核处理策略：审核通过时对申请人执行的动作。
 *
 * <p>由 {@link ReviewType} 持有，新增审核流程只需实现本接口并在枚举注册。</p>
 */
@FunctionalInterface
public interface ReviewHandler {

    /** 执行审核通过后的处理（如授予权限组）。 */
    void onApproved(UUID applicantId);
}
