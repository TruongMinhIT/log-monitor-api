package log.monitor.api.form.notificationGroup;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import log.monitor.api.form.StringToLongDeserializer;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@Schema
public class UpdateNotificationGroupForm extends CreateNotificationGroupForm {
    @NotNull(message = "id cannot be null")
    @JsonDeserialize(using = StringToLongDeserializer.class)
    @Schema(name = "id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;
}
