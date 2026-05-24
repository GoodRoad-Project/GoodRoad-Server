package goodroad.volunteer;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/volunteer/moderation")
@RequiredArgsConstructor
public class VolunteerModerationController {
    private final VolunteerService service;

    @GetMapping("/applications/pending")
    public List<VolunteerService.VolunteerApplicationResp> listPendingApplications() {
        return service.listPendingApplications();
    }

    @PostMapping("/applications/{id}/approve")
    public VolunteerService.VolunteerApplicationResp approveApplication(Authentication authentication, @PathVariable String id) {
        return service.approveApplication(authentication.getName(), id);
    }

    @PostMapping("/applications/{id}/reject")
    public VolunteerService.VolunteerApplicationResp rejectApplication(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody VolunteerService.RejectApplicationReq req
    ) {
        return service.rejectApplication(authentication.getName(), id, req);
    }

    @GetMapping("/complaints/pending")
    public List<VolunteerService.ComplaintResp> listPendingComplaints() {
        return service.listPendingComplaints();
    }

    @PostMapping("/complaints/{id}/resolve")
    public VolunteerService.ComplaintResp resolveComplaint(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody VolunteerService.ResolveComplaintReq req
    ) {
        return service.resolveComplaint(authentication.getName(), id, req);
    }

    @GetMapping("/sos")
    public List<VolunteerService.SosResp> listSosNotifications() {
        return service.listSosNotifications();
    }

    @GetMapping("/sos/all")
    public List<VolunteerService.SosResp> listAllSosNotifications() {
        return service.listAllSosNotifications();
    }

    @PostMapping("/sos/{id}/confirm")
    public VolunteerService.SosResp confirmSos(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody(required = false) VolunteerService.ResolveSosReq req
    ) {
        return service.confirmSos(authentication.getName(), id, req);
    }

    @PostMapping("/sos/{id}/false-alarm")
    public VolunteerService.SosResp markSosFalseAlarm(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody(required = false) VolunteerService.ResolveSosReq req
    ) {
        return service.markSosFalseAlarm(authentication.getName(), id, req);
    }

    @PostMapping("/sos/{id}/resolve")
    public VolunteerService.SosResp resolveSos(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody(required = false) VolunteerService.ResolveSosReq req
    ) {
        return service.resolveSos(authentication.getName(), id, req);
    }
}

