package goodroad.volunteer;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/volunteer")
@RequiredArgsConstructor
public class VolunteerController {
    private final VolunteerService service;

    @GetMapping("/menu")
    public VolunteerService.VolunteerMenuResp getMenu(Authentication authentication) {
        return service.getMenu(authentication.getName());
    }

    @PostMapping("/applications")
    public VolunteerService.VolunteerApplicationResp createApplication(
            Authentication authentication,
            @RequestBody VolunteerService.CreateVolunteerApplicationReq req
    ) {
        return service.createApplication(authentication.getName(), req);
    }

    @PostMapping(value = "/applications/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public VolunteerService.PhotoUploadResp uploadCertificate(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return service.uploadCertificate(authentication.getName(), file);
    }

    @GetMapping("/requests/own")
    public List<VolunteerService.HelpRequestResp> listOwnRequests(Authentication authentication) {
        return service.listOwnRequests(authentication.getName());
    }

    @PostMapping("/requests")
    public VolunteerService.HelpRequestResp createHelpRequest(
            Authentication authentication,
            @RequestBody VolunteerService.HelpRequestReq req
    ) {
        return service.createHelpRequest(authentication.getName(), req);
    }

    @GetMapping("/requests/available")
    public List<VolunteerService.HelpRequestResp> listAvailableRequests(
            Authentication authentication,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude
    ) {
        return service.listAvailableRequests(authentication.getName(), latitude, longitude);
    }

    @GetMapping("/requests/my-wards")
    public List<VolunteerService.HelpRequestResp> listMyWards(Authentication authentication) {
        return service.listMyWards(authentication.getName());
    }

    @GetMapping("/requests/{id}")
    public VolunteerService.HelpRequestResp getHelpRequest(Authentication authentication, @PathVariable String id) {
        return service.getHelpRequest(authentication.getName(), id);
    }

    @PostMapping("/requests/{id}/accept")
    public VolunteerService.HelpRequestResp acceptRequest(Authentication authentication, @PathVariable String id) {
        return service.acceptRequest(authentication.getName(), id);
    }

    @PostMapping("/requests/{id}/withdraw")
    public VolunteerService.HelpRequestResp withdrawResponse(Authentication authentication, @PathVariable String id) {
        return service.withdrawResponse(authentication.getName(), id);
    }

    @PostMapping("/requests/{id}/cancel")
    public VolunteerService.HelpRequestResp cancelOwnRequest(Authentication authentication, @PathVariable String id) {
        return service.cancelOwnRequest(authentication.getName(), id);
    }

    @DeleteMapping("/requests/{id}")
    public void deleteOwnRequest(Authentication authentication, @PathVariable String id) {
        service.deleteOwnRequest(authentication.getName(), id);
    }

    @PostMapping("/requests/{id}/route")
    public VolunteerService.HelpRequestResp setWalkRoute(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody VolunteerService.WalkRouteReq req
    ) {
        return service.setWalkRoute(authentication.getName(), id, req);
    }

    @PostMapping("/requests/{id}/start")
    public VolunteerService.HelpRequestResp startWalk(
            Authentication authentication,
            @PathVariable String id,
            @RequestBody(required = false) VolunteerService.WalkRouteReq req
    ) {
        return service.startWalk(authentication.getName(), id, req);
    }

    @PostMapping("/requests/{id}/finish")
    public VolunteerService.HelpRequestResp finishWalk(Authentication authentication, @PathVariable String id) {
        return service.finishWalk(authentication.getName(), id);
    }
}
