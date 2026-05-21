package goodroad.reviews;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.ObstacleType;
import goodroad.validation.InputRules;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReviewValidationService {

    public ValidatedReviewInput validate(
            UserReviewService.UpsertReviewReq req
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

        if (Double.isNaN(req.latitude())
                || Double.isNaN(req.longitude())) {

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REVIEW_COORDS_INVALID",
                    "Coordinates are invalid"
            );
        }

        UserReviewService.AddressReq address =
                validateAddress(req.address());

        List<UserReviewService.ObstacleSeverityItem>
                obstacles =
                normalizeObstacles(req.obstacles());

        List<String> photoUrls =
                normalizePhotoUrls(
                        req.photoUrls()
                );

        String comment =
                blankToNull(
                        req.comment()
                );

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
                InputRules.requireAddressText(
                        raw.country(),
                        "ADDRESS_COUNTRY_INVALID",
                        "Country"
                );

        String region =
                InputRules.requireAddressText(
                        raw.region(),
                        "ADDRESS_REGION_INVALID",
                        "Region"
                );

        String localityType =
                InputRules.requireAddressText(
                        raw.localityType(),
                        "ADDRESS_LOCALITY_TYPE_INVALID",
                        "Locality type"
                );

        String city =
                InputRules.requireAddressText(
                        raw.city(),
                        "ADDRESS_CITY_INVALID",
                        "City"
                );

        String street =
                InputRules.requireAddressText(
                        raw.street(),
                        "ADDRESS_STREET_INVALID",
                        "Street"
                );

        String house =
                InputRules.requireAddressText(
                        raw.house(),
                        "ADDRESS_HOUSE_INVALID",
                        "House"
                );

        String placeName =
                blankToNull(
                        raw.placeName()
                );

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
            Collection<String> rawUrls
    ) {

        List<String> out =
                new ArrayList<>();

        if (rawUrls == null) {
            return out;
        }

        for (String raw : rawUrls) {

            String value =
                    blankToNull(raw);

            if (value != null) {
                out.add(value);
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