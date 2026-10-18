package com.game.common.trading.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证商品表可编辑字段的更新策略：
 * 空字符串（或 null）不会进入 UPDATE 的 SET 子句，保持"传空串 = 不修改该字段"的原有业务语义，
 * 也避免把 decimal 类型的价格列更新成空串导致 SQL 报错。
 */
class GameEntityUpdateStrategyTest {

    /** 纯单元测试没有 Spring 容器，需要手动初始化 TableInfo（与缓存测试里的做法一致） */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), GameEntity.class);
    }

    @Test
    @DisplayName("5 个可编辑字段：更新时空串不参与 SET（NOT_EMPTY）")
    void editableFieldsShouldSkipEmptyValueOnUpdate() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(GameEntity.class);

        for (String property : List.of("coverImage", "wallImages", "gameName", "price", "details")) {
            assertThat(field(tableInfo, property).getUpdateStrategy())
                    .as("字段 %s 的更新策略", property)
                    .isEqualTo(FieldStrategy.NOT_EMPTY);
        }
    }

    @Test
    @DisplayName("逻辑删除字段与时间字段保持默认策略（NOT_NULL），主键不参与更新")
    void otherFieldsShouldKeepDefaultStrategy() {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(GameEntity.class);

        // MyBatis-Plus 会把注解上的 DEFAULT 解析成全局默认策略 NOT_NULL：
        // 即 null 不参与 SET、非空值正常更新 —— 与改动前完全一致
        assertThat(tableInfo.getFieldList())
                .filteredOn(f -> List.of("isDeleted", "createTime", "updateTime").contains(f.getProperty()))
                .hasSize(3)
                .allSatisfy(f -> assertThat(f.getUpdateStrategy())
                        .as("字段 %s", f.getProperty())
                        .isEqualTo(FieldStrategy.NOT_NULL));

        assertThat(tableInfo.getFieldList())
                .extracting(TableFieldInfo::getProperty)
                .doesNotContain("itemId");
    }

    private static TableFieldInfo field(TableInfo tableInfo, String property) {
        return tableInfo.getFieldList().stream()
                .filter(f -> property.equals(f.getProperty()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("未找到字段：" + property));
    }
}
