package goodroad.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import goodroad.model.RouteRequest;
import goodroad.model.RouteResponse;
import goodroad.service.RouteService;
import goodroad.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RouteController.class)
@AutoConfigureMockMvc(addFilters = false)
class RouteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RouteService routeService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    @WithMockUser
    void buildRoute_shouldReturn200_IfRequestIsValid() throws Exception {
        RouteRequest request = new RouteRequest();
        request.setStart("59.932480,30.262920");
        request.setEnd("59.928767,30.264197");

        RouteResponse response = new RouteResponse();
        response.setId(UUID.randomUUID().toString());
        response.setPaths(List.of());

        when(routeService.buildThreeRoutes(any(RouteRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    void buildRoute_shouldReturn400_IfRequestIsInvalid() throws Exception {
        String invalidRequest = "{}";

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser
    void buildRoute_shouldReturn500_IfServiceThrowsException() throws Exception {
        RouteRequest request = new RouteRequest();
        request.setStart("59.932480,30.262920");
        request.setEnd("59.928767,30.264197");

        when(routeService.buildThreeRoutes(any(RouteRequest.class)))
                .thenThrow(new RuntimeException("Service error"));

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void health_shouldReturn200() throws Exception {
        mockMvc.perform(get("/routes/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.service").value("route-service"));
    }
}