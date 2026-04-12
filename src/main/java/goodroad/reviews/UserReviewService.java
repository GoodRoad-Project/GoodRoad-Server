package goodroad.reviews;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.ObstacleType;
import goodroad.obstacle.repository.ObstacleFeatureEntity;
import goodroad.obstacle.repository.ObstacleFeatureRepo;
import goodroad.security.Crypto;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;
import goodroad.reviews.repository.*;

@Service
public class UserReviewService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";

    private final UserRepo users;
    private final ObstacleFeatureRepo features;
    private final ObstacleReviewRepo reviews;
    private final ObstacleReviewPhotoRepo photos;
    private final ObstacleReviewObstacleRepo reviewObstacles;
    private final ReviewSupportService reviewSupport;

    public UserReviewService(
            UserRepo users,
            ObstacleFeatureRepo features,
            ObstacleReviewRepo reviews,
            ObstacleReviewPhotoRepo photos,
            ObstacleReviewObstacleRepo reviewObstacles,
            ReviewSupportService reviewSupport
    ) {
        this.users = users;
        this.features = features;
        this.reviews = reviews;
        this.photos = photos;
        this.reviewObstacles = reviewObstacles;
        this.reviewSupport = reviewSupport;
    }

    public record AddressReq(
            String country,
            String region,
            String localityType,
            String city,
            String street,
            String house,
            String placeName
    ) {
    }

    public record ObstacleSeverityItem(
            String obstacleType,
            short severity
    ) {
    }

    public record UpsertReviewReq(
            double latitude,
            double longitude,
            AddressReq address,
            short rating,
            List<ObstacleSeverityItem> obstacles,
            String comment,
            List<String> photoUrls
    ) {
    }

    public record ReviewCardResp(
            String id,
            String featureId,
            AddressReq address,
            double latitude,
            double longitude,
            short rating,
            List<ObstacleSeverityItem> obstacles,
            String comment,
            List<String> photoUrls,
            String status,
            Instant createdAt,
            int awardedPoints,
            String moderatorComment
    ) {
    }

    public record ReviewPointsResp(int totalPoints, long approvedReviews) {
    }

    @Transactional(readOnly = true)
    public List<ReviewCardResp> listOwnReviews(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        List<ObstacleReviewEntity> mine = reviews.findByAuthorIdOrderByCreatedAtDesc(user.getId());
        return buildCards(mine);
    }

    @Transactional(readOnly = true)
    public ReviewPointsResp getOwnReviewPoints(String phoneFromAuth) {
        UserEntity user = findCurrent(phoneFromAuth);
        int total = normalizePoints(user.getTotalPoints());
        long approved = reviews.countByAuthorIdAndStatus(user.getId(), STATUS_APPROVED);
        return new ReviewPointsResp(total, approved);
    }

    @Transactional
    public ReviewCardResp createReview(String phoneFromAuth, UpsertReviewReq req) {
        UserEntity user = findCurrent(phoneFromAuth);
        ValidatedReviewInput input = validate(req);
        ObstacleFeatureEntity feature = resolveOrCreateFeature(input);

        if (reviews.findByFeatureIdAndAuthorId(feature.getId(), user.getId()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "Review already exists");
        }

        ObstacleReviewEntity review = ObstacleReviewEntity.builder()
                .featureId(feature.getId())
                .authorId(user.getId())
                .severity(input.rating())
                .text(input.comment())
                .createdAt(Instant.now())
                .status(STATUS_PENDING)
                .awardedPoints(0)
                .build();
        reviews.save(review);

        saveReviewObstacles(review.getId(), input.obstacles());
        savePhotos(review.getId(), input.photoUrls());

        user.setLastActiveAt(Instant.now());
        users.save(user);

        return buildCards(List.of(review)).get(0);
    }

    @Transactional
    public ReviewCardResp updateOwnReview(String phoneFromAuth, String reviewId, UpsertReviewReq req) {
        UserEntity user = findCurrent(phoneFromAuth);
        ObstacleReviewEntity review = findMineReview(user, reviewId);

        String oldStatus = review.getStatus();
        Long oldFeatureId = review.getFeatureId();

        ValidatedReviewInput input = validate(req);
        ObstacleFeatureEntity feature = resolveOrCreateFeature(input);

        if (!feature.getId().equals(oldFeatureId)) {
            reviews.findByFeatureIdAndAuthorId(feature.getId(), user.getId())
                    .ifPresent(other -> {
                        throw new ApiException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "Review already exists");
                    });
        }

        int oldAwardedPoints = normalizePoints(review.getAwardedPoints());

        review.setFeatureId(feature.getId());
        review.setSeverity(input.rating());
        review.setText(input.comment());
        review.setStatus(STATUS_PENDING);
        review.setAwardedPoints(0);
        review.setTakenByModeratorId(null);
        review.setTakenAt(null);
        review.setModeratedBy(null);
        review.setModeratedAt(null);
        review.setModeratorComment(null);
        reviews.save(review);

        reviewObstacles.deleteByIdReviewId(review.getId());
        photos.deleteByReviewId(review.getId());
        saveReviewObstacles(review.getId(), input.obstacles());
        savePhotos(review.getId(), input.photoUrls());

        if (STATUS_APPROVED.equals(oldStatus)) {
            if (oldAwardedPoints > 0) {
                user.setTotalPoints(safeSubtractPoints(user.getTotalPoints(), oldAwardedPoints));
            }
            reviewSupport.recomputeFeatureAggregate(oldFeatureId);
            if (!oldFeatureId.equals(feature.getId())) {
                reviewSupport.recomputeFeatureAggregate(feature.getId());
            }
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);

        return buildCards(List.of(review)).get(0);
    }

    @Transactional
    public void deleteOwnReview(String phoneFromAuth, String reviewId) {
        UserEntity user = findCurrent(phoneFromAuth);
        ObstacleReviewEntity review = findMineReview(user, reviewId);

        String oldStatus = review.getStatus();
        Long featureId = review.getFeatureId();
        int oldAwardedPoints = normalizePoints(review.getAwardedPoints());
        reviews.delete(review);

        if (STATUS_APPROVED.equals(oldStatus)) {
            if (oldAwardedPoints > 0) {
                user.setTotalPoints(safeSubtractPoints(user.getTotalPoints(), oldAwardedPoints));
            }
            reviewSupport.recomputeFeatureAggregate(featureId);
        }

        user.setLastActiveAt(Instant.now());
        users.save(user);
    }

    private List<ReviewCardResp> buildCards(List<ObstacleReviewEntity> rawReviews) {
        ReviewSupportService.ReviewBundle bundle = reviewSupport.loadBundle(rawReviews);

        Map<Long, ObstacleFeatureEntity> featureById = bundle.featureById();
        Map<Long, List<String>> photosByReview = bundle.photosByReview();
        Map<Long, List<ObstacleSeverityItem>> obstaclesByReview = buildObstaclesByReview(bundle);

        List<ReviewCardResp> out = new ArrayList<>();
        for (ObstacleReviewEntity review : rawReviews) {
            ObstacleFeatureEntity feature = featureById.get(review.getFeatureId());
            if (feature == null) {
                continue;
            }

            List<String> itemPhotos = photosByReview.getOrDefault(review.getId(), List.of());
            List<ObstacleSeverityItem> itemObstacles = obstaclesByReview.getOrDefault(review.getId(), List.of());
            int awardedPoints = STATUS_APPROVED.equals(review.getStatus())
                    ? normalizePoints(review.getAwardedPoints())
                    : 0;

            out.add(new ReviewCardResp(
                    review.getId().toString(),
                    feature.getId().toString(),
                    new AddressReq(
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
                    itemObstacles,
                    review.getText(),
                    itemPhotos,
                    review.getStatus(),
                    review.getCreatedAt(),
                    awardedPoints,
                    review.getModeratorComment()
            ));
        }
        return out;
    }

    private Map<Long, List<ObstacleSeverityItem>> buildObstaclesByReview(ReviewSupportService.ReviewBundle bundle) {
        Map<Long, List<ObstacleSeverityItem>> obstaclesByReview = new LinkedHashMap<>();
        for (Map.Entry<Long, List<ReviewSupportService.ReviewObstacleItem>> entry : bundle.obstaclesByReview().entrySet()) {
            List<ObstacleSeverityItem> items = new ArrayList<>();
            for (ReviewSupportService.ReviewObstacleItem item : entry.getValue()) {
                items.add(new ObstacleSeverityItem(
                        item.obstacleType(),
                        item.severity()
                ));
            }
            obstaclesByReview.put(entry.getKey(), items);
        }
        return obstaclesByReview;
    }

    private void saveReviewObstacles(Long reviewId, List<ObstacleSeverityItem> obstacles) {
        for (ObstacleSeverityItem item : obstacles) {
            ObstacleReviewObstacleEntity entity = ObstacleReviewObstacleEntity.builder()
                    .id(new ObstacleReviewObstacleKey(reviewId, item.obstacleType()))
                    .severity(item.severity())
                    .build();
            reviewObstacles.save(entity);
        }
    }

    private void savePhotos(Long reviewId, List<String> photoUrls) {
        for (String url : photoUrls) {
            ObstacleReviewPhotoEntity photo = ObstacleReviewPhotoEntity.builder()
                    .reviewId(reviewId)
                    .url(url)
                    .createdAt(Instant.now())
                    .build();
            photos.save(photo);
        }
    }

    private ObstacleFeatureEntity resolveOrCreateFeature(ValidatedReviewInput input) {
        ObstacleFeatureEntity feature = features.findByAddressAndType(
                input.primaryObstacleType(),
                input.address().country(),
                input.address().region(),
                input.address().localityType(),
                input.address().city(),
                input.address().street(),
                input.address().house(),
                input.address().placeName()
        ).orElse(null);

        if (feature != null) {
            return feature;
        }

        return createFeature(input);
    }

    private ObstacleFeatureEntity createFeature(ValidatedReviewInput input) {
        ObstacleFeatureEntity created = ObstacleFeatureEntity.builder()
                .type(input.primaryObstacleType())
                .lat(input.latitude())
                .lon(input.longitude())
                .country(input.address().country())
                .region(input.address().region())
                .localityType(input.address().localityType())
                .city(input.address().city())
                .street(input.address().street())
                .house(input.address().house())
                .placeName(input.address().placeName())
                .reviewsCount(0)
                .severityEst(null)
                .lastReviewedAt(null)
                .build();
        features.save(created);
        return created;
    }

    private ValidatedReviewInput validate(UpsertReviewReq req) {
        if (req == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_INVALID", "Review request body is empty");
        }
        if (req.rating() < 1 || req.rating() > 5) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_RATING_INVALID", "Rating must be in range from 1 to 5");
        }
        if (Double.isNaN(req.latitude()) || Double.isNaN(req.longitude())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_COORDS_INVALID", "Coordinates are invalid");
        }

        AddressReq address = validateAddress(req.address());
        List<ObstacleSeverityItem> obstacles = normalizeObstacles(req.obstacles());
        List<String> photoUrls = normalizePhotoUrls(req.photoUrls());
        String comment = blankToNull(req.comment());
        String primaryObstacleType = choosePrimaryObstacleType(obstacles);

        return new ValidatedReviewInput(
                req.latitude(),
                req.longitude(),
                address,
                req.rating(),
                obstacles,
                primaryObstacleType,
                comment,
                photoUrls
        );
    }

    private AddressReq validateAddress(AddressReq raw) {
        if (raw == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADDRESS_INVALID", "Address is invalid");
        }
        String country = requireAddressValue(raw.country());
        String region = requireAddressValue(raw.region());
        String localityType = requireAddressValue(raw.localityType());
        String city = requireAddressValue(raw.city());
        String street = requireAddressValue(raw.street());
        String house = requireAddressValue(raw.house());
        String placeName = blankToNull(raw.placeName());
        return new AddressReq(country, region, localityType, city, street, house, placeName);
    }

    private List<ObstacleSeverityItem> normalizeObstacles(List<ObstacleSeverityItem> rawItems) {
        Map<String, Short> normalized = new LinkedHashMap<>();
        for (String type : ObstacleType.allNames()) {
            normalized.put(type, (short) 0);
        }

        if (rawItems != null) {
            for (ObstacleSeverityItem rawItem : rawItems) {
                if (rawItem == null) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "OBSTACLE_EMPTY", "Obstacle is empty");
                }

                String type = ObstacleType.normalize(rawItem.obstacleType());
                short severity = rawItem.severity();
                if (severity < 0 || severity > 3) {
                    throw new ApiException(HttpStatus.BAD_REQUEST, "OBSTACLE_SEVERITY_INVALID", "Obstacle severity is invalid");
                }

                normalized.put(type, severity);
            }
        }

        boolean hasPositive = false;
        List<ObstacleSeverityItem> out = new ArrayList<>();
        for (String type : ObstacleType.allNames()) {
            short severity = normalized.get(type);
            if (severity > 0) {
                hasPositive = true;
            }
            out.add(new ObstacleSeverityItem(type, severity));
        }

        if (!hasPositive) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OBSTACLE_LIST_EMPTY", "At least one obstacle must have positive severity");
        }

        return out;
    }

    private List<String> normalizePhotoUrls(Collection<String> rawUrls) {
        List<String> out = new ArrayList<>();
        if (rawUrls == null) {
            return out;
        }
        for (String raw : rawUrls) {
            String value = blankToNull(raw);
            if (value != null) {
                out.add(value);
            }
        }
        return out;
    }

    private String choosePrimaryObstacleType(List<ObstacleSeverityItem> obstacles) {
        String bestType = null;
        short bestSeverity = -1;

        for (ObstacleSeverityItem item : obstacles) {
            if (item.severity() > bestSeverity) {
                bestSeverity = item.severity();
                bestType = item.obstacleType();
            }
        }

        if (bestType == null || bestSeverity == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "OBSTACLE_LIST_EMPTY", "At least one obstacle must have positive severity");
        }

        return bestType;
    }

    private UserEntity findCurrent(String phoneFromAuth) {
        return users.findByPhoneHash(currentPhoneHash(phoneFromAuth))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found"));
    }

    private String currentPhoneHash(String phoneFromAuth) {
        String phoneNorm = Crypto.normPhone(phoneFromAuth);
        if (phoneNorm.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "USER_PHONE_NOT_FOUND", "User with given phone not found");
        }
        return Crypto.sha256Hex(phoneNorm);
    }

    private ObstacleReviewEntity findMineReview(UserEntity user, String reviewId) {
        Long id = parseId(reviewId);
        return reviews.findByIdAndAuthorId(id, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "REVIEW_ID_NOT_FOUND", "Review with given id not found"));
    }

    private int normalizePoints(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private int safeSubtractPoints(Integer currentValue, int delta) {
        int current = normalizePoints(currentValue);
        int safeDelta = Math.max(delta, 0);
        int next = current - safeDelta;
        return Math.max(next, 0);
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID_INVALID", "Id is invalid");
        }
    }

    private String requireAddressValue(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADDRESS_INVALID", "Address value is invalid");
        }
        return normalized;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String s = value.trim();
        return s.isEmpty() ? null : s;
    }

    private record ValidatedReviewInput(
            double latitude,
            double longitude,
            AddressReq address,
            short rating,
            List<ObstacleSeverityItem> obstacles,
            String primaryObstacleType,
            String comment,
            List<String> photoUrls
    ) {
    }
}