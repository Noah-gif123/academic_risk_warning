package com.example.academic_risk_warning.controller.stats;

import com.example.academic_risk_warning.common.Result;
import com.example.academic_risk_warning.security.LoginUser;
import com.example.academic_risk_warning.security.RequireRole;
import com.example.academic_risk_warning.security.UserContext;
import com.example.academic_risk_warning.service.stats.StatsService;
import com.example.academic_risk_warning.vo.stats.StatsEnvelope;
import com.example.academic_risk_warning.vo.stats.StatsQuery;
import com.example.academic_risk_warning.vo.stats.StatsVos;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

/**
 * 统计模块统一接口（P1-4）：/api/stats 七接口。
 *
 * <p>权限：仅辅导员与教务；管理员看全校，辅导员按 counselor_class 只看本人行政班（见 StatsScopeResolver）。
 * 教师端按 PLAN 不开放（v3.1）。</p>
 *
 * <p>返回统一为 Result&lt;StatsEnvelope&lt;T&gt;&gt;；P1-4 阶段物化表未落地，
 * 历史/干预类指标以 dataStatus=PARTIAL/NO_DATA 显式降级，不用 0 冒充真实值。</p>
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
@RequireRole({ "COUNSELOR", "ADMIN" })
public class StatsController {

    private final StatsService statsService;

    /** 总览：当前风险面 + 周期内新增/关闭/干预。 */
    @GetMapping("/overview")
    public Result<StatsEnvelope<StatsVos.Overview>> overview(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LoginUser user = UserContext.require();
        return Result.ok(statsService.overview(user, query(termId, period, startDate, endDate)));
    }

    /** 趋势：按自然周的事件序列（P1-4 仅预警/干预事件口径）。 */
    @GetMapping("/trend")
    public Result<StatsEnvelope<StatsVos.Trend>> trend(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LoginUser user = UserContext.require();
        return Result.ok(statsService.trend(user, query(termId, period, startDate, endDate)));
    }

    /** 分布：当前风险等级 / 课程 / 班级分布。 */
    @GetMapping("/distribution")
    public Result<StatsEnvelope<StatsVos.Distribution>> distribution(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LoginUser user = UserContext.require();
        return Result.ok(statsService.distribution(user, query(termId, period, startDate, endDate)));
    }

    /** 对比：教学班/课程当前快照对比。 */
    @GetMapping("/compare")
    public Result<StatsEnvelope<StatsVos.Compare>> compare(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LoginUser user = UserContext.require();
        return Result.ok(statsService.compare(user, query(termId, period, startDate, endDate)));
    }

    /** 漏斗：周期内新增预警的处置漏斗。 */
    @GetMapping("/funnel")
    public Result<StatsEnvelope<StatsVos.Funnel>> funnel(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LoginUser user = UserContext.require();
        return Result.ok(statsService.funnel(user, query(termId, period, startDate, endDate)));
    }

    /** 成效：周期内干预的复评成效（P1-4 先用 effective 字段口径）。 */
    @GetMapping("/effect")
    public Result<StatsEnvelope<StatsVos.Effect>> effect(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LoginUser user = UserContext.require();
        return Result.ok(statsService.effect(user, query(termId, period, startDate, endDate)));
    }

    /** 导出 CSV：type = overview（默认）/ distribution / compare，数字与对应接口一致。 */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false, defaultValue = "overview") String type,
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false, defaultValue = "week") String period,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LoginUser user = UserContext.require();
        String csv = statsService.exportCsv(user, query(termId, period, startDate, endDate), type);
        byte[] body = ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
        String fileName = "stats_" + (type == null ? "overview" : type) + ".csv";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                .body(body);
    }

    private StatsQuery query(Long termId, String period, String startDate, String endDate) {
        StatsQuery q = new StatsQuery();
        q.setTermId(termId);
        q.setPeriod(period);
        q.setStartDate(startDate);
        q.setEndDate(endDate);
        return q;
    }
}
