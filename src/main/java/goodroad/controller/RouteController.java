package goodroad.controller;

import goodroad.model.RouteRequest;
import goodroad.model.RouteResponse;
import goodroad.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/routes")
public class RouteController {

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    public ResponseEntity<RouteResponse> buildRoute(@Valid @RequestBody RouteRequest request) {
        return ResponseEntity.ok(routeService.buildThreeRoutes(request));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(
                Map.of(
                        "status", "OK",
                        "service", "route-service"
                )
        );
    }

}