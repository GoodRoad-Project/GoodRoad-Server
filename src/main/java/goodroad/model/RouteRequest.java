package goodroad.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;
import java.util.ArrayList;

@Data
public class RouteRequest {
    private String start;
    private String end;

    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("max_stairs")
    private Integer maxStairsCount;

    @JsonProperty("max_slope")
    private Double maxSlopeAngle;

    @JsonProperty("max_curb_height")
    private Integer maxCurbHeight;

    @JsonProperty("min_path_width")
    private Integer minPathWidth;

    @JsonProperty("avoid_stairs")
    private boolean avoidStairs = false;

    @JsonProperty("need_ramp")
    private boolean needRamp = false;

    @JsonProperty("avoid_bad_road")
    private boolean avoidBadRoad = false;

    @JsonProperty("avoid_surfaces")
    private List<String> avoidSurfaceTypes = new ArrayList<>();

    @JsonProperty("obstacle_policies")
    private List<RouteObstaclePolicy> obstaclePolicies = new ArrayList<>();

    private String locale = "ru";

    @JsonProperty("alternatives")
    private boolean needAlternatives = true;

    @JsonProperty("points_encoded")
    private boolean pointsEncoded = true;

    @Data
    public static class RouteObstaclePolicy {
        @JsonProperty("obstacle_type")
        private String obstacleType;

        @JsonProperty("max_allowed_severity")
        private Short maxAllowedSeverity;
    }
}