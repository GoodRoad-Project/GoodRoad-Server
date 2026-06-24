package goodroad.model;

import goodroad.obstacle.ObstacleDBService.ReviewResp;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceInfoResponse {
    private String placeName;
    private String address;
    private Double averageSeverity;
    private List<ReviewResp> reviews;
}