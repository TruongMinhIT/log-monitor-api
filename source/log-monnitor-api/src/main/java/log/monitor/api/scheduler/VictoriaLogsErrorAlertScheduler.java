package log.monitor.api.scheduler;

import log.monitor.api.service.SlackAlertService;
import log.monitor.api.service.feign.FeignConst;
import log.monitor.api.service.feign.FeignVictoriaLogsService;
import log.monitor.api.dto.victorialogs.VictoriaLogsStatsResponse;
import log.monitor.api.dto.victorialogs.VictoriaLogsStatsResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class VictoriaLogsErrorAlertScheduler {

    @Autowired
    private FeignVictoriaLogsService feignVictoriaLogsService;

    @Autowired
    private SlackAlertService slackAlertService;

    @Value("${victorialogs.query.window}")
    private String window;

    @Value("${victorialogs.query.app-field}")
    private String appField;

    @Value("${victorialogs.query.error-field}")
    private String errorField;

    @Value("${victorialogs.query.error-value}")
    private String errorValue;

    @Value("${victorialogs.query.error-threshold}")
    private int errorThreshold;

    @Scheduled(cron = "${victorialogs.query.cron}")
    public void checkErrorRateAndAlert() {
        String query = buildQuery();
        List<VictoriaLogsStatsResult> results;
        try {
            results = queryErrorCountsByApp(query);
        } catch (Exception e) {
            log.error("Failed to query VictoriaLogs [{}]", query, e);
            return;
        }

        if (results.isEmpty()) {
            log.debug("No app crossed the error threshold ({}) in the last {}", errorThreshold, window);
            return;
        }

        List<String> lines = new ArrayList<>();
        for (VictoriaLogsStatsResult result : results) {
            lines.add(String.format(":red_circle: *%s* — %s lỗi trong %s", resolveAppName(result), result.getErrorCount(), window));
        }
        Collections.sort(lines);

        String title = String.format("🚨 %d app vượt ngưỡng %d lỗi / %s", results.size(), errorThreshold, window);
        slackAlertService.sendMessage(title, lines);
    }

    private String resolveAppName(VictoriaLogsStatsResult result) {
        return result.getMetric() == null ? "unknown" : result.getMetric().getOrDefault(appField, "unknown");
    }

    private List<VictoriaLogsStatsResult> queryErrorCountsByApp(String query) {
        VictoriaLogsStatsResponse response = feignVictoriaLogsService.statsQuery(FeignConst.LOGIN_TYPE_NO_AUTH, query);
        if (response == null || response.getData() == null || response.getData().getResult() == null) {
            return Collections.emptyList();
        }
        return response.getData().getResult();
    }

    private String buildQuery() {
        return String.format("_time:%s %s:%s | stats by (%s) count() as errors | filter errors:>=%d",
                window, errorField, errorValue, appField, errorThreshold);
    }
}
