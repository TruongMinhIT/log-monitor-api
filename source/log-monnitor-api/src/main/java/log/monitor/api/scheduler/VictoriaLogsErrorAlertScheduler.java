package log.monitor.api.scheduler;

import log.monitor.api.service.SlackAlertService;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    @Value("${victorialogs.query.dedupe-alerts:true}")
    private boolean dedupeAlerts;

    private final Set<String> currentlyAlerting = ConcurrentHashMap.newKeySet();

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

        Set<String> breachingApps = ConcurrentHashMap.newKeySet();
        for (VictoriaLogsStatsResult result : results) {
            breachingApps.add(resolveAppName(result));
        }

        // apps that recovered (were alerting, no longer over threshold) can alert again next time
        currentlyAlerting.retainAll(breachingApps);

        List<VictoriaLogsStatsResult> toAlert = new ArrayList<>();
        for (VictoriaLogsStatsResult result : results) {
            String app = resolveAppName(result);
            if (!dedupeAlerts || currentlyAlerting.add(app)) {
                toAlert.add(result);
            }
        }

        if (toAlert.isEmpty()) {
            log.debug("No new app crossed the error threshold ({}) in the last {} ({} already alerting)",
                    errorThreshold, window, currentlyAlerting.size());
            return;
        }

        List<String> lines = new ArrayList<>();
        for (VictoriaLogsStatsResult result : toAlert) {
            lines.add(String.format(":red_circle: *%s* — %s lỗi trong %s", resolveAppName(result), result.getErrorCount(), window));
        }
        Collections.sort(lines);

        String title = String.format(" %d app vượt ngưỡng %d lỗi / %s", toAlert.size(), errorThreshold, window);
        slackAlertService.sendMessage(title, lines);
    }

    private String resolveAppName(VictoriaLogsStatsResult result) {
        return result.getMetric() == null ? "unknown" : result.getMetric().getOrDefault(appField, "unknown");
    }

    private List<VictoriaLogsStatsResult> queryErrorCountsByApp(String query) {
        VictoriaLogsStatsResponse response = feignVictoriaLogsService.statsQuery(query);
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
