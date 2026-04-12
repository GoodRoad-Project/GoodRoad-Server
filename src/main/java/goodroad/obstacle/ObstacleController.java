package goodroad.obstacle;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/obstacles")
@RequiredArgsConstructor
public class ObstacleController {

    private final ObstacleDBService service;

    @GetMapping("")
    public List<ObstacleDBService.ObstacleMapItemResp> listInBox(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLon,
            @RequestParam double maxLon
    ) {
        return service.listInBox(minLat, maxLat, minLon, maxLon);
    }

    @GetMapping("/{id}")
    public ObstacleDBService.ObstacleCardResp getCard(@PathVariable("id") String featureId) {
        return service.getCard(featureId);
    }
}