package goodroad.service;

import goodroad.model.PlaceInfoResponse;
import goodroad.obstacle.ObstacleDBService;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlaceService {

    private final ObstacleDBService obstacleDBService;

    public PlaceService(ObstacleDBService obstacleDBService) {
        this.obstacleDBService = obstacleDBService;
    }

    public PlaceInfoResponse getPlaceInfo(double lat, double lon) {
        List<ObstacleDBService.ObstacleMapItemResp> obstacles = obstacleDBService.listInBox(lat - 0.0001, lat + 0.0001, lon - 0.0001, lon + 0.0001);
        ObstacleDBService.ObstacleMapItemResp place = obstacles.stream()
                .filter(o -> o.address() != null && o.address().placeName() != null && !o.address().placeName().isEmpty())
                .findFirst()
                .orElse(null);

        if (place == null) return null;

        ObstacleDBService.ObstacleCardResp card = obstacleDBService.getCard(place.id());

        double avgSeverity = card.reviews().stream()
                .flatMap(review -> review.obstacles().stream())
                .mapToInt(ObstacleDBService.ObstacleItemResp::severity)
                .average()
                .orElse(0.0);

        PlaceInfoResponse response = new PlaceInfoResponse();
        response.setPlaceName(place.address().placeName());
        response.setAddress(place.address().street() + " " + (place.address().house() != null ? place.address().house() : ""));
        response.setAverageSeverity(avgSeverity);
        response.setReviews(card.reviews());
        response.setLatitude(lat);
        response.setLongitude(lon);
        return response;
    }
}