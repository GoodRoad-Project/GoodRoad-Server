package goodroad.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PathResponse {
    private Double distance;
    private Long time;

    @JsonProperty("points_encoded")
    private Boolean pointsEncoded = true;

    private String points;
    private List<ObstacleResponse> obstacles;

    @JsonProperty("route_type")
    private String routeType = "fast";
}