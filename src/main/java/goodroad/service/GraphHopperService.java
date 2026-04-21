package goodroad.service;

import goodroad.model.gh.GraphHopperResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpMethod;

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
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    public GraphHopperResponse getRoute(
            String start,
            String end,
            String profile,
            Boolean pointsEncoded,
            String locale,
            Map<String, Object> customModel
    ) {
        System.out.println("=== GRAPHHOPPER SERVICE CALLED ===");
        System.out.println("start: '" + start + "'");
        System.out.println("end: '" + end + "'");
        System.out.println("profile: " + profile);
        System.out.println("pointsEncoded: " + pointsEncoded);
        System.out.println("locale: " + locale);
        System.out.println("customModel: " + customModel);

        // Значения по умолчанию
        if (profile == null) profile = "foot";
        if (pointsEncoded == null) pointsEncoded = true;
        if (locale == null) locale = "ru";

        try {
            String[] startPart = start.split(",");
            String[] endPart = end.split(",");

            Map<String, Object> requestBody = Map.of(
                    "points", List.of(
                            List.of(Double.parseDouble(startPart[1]), Double.parseDouble(startPart[0])),
                            List.of(Double.parseDouble(endPart[1]), Double.parseDouble(endPart[0]))
                    ),
                    "profile", "foot",
                    "points_encoded", true
            );

            RestTemplate rest = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            String url = baseUrl + "/route?key=" + apiKey;
            ResponseEntity<GraphHopperResponse> response = rest.exchange(
                    url, HttpMethod.POST, entity, GraphHopperResponse.class
            );

            return response.getBody();

        } catch (Exception e) {
            System.out.println("GraphHopper error: " + e.getMessage());
            return null;
        }
    }

    public GraphHopperResponse getRoute(String start, String end) {
        return getRoute(start, end, "foot", true, "ru", null);
    }

    public GraphHopperResponse getRoute(String start, String end, Map<String, Object> customModel) {
        return getRoute(start, end, "foot", true, "ru", customModel);
    }
}