package goodroad.service;

import goodroad.model.RouteRequest;
import goodroad.model.RouteResponse;
import goodroad.model.gh.GraphHopperResponse;
import goodroad.model.gh.Path;
import goodroad.obstacle.ObstacleDBService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private GraphHopperService graphHopperService;

    @Mock
    private ObstacleDBService obstacleDBService;

    @InjectMocks
    private RouteService service;

    @Captor
    private ArgumentCaptor<Map<String, Object>> customModelCaptor;

    private static final String START = "59.9300,30.3300";
    private static final String END = "59.9400,30.3400";

    private ObstacleDBService.ObstacleMapItemResp createObstacle(
            String id, String type, double lat, double lon, Map<String, Short> severities) {
        return new ObstacleDBService.ObstacleMapItemResp(
                id, type, lat, lon,
                new ObstacleDBService.AddressResp("Россия", "Санкт-Петербург", "город",
                        "Санкт-Петербург", "Садовая", "12", null),
                (short) 4,
                severities,
                1,
                Instant.now()
        );
    }

    private RouteRequest.RouteObstaclePolicy createPolicy(String type, short maxSeverity) {
        RouteRequest.RouteObstaclePolicy policy = new RouteRequest.RouteObstaclePolicy();
        policy.setObstacleType(type);
        policy.setMaxAllowedSeverity(maxSeverity);
        return policy;
    }

    @Test
    void shouldBuildThreeRoutes() {
        RouteRequest request = new RouteRequest();
        request.setStart("59.9300,30.3300");
        request.setEnd("59.9400,30.3400");
        RouteRequest.RouteObstaclePolicy policy = new RouteRequest.RouteObstaclePolicy();
        policy.setObstacleType("STAIRS");
        policy.setMaxAllowedSeverity((short) 1);
        request.setObstaclePolicies(List.of(policy));

        ObstacleDBService.ObstacleMapItemResp obstacle = new ObstacleDBService.ObstacleMapItemResp(
                "1", "STAIRS", 59.93, 30.33,
                new ObstacleDBService.AddressResp("Россия", "Санкт-Петербург", "город", "Санкт-Петербург", "Садовая", "12", null),
                (short) 4,
                Map.of("STAIRS", (short) 3),
                1,
                java.time.Instant.now()
        );

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(obstacle));
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        RouteResponse result = service.buildThreeRoutes(request);

        assertEquals(3, result.getPaths().size());
        assertEquals("fast", result.getPaths().get(0).getRouteType());
        assertEquals("balanced", result.getPaths().get(1).getRouteType());
        assertEquals("safe", result.getPaths().get(2).getRouteType());
        verify(graphHopperService, times(3)).getRoute(anyString(), anyString(), eq("foot"), eq(true), eq("ru"), any());
    }

    @Test
    void shouldCallFastRouteWithoutCustomModel() {
        RouteRequest request = new RouteRequest();
        request.setStart("59.9300,30.3300");
        request.setEnd("59.9400,30.3400");
        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        verify(graphHopperService, times(3)).getRoute(
                anyString(),
                anyString(),
                anyString(),
                anyBoolean(),
                anyString(),
                customModelCaptor.capture()
        );
        assertNull(customModelCaptor.getAllValues().get(0));
    }


    @Test
    void shouldReturnEmptyPathsWhenGraphHopperReturnsNull() {
        RouteRequest request = new RouteRequest();
        request.setStart("59.9300,30.3300");
        request.setEnd("59.9400,30.3400");

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(null);

        RouteResponse result = service.buildThreeRoutes(request);

        assertNotNull(result);
        assertTrue(result.getPaths().isEmpty());
    }

    @Test
    void shouldHandleMissingStartOrEnd() {
        RouteRequest request = new RouteRequest();
        request.setStart("59.9300,30.3300");

        assertThrows(NullPointerException.class, () -> {
            service.buildThreeRoutes(request);
        });
    }

    @Test
    void shouldPassCustomModelToGraphHopper() {
        RouteRequest request = new RouteRequest();
        request.setStart("59.9300,30.3300");
        request.setEnd("59.9400,30.3400");

        RouteRequest.RouteObstaclePolicy policy = new RouteRequest.RouteObstaclePolicy();
        policy.setObstacleType("STAIRS");
        policy.setMaxAllowedSeverity((short) 2);
        request.setObstaclePolicies(List.of(policy));

        ObstacleDBService.ObstacleMapItemResp obstacle = new ObstacleDBService.ObstacleMapItemResp(
                "1", "STAIRS", 59.93, 30.33,
                new ObstacleDBService.AddressResp("Россия", "Санкт-Петербург", "город", "Санкт-Петербург", "Садовая", "12", null),
                (short) 4,
                Map.of("STAIRS", (short) 3),
                1,
                java.time.Instant.now()
        );

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(obstacle));
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(graphHopperService, times(3)).getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), captor.capture());

        List<Map<String, Object>> allModels = captor.getAllValues();

        assertNull(allModels.get(0));

        assertNotNull(allModels.get(1));
        assertNotNull(allModels.get(1).get("priority"));

        assertNotNull(allModels.get(2));
        assertNotNull(allModels.get(2).get("priority"));
    }

    @Test
    void shouldNotAddObstacle_IfSeverityIsLessThanMaxAllowed() {
        RouteRequest request = new RouteRequest();
        request.setStart("59.9300,30.3300");
        request.setEnd("59.9400,30.3400");

        RouteRequest.RouteObstaclePolicy policy = new RouteRequest.RouteObstaclePolicy();
        policy.setObstacleType("STAIRS");
        policy.setMaxAllowedSeverity((short) 3);
        request.setObstaclePolicies(List.of(policy));

        ObstacleDBService.ObstacleMapItemResp obstacle = new ObstacleDBService.ObstacleMapItemResp(
                "1", "STAIRS", 59.93, 30.33,
                new ObstacleDBService.AddressResp("Россия", "Санкт-Петербург", "город", "Санкт-Петербург", "Садовая", "12", null),
                (short) 4,
                Map.of("STAIRS", (short) 2),
                1,
                java.time.Instant.now()
        );

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(obstacle));
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(graphHopperService, times(3)).getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), captor.capture());

        assertNull(captor.getAllValues().get(0));

        Map<String, Object> balancedModel = captor.getAllValues().get(1);
        if (balancedModel != null) {
            assertNull(balancedModel.get("priority"));
        }
    }

    @Test
    void shouldHandleAllObstacleTypesInBalancedModel() {
        RouteRequest request = new RouteRequest();
        request.setStart(START);
        request.setEnd(END);
        request.setObstaclePolicies(List.of(
                createPolicy("STAIRS", (short) 1),
                createPolicy("POTHOLES", (short) 1),
                createPolicy("ROAD_SLOPE", (short) 1),
                createPolicy("SAND", (short) 1),
                createPolicy("GRAVEL", (short) 1),
                createPolicy("CURB", (short) 1)
        ));

        List<ObstacleDBService.ObstacleMapItemResp> obstacles = List.of(
                createObstacle("1", "STAIRS", 59.93, 30.33, Map.of("STAIRS", (short) 3)),
                createObstacle("2", "POTHOLES", 59.935, 30.335, Map.of("POTHOLES", (short) 3)),
                createObstacle("3", "ROAD_SLOPE", 59.94, 30.34, Map.of("ROAD_SLOPE", (short) 3)),
                createObstacle("4", "SAND", 59.93, 30.33, Map.of("SAND", (short) 3)),
                createObstacle("5", "GRAVEL", 59.93, 30.33, Map.of("GRAVEL", (short) 3)),
                createObstacle("6", "CURB", 59.93, 30.33, Map.of("CURB", (short) 3))
        );

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(obstacles);
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        verify(graphHopperService, times(3)).getRoute(
                anyString(), anyString(), anyString(), anyBoolean(), anyString(), customModelCaptor.capture()
        );

        Map<String, Object> balancedModel = customModelCaptor.getAllValues().get(1);
        assertNotNull(balancedModel);
        assertNotNull(balancedModel.get("priority"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> conditions = (List<Map<String, Object>>) balancedModel.get("priority");
        assertEquals(6, conditions.size());
    }

    @Test
    void shouldNotAddObstacleToBalancedModel_IfNoMatchingPolicy() {
        RouteRequest request = new RouteRequest();
        request.setStart(START);
        request.setEnd(END);
        request.setObstaclePolicies(List.of(
                createPolicy("STAIRS", (short) 1)
        ));

        ObstacleDBService.ObstacleMapItemResp obstacle = createObstacle(
                "1", "POTHOLES", 59.93, 30.33, Map.of("POTHOLES", (short) 3)
        );

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(obstacle));
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        verify(graphHopperService, times(3)).getRoute(
                anyString(), anyString(), anyString(), anyBoolean(), anyString(), customModelCaptor.capture()
        );

        Map<String, Object> balancedModel = customModelCaptor.getAllValues().get(1);
        assertNull(balancedModel);
    }

    @Test
    void shouldHandleEmptyObstaclePolicies() {
        RouteRequest request = new RouteRequest();
        request.setStart(START);
        request.setEnd(END);
        request.setObstaclePolicies(List.of());

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        verify(graphHopperService, times(3)).getRoute(
                anyString(), anyString(), anyString(), anyBoolean(), anyString(), customModelCaptor.capture()
        );

        assertNull(customModelCaptor.getAllValues().get(0));
        assertNull(customModelCaptor.getAllValues().get(1));
        assertNull(customModelCaptor.getAllValues().get(2));
    }

    @Test
    void shouldHandleNullObstaclePolicies() {
        RouteRequest request = new RouteRequest();
        request.setStart(START);
        request.setEnd(END);
        request.setObstaclePolicies(null);

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of());
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        verify(graphHopperService, times(3)).getRoute(
                anyString(), anyString(), anyString(), anyBoolean(), anyString(), customModelCaptor.capture()
        );

        assertNull(customModelCaptor.getAllValues().get(0));
        assertNull(customModelCaptor.getAllValues().get(1));
        assertNull(customModelCaptor.getAllValues().get(2));
    }

    @Test
    void shouldHandleObstacleWithNullSeverityEstimates() {
        RouteRequest request = new RouteRequest();
        request.setStart(START);
        request.setEnd(END);
        request.setObstaclePolicies(List.of(
                createPolicy("STAIRS", (short) 1)
        ));

        ObstacleDBService.ObstacleMapItemResp obstacle = new ObstacleDBService.ObstacleMapItemResp(
                "1", "STAIRS", 59.93, 30.33,
                new ObstacleDBService.AddressResp("Россия", "Санкт-Петербург", "город",
                        "Санкт-Петербург", "Садовая", "12", null),
                (short) 4,
                null,
                1,
                Instant.now()
        );

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(obstacle));
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        verify(graphHopperService, times(3)).getRoute(
                anyString(), anyString(), anyString(), anyBoolean(), anyString(), customModelCaptor.capture()
        );

        assertNull(customModelCaptor.getAllValues().get(1));
    }

    @Test
    void shouldHandleObstacleWithEmptySeverityForType() {
        RouteRequest request = new RouteRequest();
        request.setStart(START);
        request.setEnd(END);
        request.setObstaclePolicies(List.of(
                createPolicy("STAIRS", (short) 1)
        ));

        ObstacleDBService.ObstacleMapItemResp obstacle = createObstacle(
                "1", "STAIRS", 59.93, 30.33, Map.of()
        );

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(obstacle));
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        verify(graphHopperService, times(3)).getRoute(
                anyString(), anyString(), anyString(), anyBoolean(), anyString(), customModelCaptor.capture()
        );

        assertNull(customModelCaptor.getAllValues().get(1));
    }

    @Test
    void shouldHandlePolicyWithNullFields() {
        RouteRequest request = new RouteRequest();
        request.setStart(START);
        request.setEnd(END);

        RouteRequest.RouteObstaclePolicy policy = new RouteRequest.RouteObstaclePolicy();
        policy.setObstacleType(null);
        policy.setMaxAllowedSeverity(null);
        request.setObstaclePolicies(List.of(policy));

        ObstacleDBService.ObstacleMapItemResp obstacle = createObstacle(
                "1", "STAIRS", 59.93, 30.33, Map.of("STAIRS", (short) 3)
        );

        when(obstacleDBService.listInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(List.of(obstacle));
        when(graphHopperService.getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), any()))
                .thenReturn(new GraphHopperResponse(List.of(new Path(100.0, 1000L, "abc", true)), null));

        service.buildThreeRoutes(request);

        verify(graphHopperService, times(3)).getRoute(
                anyString(), anyString(), anyString(), anyBoolean(), anyString(), customModelCaptor.capture()
        );

        assertNull(customModelCaptor.getAllValues().get(1));
    }
}

