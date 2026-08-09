package goodroad.reviews;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.ObstacleType;
import goodroad.validation.GeoUtils;
import goodroad.validation.InputRules;
import goodroad.validation.TrustedUrlService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReviewValidationService {
    private final TrustedUrlService trustedUrls;

    public ReviewValidationService(TrustedUrlService trustedUrls) {
        this.trustedUrls = trustedUrls;
    }

    public ValidatedReviewInput validate(
            UserReviewService.UpsertReviewReq req,
            Long userId
    ) {

        if (req == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REVIEW_INVALID",
                    "Review request body is empty"
            );
        }

        if (req.rating() < 1 || req.rating() > 5) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REVIEW_RATING_INVALID",
                    "Rating must be in range from 1 to 5"
            );
        }

        GeoUtils.requireCoordinates(req.latitude(), req.longitude(), "REVIEW_COORDS_INVALID");

        UserReviewService.AddressReq address =
                validateAddress(req.address());

        List<UserReviewService.ObstacleSeverityItem>
                obstacles =
                normalizeObstacles(req.obstacles());

        List<String> photoUrls =
                normalizePhotoUrls(
                        req.photoUrls(),
                        userId
                );

        String comment = blankToNull(req.comment());
        if (comment != null && comment.length() > 1000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_COMMENT_TOO_LONG", "Comment is too long");
        }

        String primaryObstacleType =
                choosePrimaryObstacleType(
                        obstacles
                );

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

    private UserReviewService.AddressReq
    validateAddress(
            UserReviewService.AddressReq raw
    ) {

        if (raw == null) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "ADDRESS_INVALID",
                    "Address is invalid"
            );
        }

        String country =
                InputRules.requireCyrillicText(
                        raw.country(),
                        "ADDRESS_COUNTRY_INVALID",
                        "Country"
                );

        String region =
                InputRules.requireCyrillicText(
                        raw.region(),
                        "ADDRESS_REGION_INVALID",
                        "Region"
                );

        String localityType =
                InputRules.requireCyrillicText(
                        raw.localityType(),
                        "ADDRESS_LOCALITY_TYPE_INVALID",
                        "Locality type"
                );

        String city =
                InputRules.requireCyrillicText(
                        raw.city(),
                        "ADDRESS_CITY_INVALID",
                        "City"
                );

        String street =
                InputRules.requireCyrillicText(
                        raw.street(),
                        "ADDRESS_STREET_INVALID",
                        "Street"
                );

        String house =
                InputRules.requireDigits(
                        raw.house(),
                        "ADDRESS_HOUSE_INVALID",
                        "House"
                );

        String placeName =
                blankToNull(
                        raw.placeName()
                );
        if (placeName != null && placeName.length() > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ADDRESS_PLACE_NAME_INVALID", "Place name is too long");
        }

        return new UserReviewService.AddressReq(
                country,
                region,
                localityType,
                city,
                street,
                house,
                placeName
        );
    }

    private List<UserReviewService.ObstacleSeverityItem>
    normalizeObstacles(
            List<UserReviewService.ObstacleSeverityItem>
                    rawItems
    ) {

        Map<String, Short> normalized =
                new LinkedHashMap<>();

        for (String type :
                ObstacleType.allNames()) {

            normalized.put(
                    type,
                    (short) 0
            );
        }

        if (rawItems != null) {

            for (UserReviewService
                    .ObstacleSeverityItem rawItem
                    : rawItems) {

                if (rawItem == null) {

                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "OBSTACLE_EMPTY",
                            "Obstacle is empty"
                    );
                }

                String type =
                        ObstacleType.normalize(
                                rawItem.obstacleType()
                        );

                short severity =
                        rawItem.severity();

                if (severity < 0
                        || severity > 3) {

                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "OBSTACLE_SEVERITY_INVALID",
                            "Obstacle severity is invalid"
                    );
                }

                normalized.put(
                        type,
                        severity
                );
            }
        }

        boolean hasPositive = false;

        List<UserReviewService
                .ObstacleSeverityItem> out =
                new ArrayList<>();

        for (String type :
                ObstacleType.allNames()) {

            short severity =
                    normalized.get(type);

            if (severity > 0) {
                hasPositive = true;
            }

            out.add(
                    new UserReviewService
                            .ObstacleSeverityItem(
                            type,
                            severity
                    )
            );
        }

        if (!hasPositive) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "OBSTACLE_LIST_EMPTY",
                    "At least one obstacle must have positive severity"
            );
        }

        return out;
    }

    private List<String> normalizePhotoUrls(
            Collection<String> rawUrls,
            Long userId
    ) {

        List<String> out = new ArrayList<>();

        if (rawUrls == null) {
            return List.of();
        }
        if (rawUrls.size() > 10) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_PHOTO_LIMIT_EXCEEDED", "Too many review photos");
        }

        for (String raw : rawUrls) {

            String value =
                    blankToNull(raw);

            if (value != null) {
                out.add(trustedUrls.requireOwnedStorageUrl(
                        value,
                        "reviews",
                        userId,
                        "REVIEW_PHOTO_URL_INVALID"
                ));
            }
        }

        return out;
    }

    private String choosePrimaryObstacleType(
            List<UserReviewService
                    .ObstacleSeverityItem>
                    obstacles
    ) {

        String bestType = null;
        short bestSeverity = -1;

        for (UserReviewService
                .ObstacleSeverityItem item
                : obstacles) {

            if (item.severity()
                    > bestSeverity) {

                bestSeverity =
                        item.severity();

                bestType =
                        item.obstacleType();
            }
        }

        if (bestType == null
                || bestSeverity == 0) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "OBSTACLE_LIST_EMPTY",
                    "At least one obstacle must have positive severity"
            );
        }

        return bestType;
    }

    private static String blankToNull(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String s = value.trim();

        return s.isEmpty()
                ? null
                : s;
    }

    public record ValidatedReviewInput(
            double latitude,
            double longitude,
            UserReviewService.AddressReq address,
            short rating,
            List<UserReviewService
                    .ObstacleSeverityItem>
            obstacles,
            String primaryObstacleType,
            String comment,
            List<String> photoUrls
    ) {}

}