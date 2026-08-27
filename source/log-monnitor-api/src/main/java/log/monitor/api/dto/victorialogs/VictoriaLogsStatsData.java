package log.monitor.api.dto.victorialogs;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class VictoriaLogsStatsData {
    private String resultType;
    private List<VictoriaLogsStatsResult> result;
}
