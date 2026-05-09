package goodroad.obstacle;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.ReviewStatus;
import goodroad.obstacle.repository.*;
import goodroad.reviews.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class ObstacleDBService {

    private final ObstacleFeatureRepo features;
    private final ObstacleFeatureObstacleScoreRepo obstacleScores;
    private final ObstacleReviewRepo reviews;
    private final ObstacleReviewPhotoRepo photos;
    private final ObstacleReviewObstacleRepo reviewObstacles;

    public ObstacleDBService(
            ObstacleFeatureRepo features,
            ObstacleFeatureObstacleScoreRepo obstacleScores,
            ObstacleReviewRepo reviews,
            ObstacleReviewPhotoRepo photos,
            ObstacleReviewObstacleRepo reviewObstacles
    ) {
        this.features = features;
        this.obstacleScores = obstacleScores;
        this.reviews = reviews;
        this.photos = photos;
        this.reviewObstacles = reviewObstacles;
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

    public record ReviewResp(
            String id,
            short rating,
            String comment,
            Instant createdAt,
            List<String> photoUrls,
            List<ObstacleItemResp> obstacles
    ) {
    }

    public record ObstacleMapItemResp(
            String id,
            String type,
            double latitude,
            double longitude,
            AddressResp address,
            Short severityEstimate,
            Map<String, Short> obstacleSeverityEstimates,
            int reviewsCount,
            Instant lastReviewedAt
    ) {
    }

    public record ObstacleCardResp(
            String id,
            String type,
            double latitude,
            double longitude,
            AddressResp address,
            Short severityEstimate,
            int reviewsCount,
            Instant lastReviewedAt,
            List<ReviewResp> reviews
    ) {
    }

    @Transactional(readOnly = true)
    public List<ObstacleMapItemResp> listInBox(double minLat, double maxLat, double minLon, double maxLon) {
        validateBBox(minLat, maxLat, minLon, maxLon);

        List<ObstacleFeatureEntity> rawFeatures = features.findByBboxWithReviewStatus(
                minLat,
                maxLat,
                minLon,
                maxLon,
                ReviewStatus.APPROVED.name()
        );

        List<Long> featureIds = new ArrayList<>();
        for (ObstacleFeatureEntity feature : rawFeatures) {
            featureIds.add(feature.getId());
        }

        Map<Long, Map<String, Short>> scoresByFeature = loadScoresByFeature(featureIds);
        List<ObstacleMapItemResp> out = new ArrayList<>();
        for (ObstacleFeatureEntity feature : rawFeatures) {
            out.add(new ObstacleMapItemResp(
                    feature.getId().toString(),
                    feature.getType(),
                    feature.getLat(),
                    feature.getLon(),
                    toAddress(feature),
                    feature.getSeverityEst(),
                    scoresByFeature.getOrDefault(feature.getId(), Map.of()),
                    feature.getReviewsCount(),
                    feature.getLastReviewedAt()
            ));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public ObstacleCardResp getCard(String featureId) {
        Long id = parseId(featureId);
        ObstacleFeatureEntity feature = features.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "OBSTACLE_FEATURE_NOT_FOUND", "Obstacle feature not found"));

        if (reviews.countByFeatureIdAndStatus(id, ReviewStatus.APPROVED.name()) == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "OBSTACLE_FEATURE_NOT_FOUND", "Obstacle feature not found");
        }

        List<ObstacleReviewEntity> approved = reviews.findByFeatureIdAndStatusOrderByCreatedAtDesc(
                id,
                ReviewStatus.APPROVED.name()
        );
        List<Long> reviewIds = new ArrayList<>();
        for (ObstacleReviewEntity review : approved) {
            reviewIds.add(review.getId());
        }

        Map<Long, List<String>> photosByReview = loadPhotosByReview(reviewIds);
        Map<Long, List<ObstacleItemResp>> obstaclesByReview = loadObstaclesByReview(reviewIds);

        List<ReviewResp> items = new ArrayList<>();
        for (ObstacleReviewEntity review : approved) {
            items.add(new ReviewResp(
                    review.getId().toString(),
                    review.getSeverity(),
                    review.getText(),
                    review.getCreatedAt(),
                    photosByReview.getOrDefault(review.getId(), List.of()),
                    obstaclesByReview.getOrDefault(review.getId(), List.of())
            ));
        }

        return new ObstacleCardResp(
                feature.getId().toString(),
                feature.getType(),
                feature.getLat(),
                feature.getLon(),
                toAddress(feature),
                feature.getSeverityEst(),
                feature.getReviewsCount(),
                feature.getLastReviewedAt(),
                items
        );
    }

    private Map<Long, Map<String, Short>> loadScoresByFeature(List<Long> featureIds) {
        Map<Long, Map<String, Short>> scoresByFeature = new LinkedHashMap<>();
        if (featureIds.isEmpty()) {
            return scoresByFeature;
        }

        for (ObstacleFeatureObstacleScoreEntity score : obstacleScores.findByIdFeatureIdIn(featureIds)) {
            scoresByFeature.computeIfAbsent(score.getId().getFeatureId(), k -> new LinkedHashMap<>())
                    .put(score.getId().getObstacleType(), score.getSeverityEstimate());
        }
        return scoresByFeature;
    }

    private Map<Long, List<String>> loadPhotosByReview(List<Long> reviewIds) {
        Map<Long, List<String>> photosByReview = new LinkedHashMap<>();
        for (ObstacleReviewPhotoEntity photo : photos.findByReviewIdIn(reviewIds)) {
            photosByReview.computeIfAbsent(photo.getReviewId(), k -> new ArrayList<>()).add(photo.getUrl());
        }
        return photosByReview;
    }

    private Map<Long, List<ObstacleItemResp>> loadObstaclesByReview(List<Long> reviewIds) {
        Map<Long, List<ObstacleItemResp>> obstaclesByReview = new LinkedHashMap<>();
        for (ObstacleReviewObstacleEntity item : reviewObstacles.findByIdReviewIdIn(reviewIds)) {
            obstaclesByReview.computeIfAbsent(item.getId().getReviewId(), k -> new ArrayList<>())
                    .add(new ObstacleItemResp(
                            item.getId().getObstacleType(),
                            item.getSeverity()
                    ));
        }
        return obstaclesByReview;
    }

    private AddressResp toAddress(ObstacleFeatureEntity feature) {
        return new AddressResp(
                feature.getCountry(),
                feature.getRegion(),
                feature.getLocalityType(),
                feature.getCity(),
                feature.getStreet(),
                feature.getHouse(),
                feature.getPlaceName()
        );
    }

    private void validateBBox(double minLat, double maxLat, double minLon, double maxLon) {
        if (Double.isNaN(minLat) || Double.isNaN(maxLat) || Double.isNaN(minLon) || Double.isNaN(maxLon)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BBOX_INVALID", "Bbox is invalid");
        }
        if (minLat < -90 || maxLat > 90 || minLon < -180 || maxLon > 180) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BBOX_INVALID", "Bbox is invalid");
        }
        if (minLat > maxLat || minLon > maxLon) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "BBOX_INVALID", "Bbox is invalid");
        }
    }

    private Long parseId(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ID_INVALID", "Id is invalid");
        }
    }
}