package com.jokerhub.paper.plugin.orzmc.guidebook.models;

import org.jetbrains.annotations.NotNull;

/**
 * @param newlineCount 新增：换行数量
 */ // 链接内容类
public record LinkContent(String content, String url, String hoverText, TextStyle style, int newlineCount) {

    @Override
    public @NotNull String toString() {
        return "LinkContent{content='" + content + "', url='" + url +
                "', hoverText='" + hoverText + "', style=" + style +
                ", newlineCount=" + newlineCount + "}";
    }
}
