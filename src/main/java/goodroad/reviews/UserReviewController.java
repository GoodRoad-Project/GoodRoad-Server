package goodroad.reviews;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class UserReviewController {
    private final UserReviewService service;

    @GetMapping("/own")
    public List<UserReviewService.ReviewCardResp> listOwnReviews(Authentication authentication) {
        return service.listOwnReviews(authentication.getName());
    }

    @GetMapping("/points")
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

    @PostMapping(
            value = "/photos",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public UserReviewService.ReviewPhotoUploadResp uploadPhoto(
            Authentication authentication,
            @RequestParam("file") MultipartFile file
    ) {
        return service.uploadReviewPhoto(
                authentication.getName(),
                file
        );
    }

    @DeleteMapping("/{id}")
    public void deleteOwnReview(Authentication authentication, @PathVariable("id") String reviewId) {
        service.deleteOwnReview(authentication.getName(), reviewId);
    }

}
