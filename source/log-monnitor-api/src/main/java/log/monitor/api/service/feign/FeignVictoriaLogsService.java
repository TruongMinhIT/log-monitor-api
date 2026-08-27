package log.monitor.api.service.feign;

import log.monitor.api.config.CustomFeignConfig;
import log.monitor.api.dto.victorialogs.VictoriaLogsStatsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "victoria-logs-svr", url = "${victorialogs.api.url}", configuration = CustomFeignConfig.class)
public interface FeignVictoriaLogsService {
    @GetMapping(value = "/select/logsql/stats_query")
    VictoriaLogsStatsResponse statsQuery(@RequestParam("query") String query);
}
