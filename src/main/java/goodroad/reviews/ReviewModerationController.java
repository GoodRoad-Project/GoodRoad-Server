package goodroad.reviews;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews/moderation")
@RequiredArgsConstructor
public class ReviewModerationController {
    private final ReviewModerationService service;

    public record RejectReq(String reason) {
    }

    @GetMapping("/pending")
    public ReviewModerationService.ReviewsPageResp listPending(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return service.listPending(authentication.getName(), page, size);
    }

    @PostMapping("/{id}/take")
    public ReviewModerationService.ReviewView takeInWork(
            Authentication authentication,
            @PathVariable("id") String reviewId
    ) {
        return service.takeInWork(authentication.getName(), reviewId);
    }

    @PostMapping("/{id}/approve")
    public void approve(
            Authentication authentication,
            @PathVariable("id") String reviewId
    ) {
        service.approve(authentication.getName(), reviewId);
    }

    @PostMapping("/{id}/reject")
    public void reject(
            Authentication authentication,
            @PathVariable("id") String reviewId,
            @RequestBody RejectReq req
    ) {
        service.reject(authentication.getName(), reviewId, req == null ? null : req.reason());
    }

    @PostMapping("/{id}/release")
    public void release(
            Authentication authentication,
            @PathVariable("id") String reviewId
    ) {
        service.release(authentication.getName(), reviewId);
    }
}