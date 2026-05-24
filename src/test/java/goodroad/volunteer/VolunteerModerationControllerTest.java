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

    @Test
    void shouldModerateComplaints() throws Exception {
        MockMvc mvc = standaloneSetup(new VolunteerModerationController(service)).build();
        Principal moderator = principal("+79990000151");
        VolunteerService.ComplaintResp pending = complaint("40", "PENDING", null);
        VolunteerService.ComplaintResp resolved = complaint("40", "RESOLVED", "Виновный предупрежден");

        when(service.listPendingComplaints()).thenReturn(List.of(pending));
        when(service.resolveComplaint(eq("+79990000151"), eq("40"), any(VolunteerService.ResolveComplaintReq.class)))
                .thenReturn(resolved);

        mvc.perform(get("/volunteer/moderation/complaints/pending").principal(moderator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("40"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mvc.perform(post("/volunteer/moderation/complaints/40/resolve")
                        .principal(moderator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guiltyUserId": "2",
                                  "moderatorComment": "Виновный предупрежден"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"))
                .andExpect(jsonPath("$.moderatorComment").value("Виновный предупрежден"));

        verify(service).resolveComplaint(eq("+79990000151"), eq("40"), argThat(req ->
                "2".equals(req.guiltyUserId()) && "Виновный предупрежден".equals(req.moderatorComment())
        ));
    }

    @Test
    void shouldModerateSosNotifications() throws Exception {
        MockMvc mvc = standaloneSetup(new VolunteerModerationController(service)).build();
        Principal moderator = principal("+79990000151");
        VolunteerService.SosResp open = sos("30", "OPEN", null);
        VolunteerService.SosResp confirmed = sos("30", "CONFIRMED", "Связались с участниками");
        VolunteerService.SosResp falseAlarm = sos("31", "FALSE_ALARM", "Ошибочное нажатие");
        VolunteerService.SosResp resolved = sos("30", "RESOLVED", "Ситуация решена");

        when(service.listSosNotifications()).thenReturn(List.of(open, confirmed));
        when(service.listAllSosNotifications()).thenReturn(List.of(open, confirmed, falseAlarm, resolved));
        when(service.confirmSos(eq("+79990000151"), eq("30"), any(VolunteerService.ResolveSosReq.class)))
                .thenReturn(confirmed);
        when(service.markSosFalseAlarm(eq("+79990000151"), eq("31"), any(VolunteerService.ResolveSosReq.class)))
                .thenReturn(falseAlarm);
        when(service.resolveSos(eq("+79990000151"), eq("30"), any(VolunteerService.ResolveSosReq.class)))
                .thenReturn(resolved);

        mvc.perform(get("/volunteer/moderation/sos").principal(moderator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"))
                .andExpect(jsonPath("$[1].status").value("CONFIRMED"));

        mvc.perform(get("/volunteer/moderation/sos/all").principal(moderator))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[2].status").value("FALSE_ALARM"))
                .andExpect(jsonPath("$[3].status").value("RESOLVED"));

        mvc.perform(post("/volunteer/moderation/sos/30/confirm")
                        .principal(moderator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moderatorComment": "Связались с участниками"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mvc.perform(post("/volunteer/moderation/sos/31/false-alarm")
                        .principal(moderator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moderatorComment": "Ошибочное нажатие"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FALSE_ALARM"));

        mvc.perform(post("/volunteer/moderation/sos/30/resolve")
                        .principal(moderator)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "moderatorComment": "Ситуация решена"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));

        verify(service).confirmSos(eq("+79990000151"), eq("30"), argThat(req ->
                "Связались с участниками".equals(req.moderatorComment())
        ));
        verify(service).markSosFalseAlarm(eq("+79990000151"), eq("31"), argThat(req ->
                "Ошибочное нажатие".equals(req.moderatorComment())
        ));
        verify(service).resolveSos(eq("+79990000151"), eq("30"), argThat(req ->
                "Ситуация решена".equals(req.moderatorComment())
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

    private VolunteerService.ComplaintResp complaint(String id, String status, String comment) {
        return new VolunteerService.ComplaintResp(
                id, "20", "1", "2", "Волонтер не пришел", status, comment,
                Instant.parse("2026-05-01T10:00:00Z"), "RESOLVED".equals(status) ? Instant.parse("2026-05-01T11:00:00Z") : null
        );
    }

    private VolunteerService.SosResp sos(String id, String status, String comment) {
        return new VolunteerService.SosResp(
                id, "20", "MANUAL", "Нужна помощь", status, comment,
                "Иван Петров", "Анна Иванова", "79990000001", "79990000002", "@user", "@volunteer",
                Instant.parse("2026-05-01T10:00:00Z"),
                List.of("RESOLVED", "FALSE_ALARM").contains(status) ? Instant.parse("2026-05-01T11:00:00Z") : null
        );
    }
}
