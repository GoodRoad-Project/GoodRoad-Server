package goodroad.volunteer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
class VolunteerControllerTest {

    @Mock
    private VolunteerService service;

    @Test
    void shouldReturnVolunteerMenu() throws Exception {
        MockMvc mvc = standaloneSetup(new VolunteerController(service)).build();
        when(service.getMenu("+79990000001"))
                .thenReturn(new VolunteerService.VolunteerMenuResp(true, "APPROVED", null));

        mvc.perform(get("/volunteer/menu").principal(principal("+79990000001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volunteer").value(true))
                .andExpect(jsonPath("$.applicationStatus").value("APPROVED"));

        verify(service).getMenu("+79990000001");
    }

    @Test
    void shouldCreateVolunteerApplication() throws Exception {
        MockMvc mvc = standaloneSetup(new VolunteerController(service)).build();
        when(service.createApplication(eq("+79990000001"), any(VolunteerService.CreateVolunteerApplicationReq.class)))
                .thenReturn(application("10", "PENDING", null));

        mvc.perform(post("/volunteer/applications")
                        .principal(principal("+79990000001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dobroUrl": "https://dobro.ru/volunteer/1",
                                  "phone": "+79990000001",
                                  "socialNickname": "@volunteer",
                                  "certificatePhotoUrls": [
                                    "https://storage.yandexcloud.net/bucket/cert.jpg"
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("10"))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(service).createApplication(eq("+79990000001"), argThat(req ->
                "https://dobro.ru/volunteer/1".equals(req.dobroUrl())
                        && "+79990000001".equals(req.phone())
                        && "@volunteer".equals(req.socialNickname())
                        && req.certificatePhotoUrls().size() == 1
        ));
    }

    @Test
    void shouldUploadVolunteerCertificate() throws Exception {
        MockMvc mvc = standaloneSetup(new VolunteerController(service)).build();
        MockMultipartFile file = new MockMultipartFile(
                "file", "cert.png", "image/png", new byte[] {1, 2, 3}
        );
        when(service.uploadCertificate(eq("+79990000001"), any()))
                .thenReturn(new VolunteerService.PhotoUploadResp("https://storage/cert.png"));

        mvc.perform(multipart("/volunteer/applications/photos")
                        .file(file)
                        .principal(principal("+79990000001")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.photoUrl").value("https://storage/cert.png"));

        verify(service).uploadCertificate(eq("+79990000001"), any());
    }

    @Test
    void shouldUseHelpRequestEndpoints() throws Exception {
        MockMvc mvc = standaloneSetup(new VolunteerController(service)).build();
        Principal user = principal("+79990000001");
        VolunteerService.HelpRequestResp request = helpRequest("20", null, "OPEN", false, false, false);
        VolunteerService.HelpRequestResp accepted = helpRequest("20", "2", "ACCEPTED", true, false, false);

        when(service.createHelpRequest(eq("+79990000001"), any(VolunteerService.HelpRequestReq.class))).thenReturn(request);
        when(service.listOwnRequests("+79990000001")).thenReturn(List.of(request));
        when(service.getHelpRequest("+79990000001", "20")).thenReturn(request);
        when(service.cancelOwnRequest("+79990000001", "20")).thenReturn(request);

        mvc.perform(post("/volunteer/requests")
                        .principal(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(helpRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("20"))
                .andExpect(jsonPath("$.contactsVisible").value(false));

        mvc.perform(get("/volunteer/requests/own").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("20"));

        mvc.perform(get("/volunteer/requests/20").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));

        mvc.perform(post("/volunteer/requests/20/cancel").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("20"));

        mvc.perform(delete("/volunteer/requests/20").principal(user))
                .andExpect(status().isOk());

        verify(service).deleteOwnRequest("+79990000001", "20");
        verify(service).createHelpRequest(eq("+79990000001"), argThat(req ->
                "Садовая, 10".equals(req.fromAddress())
                        && "Невский, 20".equals(req.toAddress())
                        && "24-05-2026".equals(req.date())
                        && "13:30".equals(req.time())
        ));

        when(service.acceptRequest("+79990000001", "20")).thenReturn(accepted);
        when(service.withdrawResponse("+79990000001", "20")).thenReturn(request);
        when(service.listAvailableRequests("+79990000001", 59.93, 30.31)).thenReturn(List.of(request));
        when(service.listMyWards("+79990000001")).thenReturn(List.of(accepted));

        mvc.perform(get("/volunteer/requests/available")
                        .principal(user)
                        .param("latitude", "59.93")
                        .param("longitude", "30.31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("20"));

        mvc.perform(get("/volunteer/requests/my-wards").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].volunteerId").value("2"));

        mvc.perform(post("/volunteer/requests/20/accept").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactsVisible").value(true));

        mvc.perform(post("/volunteer/requests/20/withdraw").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldUseWalkEndpoints() throws Exception {
        MockMvc mvc = standaloneSetup(new VolunteerController(service)).build();
        Principal user = principal("+79990000001");
        VolunteerService.HelpRequestResp active = helpRequest("20", "2", "ACCEPTED", true, true, false);
        VolunteerService.HelpRequestResp completed = helpRequest("20", "2", "COMPLETED", true, true, true);

        when(service.setWalkRoute(eq("+79990000001"), eq("20"), any(VolunteerService.WalkRouteReq.class))).thenReturn(active);
        when(service.startWalk(eq("+79990000001"), eq("20"), any(VolunteerService.WalkRouteReq.class))).thenReturn(active);
        when(service.finishWalk("+79990000001", "20")).thenReturn(completed);

        mvc.perform(post("/volunteer/requests/20/route")
                        .principal(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routeJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.started").value(true));

        mvc.perform(post("/volunteer/requests/20/start")
                        .principal(user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(routeJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mvc.perform(post("/volunteer/requests/20/finish").principal(user))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    private Principal principal(String phone) {
        return new UsernamePasswordAuthenticationToken(phone, "password");
    }

    private VolunteerService.VolunteerApplicationResp application(String id, String status, String comment) {
        return new VolunteerService.VolunteerApplicationResp(
                id, "1", "Иван Петров", "https://dobro.ru/volunteer/1", "79990000001", "@volunteer",
                List.of("https://storage/cert.png"), status, comment,
                Instant.parse("2026-05-01T10:00:00Z"), null
        );
    }

    private VolunteerService.HelpRequestResp helpRequest(
            String id,
            String volunteerId,
            String status,
            boolean contactsVisible,
            boolean started,
            boolean completed
    ) {
        return new VolunteerService.HelpRequestResp(
                id, "1", volunteerId, "Садовая, 10", "Невский, 20", "24-05-2026", "13:30",
                contactsVisible ? "79990000001" : null,
                contactsVisible ? "@user" : null,
                "Нужно помочь дойти до метро", status, contactsVisible, true, started, completed,
                Instant.parse("2026-05-01T10:00:00Z")
        );
    }

    private String helpRequestJson() {
        return """
                {
                  "fromAddress": "Садовая, 10",
                  "toAddress": "Невский, 20",
                  "date": "24-05-2026",
                  "time": "13:30",
                  "phone": "+79990000001",
                  "socialNickname": "@user",
                  "comment": "Нужно помочь дойти до метро"
                }
                """;
    }

    private String routeJson() {
        return """
                {
                  "routePoints": [
                    {
                      "latitude": 59.93,
                      "longitude": 30.31
                    },
                    {
                      "latitude": 59.94,
                      "longitude": 30.32
                    }
                  ]
                }
                """;
    }
}
