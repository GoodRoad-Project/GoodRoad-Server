package goodroad.reviews;

import goodroad.api.ApiErrors.ApiException;
import goodroad.obstacle.repository.ObstacleFeatureEntity;
import goodroad.security.Crypto;
import goodroad.storage.StorageService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import goodroad.reviews.repository.ObstacleReviewEntity;
import goodroad.reviews.repository.ObstacleReviewRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@Service
public class UserReviewService {

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";

    private static final long MAX_PHOTO_SIZE = 5 * 1024 * 1024;

    private final UserRepo users;
    private final ObstacleReviewRepo reviews;
    private final ReviewSupportService reviewSupport;
    private final StorageService storageService;
    private final ReviewValidationService validator;
    private final ReviewFeatureService featureService;
    private final ReviewMapper mapper;

    public UserReviewService(
            UserRepo users,
            ObstacleReviewRepo reviews,
            ReviewSupportService reviewSupport,
            StorageService storageService,
            ReviewValidationService validator,
            ReviewFeatureService featureService,
            ReviewMapper mapper
    ) {
        this.users = users;
        this.reviews = reviews;
        this.reviewSupport = reviewSupport;
        this.storageService = storageService;
        this.validator = validator;
        this.featureService = featureService;
        this.mapper = mapper;
    }

    public record AddressReq(
            String country,
            String region,
            String localityType,
            String city,
            String street,
            String house,
            String placeName
    ) {}

    public record ObstacleSeverityItem(
            String obstacleType,
            short severity
    ) {}

    public record UpsertReviewReq(
            double latitude,
            double longitude,
            AddressReq address,
            short rating,
            List<ObstacleSeverityItem> obstacles,
            String comment,
            List<String> photoUrls
    ) {}

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
    ) {}

    public record ReviewPointsResp(
            int totalPoints,
            long approvedReviews
    ) {}

    public record ReviewPhotoUploadResp(
            String photoUrl
    ) {}

    @Transactional(readOnly = true)
    public List<ReviewCardResp> listOwnReviews(
            String phoneFromAuth
    ) {

        UserEntity user = findCurrent(phoneFromAuth);

        return mapper.buildCards(
                reviews.findByAuthorIdOrderByCreatedAtDesc(
                        user.getId()
                )
        );
    }

    @Transactional(readOnly = true)
    public ReviewPointsResp getOwnReviewPoints(
            String phoneFromAuth
    ) {

        UserEntity user = findCurrent(phoneFromAuth);

        return new ReviewPointsResp(
                normalizePoints(user.getTotalPoints()),
                reviews.countByAuthorIdAndStatus(
                        user.getId(),
                        STATUS_APPROVED
                )
        );
    }

    @Transactional
    public ReviewCardResp createReview(
            String phoneFromAuth,
            UpsertReviewReq req
    ) {

        UserEntity user = findCurrent(phoneFromAuth);

        ReviewValidationService.ValidatedReviewInput input =
                validator.validate(req);

        ObstacleFeatureEntity feature =
                featureService.resolveOrCreateFeature(
                        input
                );

        if (reviews.findByFeatureIdAndAuthorId(
                feature.getId(),
                user.getId()
        ).isPresent()) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "REVIEW_ALREADY_EXISTS",
                    "Review already exists"
            );
        }

        ObstacleReviewEntity review =
                ObstacleReviewEntity.builder()
                        .featureId(feature.getId())
                        .authorId(user.getId())
                        .severity(input.rating())
                        .text(input.comment())
                        .createdAt(Instant.now())
                        .status(STATUS_PENDING)
                        .awardedPoints(0)
                        .build();

        reviews.save(review);

        mapper.saveReviewObstacles(
                review.getId(),
                input.obstacles()
        );

        mapper.savePhotos(
                review.getId(),
                input.photoUrls()
        );

        user.setLastActiveAt(
                Instant.now()
        );

        users.save(user);

        return mapper.buildCards(
                List.of(review)
        ).get(0);
    }

    @Transactional
    public ReviewCardResp updateOwnReview(
            String phoneFromAuth,
            String reviewId,
            UpsertReviewReq req
    ) {

        UserEntity user = findCurrent(phoneFromAuth);

        ObstacleReviewEntity review =
                reviews.findByIdAndAuthorId(
                        parseId(reviewId),
                        user.getId()
                ).orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "REVIEW_ID_NOT_FOUND",
                                "Review with given id not found"
                        )
                );

        String oldStatus = review.getStatus();
        Long oldFeatureId = review.getFeatureId();

        ReviewValidationService.ValidatedReviewInput input =
                validator.validate(req);

        ObstacleFeatureEntity feature =
                featureService.resolveOrCreateFeature(input);

        review.setFeatureId(feature.getId());
        review.setSeverity(input.rating());
        review.setText(input.comment());
        review.setStatus(STATUS_PENDING);
        review.setAwardedPoints(0);
        review.setModeratorComment(null);

        reviews.save(review);

        mapper.saveReviewObstacles(review.getId(), input.obstacles());
        mapper.savePhotos(review.getId(), input.photoUrls());

        user.setLastActiveAt(Instant.now());
        users.save(user);

        return mapper.buildCards(List.of(review)).get(0);
    }

    @Transactional
    public void deleteOwnReview(
            String phoneFromAuth,
            String reviewId
    ) {

        UserEntity user = findCurrent(phoneFromAuth);

        ObstacleReviewEntity review =
                reviews.findByIdAndAuthorId(
                        parseId(reviewId),
                        user.getId()
                ).orElseThrow(() ->
                        new ApiException(
                                HttpStatus.NOT_FOUND,
                                "REVIEW_ID_NOT_FOUND",
                                "Review with given id not found"
                        )
                );

        reviews.delete(review);

        user.setLastActiveAt(Instant.now());
        users.save(user);
    }

    @Transactional
    public ReviewPhotoUploadResp uploadReviewPhoto(
            String phoneFromAuth,
            MultipartFile file
    ) {

        if (file == null || file.isEmpty()) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PHOTO_EMPTY",
                    "Photo file is empty"
            );
        }

        if (file.getSize() > MAX_PHOTO_SIZE) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "PHOTO_TOO_LARGE",
                    "Photo file is too large"
            );
        }

        UserEntity user =
                findCurrent(phoneFromAuth);

        String photoUrl =
                storageService.uploadReviewPhoto(
                        file,
                        user.getId().toString()
                );

        return new ReviewPhotoUploadResp(
                photoUrl
        );
    }

    private UserEntity findCurrent(
            String phoneFromAuth
    ) {

        return users.findByPhoneHash(
                currentPhoneHash(
                        phoneFromAuth
                )
        ).orElseThrow(() ->
                new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "USER_PHONE_NOT_FOUND",
                        "User with given phone not found"
                )
        );
    }

    private String currentPhoneHash(
            String phoneFromAuth
    ) {

        String phoneNorm =
                Crypto.normPhone(
                        phoneFromAuth
                );

        if (phoneNorm.isEmpty()) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "USER_PHONE_NOT_FOUND",
                    "User with given phone not found"
            );
        }

        return Crypto.sha256Hex(
                phoneNorm
        );
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ID_INVALID",
                    "Id is invalid"
            );
        }
    }

    private int normalizePoints(
            Integer value
    ) {

        return value == null || value < 0
                ? 0
                : value;
    }

}