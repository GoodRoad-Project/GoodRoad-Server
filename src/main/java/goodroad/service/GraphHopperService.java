package goodroad.service;

import goodroad.api.ApiErrors.ApiException;
import goodroad.config.CacheConfig;
import goodroad.model.gh.GraphHopperResponse;
import goodroad.validation.GeoUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GraphHopperService {
    private final String apiKey;
    private final String baseUrl;
    private final WebClient webClient;

    public GraphHopperService(
            @Value("${graphhopper.api.key}") String apiKey,
            @Value("${graphhopper.api.url:https://graphhopper.com/api/1}") String baseUrl
    ) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("graphhopper.api.key must be configured");
        }
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    @Cacheable(cacheNames = CacheConfig.GRAPH_HOPPER_ROUTES, keyGenerator = "graphHopperRouteKeyGenerator", unless = "#result == null")
    public GraphHopperResponse getRoute(
            String start,
            String end,
            String profile,
            Boolean pointsEncoded,
            String locale,
            Map<String, Object> customModel
    ) {
        if (profile == null) profile = "foot";
        if (pointsEncoded == null) pointsEncoded = true;
        if (locale == null) locale = "ru";

        GeoUtils.Coordinates startPoint = GeoUtils.parseLatLon(start, "start");
        GeoUtils.Coordinates endPoint = GeoUtils.parseLatLon(end, "end");

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("points", List.of(
                    List.of(startPoint.longitude(), startPoint.latitude()),
                    List.of(endPoint.longitude(), endPoint.latitude())
            ));
            requestBody.put("profile", profile);
            requestBody.put("points_encoded", pointsEncoded);

            if (customModel != null && !customModel.isEmpty()) {
                requestBody.put("custom_model", customModel);
            }

            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = baseUrl + "/route?key=" + apiKey + "&locale=" + locale;
            ResponseEntity<GraphHopperResponse> response = rest.exchange(
                    url, HttpMethod.POST, entity, GraphHopperResponse.class
            );

            GraphHopperResponse body = response.getBody();
            if (body == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "ROUTING_PROVIDER_EMPTY_RESPONSE", "Routing provider returned an empty response");
            }
            return body;

        } catch (ApiException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ROUTING_PROVIDER_ERROR", "Routing provider rejected the request");
        } catch (RuntimeException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "ROUTING_PROVIDER_UNAVAILABLE", "Routing provider is unavailable");
        }
    }

    public GraphHopperResponse getRoute(String start, String end) {
        return getRoute(start, end, "foot", true, "ru", null);
    }

    public GraphHopperResponse getRoute(String start, String end, Map<String, Object> customModel) {
        return getRoute(start, end, "foot", true, "ru", customModel);
    }
}
