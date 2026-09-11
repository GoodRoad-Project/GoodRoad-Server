package goodroad.controller;

import goodroad.model.RouteRequest;
import goodroad.model.RouteResponse;
import goodroad.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/routes")
public class RouteController {

    private static final Logger log = LoggerFactory.getLogger(RouteController.class);

    private final RouteService routeService;

    public RouteController(RouteService routeService) {
        this.routeService = routeService;
    }

    @PostMapping
    public ResponseEntity<RouteResponse> buildRoute(@Valid @RequestBody RouteRequest request) {

        long startTime = System.nanoTime();

        log.info("Received route request: start={}, end={}, alternatives={}",
                request.getStart(), request.getEnd(), request.isNeedAlternatives());

        try {
            RouteResponse response = routeService.buildThreeRoutes(request);
            log.info("Route built successfully");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid request: " + e.getMessage()
            );
        } catch (Exception e) {
            log.error("Failed to build route", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to build route: " + e.getMessage(),
                    e
            );
        } finally {
            long elapsedMs = (System.nanoTime() - startTime) / 1_000_000;
            log.info("POST /routes completed in {} ms", elapsedMs);
        }
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