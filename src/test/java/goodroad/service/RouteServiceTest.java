package goodroad.service;

import goodroad.model.RouteRequest;
import goodroad.model.RouteResponse;
import goodroad.model.gh.GraphHopperResponse;
import goodroad.model.gh.Path;
import goodroad.obstacle.ObstacleDBService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

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

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(graphHopperService, times(3)).getRoute(anyString(), anyString(), anyString(), anyBoolean(), anyString(), captor.capture());
        assertNull(captor.getAllValues().get(0));
    }
}
