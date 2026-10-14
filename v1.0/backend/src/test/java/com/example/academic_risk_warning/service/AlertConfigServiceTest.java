package com.example.academic_risk_warning.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.academic_risk_warning.entity.AlertRuleConfig;
import com.example.academic_risk_warning.mapper.AlertRuleConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * AlertConfigService 单元测试
 * <p>
 * 使用 Mockito 模拟 AlertRuleConfigMapper 和 WarningGenerationService，
 * 验证预警规则配置的完整 CRUD 操作及缓存失效逻辑。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AlertConfigService 单元测试")
class AlertConfigServiceTest {

    @Mock
    private AlertRuleConfigMapper mapper;

    @Mock
    private WarningGenerationService warningGenerationService;

    private AlertConfigService alertConfigService;

    @BeforeEach
    void setUp() {
        alertConfigService = new AlertConfigService(mapper, warningGenerationService);
    }

    // ==================== listBySystem ====================

    @Nested
    @DisplayName("listBySystem — 按体系查询")
    class ListBySystem {

        @Test
        @DisplayName("返回该系统类型的配置列表")
        void shouldReturnConfigsForSystem() {
            AlertRuleConfig c1 = buildConfig(1L, "FRESHMAN", 101L);
            AlertRuleConfig c2 = buildConfig(2L, "FRESHMAN", 102L);

            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(c1, c2));

            List<AlertRuleConfig> result = alertConfigService.listBySystem("FRESHMAN");
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("无数据时返回空列表")
        void shouldReturnEmptyWhenNoData() {
            when(mapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            List<AlertRuleConfig> result = alertConfigService.listBySystem("FRESHMAN");
            assertTrue(result.isEmpty());
        }
    }

    // ==================== listAll ====================

    @Nested
    @DisplayName("listAll — 查询全部")
    class ListAll {

        @Test
        @DisplayName("返回全部配置")
        void shouldReturnAllConfigs() {
            when(mapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Arrays.asList(
                            buildConfig(1L, "FRESHMAN", null),
                            buildConfig(2L, "SENIOR", 201L)));

            List<AlertRuleConfig> result = alertConfigService.listAll();
            assertEquals(2, result.size());
        }
    }

    // ==================== getById ====================

    @Nested
    @DisplayName("getById — 按ID查询")
    class GetById {

        @Test
        @DisplayName("存在时返回配置")
        void shouldReturnConfigWhenExists() {
            AlertRuleConfig config = buildConfig(1L, "FRESHMAN", null);
            when(mapper.selectById(1L)).thenReturn(config);

            AlertRuleConfig result = alertConfigService.getById(1L);
            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("不存在时返回null")
        void shouldReturnNullWhenNotExists() {
            when(mapper.selectById(999L)).thenReturn(null);
            assertNull(alertConfigService.getById(999L));
        }
    }

    // ==================== save ====================

    @Nested
    @DisplayName("save — 创建/更新配置")
    class Save {

        @Test
        @DisplayName("id 为 null → insert + 使缓存失效")
        void shouldInsertWhenIdIsNull() {
            AlertRuleConfig config = buildConfig(null, "FRESHMAN", 101L);

            AlertRuleConfig result = alertConfigService.save(config);

            verify(mapper).insert(config);
            verify(mapper, never()).updateById(any(AlertRuleConfig.class));
            verify(warningGenerationService).invalidateRuleCache("FRESHMAN");
            assertEquals(config, result);
        }

        @Test
        @DisplayName("id 不为 null → updateById + 使缓存失效")
        void shouldUpdateWhenIdIsNotNull() {
            AlertRuleConfig config = buildConfig(5L, "SENIOR", 201L);

            AlertRuleConfig result = alertConfigService.save(config);

            verify(mapper).updateById(config);
            verify(mapper, never()).insert(any(AlertRuleConfig.class));
            verify(warningGenerationService).invalidateRuleCache("SENIOR");
            assertEquals(config, result);
        }
    }

    // ==================== delete ====================

    @Nested
    @DisplayName("delete — 删除配置")
    class Delete {

        @Test
        @DisplayName("存在时 → deleteById + 使缓存失效")
        void shouldDeleteAndInvalidateCache() {
            AlertRuleConfig config = buildConfig(3L, "FRESHMAN", null);
            when(mapper.selectById(3L)).thenReturn(config);

            alertConfigService.delete(3L);

            verify(mapper).selectById(3L);
            verify(mapper).deleteById(3L);
            verify(warningGenerationService).invalidateRuleCache("FRESHMAN");
        }

        @Test
        @DisplayName("不存在时 → 不执行 deleteById，不使缓存失效")
        void shouldNotDeleteWhenNotExists() {
            when(mapper.selectById(999L)).thenReturn(null);

            assertDoesNotThrow(() -> alertConfigService.delete(999L));

            verify(mapper).selectById(999L);
            verify(mapper, never()).deleteById(anyLong());
            verify(warningGenerationService, never()).invalidateRuleCache(anyString());
        }
    }

    // ==================== 辅助 ====================

    private AlertRuleConfig buildConfig(Long id, String systemType, Long courseId) {
        AlertRuleConfig c = new AlertRuleConfig();
        c.setId(id);
        c.setSystemType(systemType);
        c.setCourseId(courseId);
        c.setWeightAcademic(new BigDecimal("0.4"));
        c.setWeightHomework(new BigDecimal("0.2"));
        c.setWeightKnowledge(new BigDecimal("0.2"));
        c.setWeightAttendance(new BigDecimal("0.1"));
        c.setWeightHistory(new BigDecimal("0.1"));
        c.setThresholdRed(new BigDecimal("80"));
        c.setThresholdOrange(new BigDecimal("60"));
        c.setThresholdYellow(new BigDecimal("40"));
        c.setIsActive(true);
        return c;
    }
}
