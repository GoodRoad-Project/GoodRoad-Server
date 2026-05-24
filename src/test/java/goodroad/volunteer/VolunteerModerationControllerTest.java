package goodroad.volunteer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
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
class VolunteerModerationControllerTest {

    @Mock
    private VolunteerService service;

    @Test
    void shouldModerateVolunteerApplications() throws Exception {
        MockMvc mvc = standaloneSetup(new VolunteerModerationController(service)).build();
        Principal moderator = principal("+79990000151");
        VolunteerService.VolunteerApplicationResp pending = application("10", "PENDING", null);
        VolunteerService.VolunteerApplicationResp approved = application("10", "APPROVED", null);
        VolunteerService.VolunteerApplicationResp rejected = application("10", "REJECTED", "Не хватает сертификатов");

        when(service.listPendingApplications()).thenReturn(List.of(pending));
        when(service.approveApplication("+79990000151", "10")).thenReturn(approved);
        when(service.rejectApplication(eq("+79990000151"), eq("10"), any(VolunteerService.RejectApplicationReq.class)))
                .thenReturn(rejected);

        mvc.perform(get("/volunteer/moderation/applications/pending").principal(moderator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("10"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mvc.perform(post("/volunteer/moderation/applications/10/approve").principal(moderator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(post("/volunteer/moderation/applications/10/reject")
                        .principal(moderator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Не хватает сертификатов"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"))
                .andExpect(jsonPath("$.moderatorComment").value("Не хватает сертификатов"));

        verify(service).rejectApplication(eq("+79990000151"), eq("10"), argThat(req ->
                "Не хватает сертификатов".equals(req.reason())
        ));
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
}
