package log.monitor.api.dto.victorialogs;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VictoriaLogsStatsResponse {
    private String status;
    private VictoriaLogsStatsData data;
}
