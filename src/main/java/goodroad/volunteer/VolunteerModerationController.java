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
}
