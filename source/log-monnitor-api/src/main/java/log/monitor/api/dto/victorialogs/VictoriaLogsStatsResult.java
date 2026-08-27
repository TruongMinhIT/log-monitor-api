package log.monitor.api.dto.victorialogs;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class VictoriaLogsStatsResult {
    private Map<String, String> metric;
    private List<Object> value;

    public String getErrorCount() {
        if (value == null || value.size() < 2 || value.get(1) == null) {
            return "0";
        }
        return String.valueOf(value.get(1));
    }
}
