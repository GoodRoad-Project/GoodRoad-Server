package goodroad.controller;

import goodroad.model.PlaceInfoResponse;
import goodroad.service.PlaceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/places")
public class PlaceController {

    private final PlaceService placeService;

    public PlaceController(PlaceService placeService) {
        this.placeService = placeService;
    }

    @GetMapping("/info")
    public ResponseEntity<PlaceInfoResponse> getPlaceInfo(@RequestParam double lat, @RequestParam double lon) {
        PlaceInfoResponse response = placeService.getPlaceInfo(lat, lon);
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }
}