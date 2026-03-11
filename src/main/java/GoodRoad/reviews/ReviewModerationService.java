package GoodRoad.reviews;

import GoodRoad.api.ApiErrors.ApiException;
import GoodRoad.model.Role;
import GoodRoad.obstacle.repository.ObstacleFeatureEntity;
import GoodRoad.reviews.repository.ObstacleReviewEntity;
import GoodRoad.reviews.repository.ObstacleReviewPhotoRepo;
import GoodRoad.reviews.repository.ObstacleReviewRepo;
import GoodRoad.security.Crypto;
import GoodRoad.users.repository.UserEntity;
import GoodRoad.users.repository.UserRepo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewModerationService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final ObstacleReviewRepo reviews;
    private final ObstacleReviewPhotoRepo photos;
    private final UserRepo users;
    private final ReviewSupportService reviewSupport;

    public ReviewModerationService(
            ObstacleReviewRepo reviews,
            ObstacleReviewPhotoRepo photos,
            UserRepo users,
            ReviewSupportService reviewSupport
    ) {
        this.reviews = reviews;
        this.photos = photos;
        this.users = users;
        this.reviewSupport = reviewSupport;
    }

    public record AddressResp(
            String country,
            String region,
            String localityType,
            String city,
            String street,
            String house,
            String placeName
    ) {
    }

    public record ObstacleItemResp(
            String obstacleType,
            short severity
    ) {
    }

    public record ReviewView(
            String id,
            String featureId,
            String authorId,
            AddressResp address,
            double latitude,
            double longitude,
            short rating,
            List<ObstacleItemResp> obstacles,
            String comment,
            List<String> photoUrls,
            String status,
            Instant createdAt,
            boolean takenInWork,
            boolean takenByMe,
            String takenByModeratorId,
            Instant takenAt,
            String moderatorComment
    ) {
    }

    public record ReviewsPageResp(
            List<ReviewView> items,
            int page,
            int size,
            long total
    ) {
    }

    @Transactional(readOnly = true)
    public ReviewsPageResp listPending(String phoneFromAuth, int page, int size) {
        UserEntity moderator = findCurrentModerator(phoneFromAuth);

        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 200));

        Page<ObstacleReviewEntity> p = reviews.findQueueForModerator(
                STATUS_PENDING,
                moderator.getId(),
                PageRequest.of(safePage, safeSize)
        );

        return new ReviewsPageResp(
                buildViews(p.getContent(), moderator.getId()),
                safePage,
                safeSize,
                p.getTotalElements()
        );
    }

    @Transactional
    public ReviewView takeInWork(String phoneFromAuth, String reviewId) {
        UserEntity moderator = findCurrentModerator(phoneFromAuth);
        ObstacleReviewEntity review = findPendingForUpdate(reviewId);

        Long takenBy = review.getTakenByModeratorId();
        if (takenBy != null && !takenBy.equals(moderator.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_TAKEN", "Review already taken by another moderator");
        }

        if (takenBy == null) {
            review.setTakenByModeratorId(moderator.getId());
            review.setTakenAt(Instant.now());
            reviews.save(review);
        }

        return buildViews(List.of(review), moderator.getId()).get(0);
    }

    @Transactional
    public void approve(String phoneFromAuth, String reviewId) {
        UserEntity moderator = findCurrentModerator(phoneFromAuth);
        ObstacleReviewEntity review = findTakenByCurrentModerator(reviewId, moderator.getId());

        int previousPoints = normalizePoints(review.getAwardedPoints());
        int currentPoints = calcPoints(review.getText(), photos.existsByReviewId(review.getId()));
        int creditedPoints = Math.max(previousPoints, currentPoints);
        int delta = Math.max(0, creditedPoints - previousPoints);

        if (delta > 0 && review.getAuthorId() != null) {
            UserEntity user = users.findById(review.getAuthorId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_USER", "No user"));
            user.setTotalPoints(safeAddPoints(user.getTotalPoints(), delta));
            users.save(user);
        }

        review.setStatus(STATUS_APPROVED);
        review.setAwardedPoints(creditedPoints);
        review.setTakenByModeratorId(null);
        review.setTakenAt(null);
        review.setModeratedBy(moderator.getId());
        review.setModeratedAt(Instant.now());
        review.setModeratorComment(null);
        reviews.save(review);

        reviewSupport.recomputeFeatureAggregate(review.getFeatureId());
    }

    @Transactional
    public void reject(String phoneFromAuth, String reviewId, String reason) {
        UserEntity moderator = findCurrentModerator(phoneFromAuth);
        ObstacleReviewEntity review = findTakenByCurrentModerator(reviewId, moderator.getId());

        String comment = blankToNull(reason);
        if (comment == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMPTY_REASON", "Reject reason is required");
        }

        review.setStatus(STATUS_REJECTED);
        review.setAwardedPoints(0);
        review.setTakenByModeratorId(null);
        review.setTakenAt(null);
        review.setModeratedBy(moderator.getId());
        review.setModeratedAt(Instant.now());
        review.setModeratorComment(comment);
        reviews.save(review);
    }

    @Transactional
    public void release(String phoneFromAuth, String reviewId) {
        UserEntity moderator = findCurrentModerator(phoneFromAuth);
        ObstacleReviewEntity review = findPendingForUpdate(reviewId);

        if (review.getTakenByModeratorId() == null) {
            return;
        }

        if (!review.getTakenByModeratorId().equals(moderator.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_YOURS", "Review is taken by another moderator");
        }

        review.setTakenByModeratorId(null);
        review.setTakenAt(null);
        reviews.save(review);
    }

    private List<ReviewView> buildViews(List<ObstacleReviewEntity> rawReviews, Long currentModeratorId) {
        ReviewSupportService.ReviewBundle bundle = reviewSupport.loadBundle(rawReviews);

        Map<Long, ObstacleFeatureEntity> featureById = bundle.featureById();
        Map<Long, List<String>> photosByReview = bundle.photosByReview();
        Map<Long, List<ObstacleItemResp>> obstaclesByReview = buildObstaclesByReview(bundle);

        List<ReviewView> out = new ArrayList<>();
        for (ObstacleReviewEntity review : rawReviews) {
            ObstacleFeatureEntity feature = featureById.get(review.getFeatureId());
            if (feature == null) {
                continue;
            }

            Long takenByModeratorId = review.getTakenByModeratorId();
            boolean takenInWork = takenByModeratorId != null;
            boolean takenByMe = takenByModeratorId != null && takenByModeratorId.equals(currentModeratorId);

            out.add(new ReviewView(
                    review.getId().toString(),
                    review.getFeatureId().toString(),
                    review.getAuthorId() == null ? null : review.getAuthorId().toString(),
                    new AddressResp(
                            feature.getCountry(),
                            feature.getRegion(),
                            feature.getLocalityType(),
                            feature.getCity(),
                            feature.getStreet(),
                            feature.getHouse(),
                            feature.getPlaceName()
                    ),
                    feature.getLat(),
                    feature.getLon(),
                    review.getSeverity(),
                    obstaclesByReview.getOrDefault(review.getId(), List.of()),
                    review.getText(),
                    photosByReview.getOrDefault(review.getId(), List.of()),
                    review.getStatus(),
                    review.getCreatedAt(),
                    takenInWork,
                    takenByMe,
                    takenByModeratorId == null ? null : takenByModeratorId.toString(),
                    review.getTakenAt(),
                    review.getModeratorComment()
            ));
        }

        return out;
    }

    private Map<Long, List<ObstacleItemResp>> buildObstaclesByReview(ReviewSupportService.ReviewBundle bundle) {
        Map<Long, List<ObstacleItemResp>> obstaclesByReview = new LinkedHashMap<>();
        for (Map.Entry<Long, List<ReviewSupportService.ReviewObstacleItem>> entry : bundle.obstaclesByReview().entrySet()) {
            List<ObstacleItemResp> items = new ArrayList<>();
            for (ReviewSupportService.ReviewObstacleItem item : entry.getValue()) {
                items.add(new ObstacleItemResp(
                        item.obstacleType(),
                        item.severity()
                ));
            }
            obstaclesByReview.put(entry.getKey(), items);
        }
        return obstaclesByReview;
    }

    private ObstacleReviewEntity findPendingForUpdate(String reviewId) {
        Long id = parseId(reviewId);

        ObstacleReviewEntity review = reviews.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NO_REVIEW", "No review"));

        if (!STATUS_PENDING.equals(review.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_PENDING", "Review is not pending");
        }

        return review;
    }

    private ObstacleReviewEntity findTakenByCurrentModerator(String reviewId, Long moderatorId) {
        ObstacleReviewEntity review = findPendingForUpdate(reviewId);

        if (review.getTakenByModeratorId() == null) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_TAKEN", "Review is not taken in work");
        }

        if (!review.getTakenByModeratorId().equals(moderatorId)) {
            throw new ApiException(HttpStatus.CONFLICT, "NOT_YOURS", "Review is taken by another moderator");
        }

        return review;
    }

    private UserEntity findCurrentModerator(String phoneFromAuth) {
        UserEntity user = users.findByPhoneHash(currentPhoneHash(phoneFromAuth))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "NO_USER", "No user"));

        String role = user.getRole();
        if (!Role.MODERATOR.name().equals(role) && !Role.MODERATOR_ADMIN.name().equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Forbidden");
        }

        return user;
    }

    private int calcPoints(String comment, boolean hasPhoto) {
        boolean hasComment = comment != null && !comment.isBlank();

        if (hasPhoto && hasComment) {
            return 20;
        }
        if (hasPhoto) {
            return 15;
        }
        if (hasComment) {
            return 10;
        }
        return 5;
    }

    private String currentPhoneHash(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "NO_USER", "No user");
        }
        return Crypto.sha256Hex(phoneNorm);
    }

    private int normalizePoints(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private int safeAddPoints(Integer currentValue, int delta) {
        int current = normalizePoints(currentValue);
        int safeDelta = Math.max(delta, 0);
        long next = (long) current + safeDelta;

        if (next > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (next < 0) {
            return 0;
        }
        return (int) next;
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BAD_ID", "Bad id");
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        return s.isEmpty() ? null : s;
    }
}