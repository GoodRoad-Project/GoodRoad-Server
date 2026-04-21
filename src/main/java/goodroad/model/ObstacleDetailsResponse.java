package goodroad.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ObstacleDetailsResponse {
    @JsonProperty("step_count")
    private Integer stepCount;

    @JsonProperty("height_cm")
    private Integer heightCm;

    @JsonProperty("angle_degrees")
    private Double angleDegrees;

    @JsonProperty("has_ramp")
    private Boolean hasRamp;

    @JsonProperty("surface_type")
    private String surfaceType;
}
