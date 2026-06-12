package goodroad.service;

import goodroad.model.gh.GraphHopperResponse;
import goodroad.model.gh.Path;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class GraphHopperServiceTest {

    private MockWebServer mockWebServer;
    private GraphHopperService graphHopperService;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("").toString();
        baseUrl = baseUrl.substring(0, baseUrl.length() - 1);

        graphHopperService = new GraphHopperService(
                "test-api-key",
                baseUrl
        );
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnValidResponse_IfRequestIsCorrect() throws Exception {
        String fakeResponse = """
            {
                "paths": [
                    {
                        "distance": 1250.0,
                        "time": 600000,
                        "points": "encoded_points_string",
                        "points_encoded": true
                    }
                ],
                "info": {"took": 0.5}
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(fakeResponse)
                .setHeader("Content-Type", "application/json"));

        GraphHopperResponse response = graphHopperService.getRoute(
                "59.932480,30.262920",
                "59.928767,30.264197",
                "foot",
                true,
                "ru",
                null
        );

        assertNotNull(response);
        assertNotNull(response.getPaths());
        assertEquals(1, response.getPaths().size());

        Path path = response.getPaths().get(0);
        assertEquals(1250.0, path.getDistance(), 0.01);
        assertEquals(600000, path.getTime());
        assertEquals("encoded_points_string", path.getPoints());
        assertTrue(path.getPointsEncoded());

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        assertEquals("POST", recordedRequest.getMethod());
        assertTrue(recordedRequest.getPath().contains("key=test-api-key"));
    }

    @Test
    void shouldReturnNull_IfServerReturnsError() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        GraphHopperResponse response = graphHopperService.getRoute(
                "59.932480,30.262920",
                "59.928767,30.264197",
                "foot",
                true,
                "ru",
                null
        );

        assertNull(response);
    }

    @Test
    void shouldHandleNullProfileAndPointsEncoded() throws Exception {
        String fakeResponse = """
            {
                "paths": [
                    {
                        "distance": 1000.0,
                        "time": 500000,
                        "points": "test_points",
                        "points_encoded": true
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(fakeResponse)
                .setHeader("Content-Type", "application/json"));

        GraphHopperResponse response = graphHopperService.getRoute(
                "59.932480,30.262920",
                "59.928767,30.264197",
                null,
                null,
                null,
                null
        );

        assertNotNull(response);
        assertEquals(1000.0, response.getPaths().get(0).getDistance(), 0.01);

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        String body = recordedRequest.getBody().readUtf8();
        assertTrue(body.contains("\"profile\":\"foot\""));
        assertTrue(body.contains("\"points_encoded\":true"));
    }

    @Test
    void shouldParseCustomModelCorrectly() throws Exception {
        String fakeResponse = """
            {
                "paths": [
                    {
                        "distance": 800.0,
                        "time": 400000,
                        "points": "custom_points",
                        "points_encoded": true
                    }
                ]
            }
            """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(fakeResponse)
                .setHeader("Content-Type", "application/json"));

        Map<String, Object> customModel = Map.of(
                "priority", List.of(
                        Map.of("if", "road_class == STEPS", "multiply_by", "0")
                )
        );

        GraphHopperResponse response = graphHopperService.getRoute(
                "59.932480,30.262920",
                "59.928767,30.264197",
                "foot",
                true,
                "ru",
                customModel
        );

        assertNotNull(response);

        RecordedRequest recordedRequest = mockWebServer.takeRequest(1, TimeUnit.SECONDS);
        assertNotNull(recordedRequest);
        String body = recordedRequest.getBody().readUtf8();
        assertTrue(body.contains("custom_model"));
        assertTrue(body.contains("road_class == STEPS"));
    }
}