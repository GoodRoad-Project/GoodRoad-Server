package GoodRoad.reviews;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class UserReviewController {
    private final UserReviewService service;

    @GetMapping("/own")
    public List<UserReviewService.ReviewCardResp> listOwnReviews(Authentication authentication) {
        return service.listOwnReviews(authentication.getName());
    }

    @GetMapping("points")
    public UserReviewService.ReviewPointsResp getOwnReviewPoints(Authentication authentication) {
        return service.getOwnReviewPoints(authentication.getName());
    }

    @PostMapping("")
    public UserReviewService.ReviewCardResp createReview(Authentication authentication, @RequestBody UserReviewService.UpsertReviewReq req) {
        return service.createReview(authentication.getName(), req);
    }

    @PatchMapping("/{id}")
    public UserReviewService.ReviewCardResp updateOwnReview(Authentication authentication, @PathVariable("id") String reviewId, @RequestBody UserReviewService.UpsertReviewReq req) {
        return service.updateOwnReview(authentication.getName(), reviewId, req);
    }

    @DeleteMapping("/{id}")
    public void deleteOwnReview(Authentication authentication, @PathVariable("id") String reviewId) {
        service.deleteOwnReview(authentication.getName(), reviewId);
    }

}
