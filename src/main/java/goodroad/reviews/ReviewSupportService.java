package goodroad.reviews;

import goodroad.api.ApiErrors.ApiException;
import goodroad.model.ObstacleType;
import goodroad.obstacle.repository.ObstacleFeatureEntity;
import goodroad.obstacle.repository.ObstacleFeatureRepo;
import goodroad.reviews.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.*;

@Service
public class ReviewSupportService {

    private static final String STATUS_APPROVED = "APPROVED";

    private final ObstacleFeatureRepo features;
    private final ObstacleReviewRepo reviews;
    private final ObstacleReviewPhotoRepo photos;
    private final ObstacleReviewObstacleRepo reviewObstacles;

    public ReviewSupportService(
            ObstacleFeatureRepo features,
            ObstacleReviewRepo reviews,
            ObstacleReviewPhotoRepo photos,
            ObstacleReviewObstacleRepo reviewObstacles
    ) {
        this.features = features;
        this.reviews = reviews;
        this.photos = photos;
        this.reviewObstacles = reviewObstacles;
    }

    public record ReviewObstacleItem(
            String obstacleType,
            short severity
    ) {
    }

    public record ReviewBundle(
            Map<Long, ObstacleFeatureEntity> featureById,
            Map<Long, List<String>> photosByReview,
            Map<Long, List<ReviewObstacleItem>> obstaclesByReview
    ) {
    }

    public ReviewBundle loadBundle(List<ObstacleReviewEntity> rawReviews) {
        if (rawReviews.isEmpty()) {
            return new ReviewBundle(Map.of(), Map.of(), Map.of());
        }

        List<Long> reviewIds = new ArrayList<>();
        List<Long> featureIds = new ArrayList<>();
        for (ObstacleReviewEntity review : rawReviews) {
            reviewIds.add(review.getId());
            featureIds.add(review.getFeatureId());
        }

        Map<Long, ObstacleFeatureEntity> featureById = new HashMap<>();
        for (ObstacleFeatureEntity feature : features.findAllById(featureIds)) {
            featureById.put(feature.getId(), feature);
        }

        Map<Long, List<String>> photosByReview = new LinkedHashMap<>();
        for (ObstacleReviewPhotoEntity photo : photos.findByReviewIdIn(reviewIds)) {
            photosByReview.computeIfAbsent(photo.getReviewId(), k -> new ArrayList<>()).add(photo.getUrl());
        }

        Map<Long, List<ReviewObstacleItem>> obstaclesByReview = new LinkedHashMap<>();
        for (ObstacleReviewObstacleEntity item : reviewObstacles.findByIdReviewIdIn(reviewIds)) {
            obstaclesByReview.computeIfAbsent(item.getId().getReviewId(), k -> new ArrayList<>())
                    .add(new ReviewObstacleItem(
                            item.getId().getObstacleType(),
                            item.getSeverity()
                    ));
        }

        for (List<ReviewObstacleItem> items : obstaclesByReview.values()) {
            items.sort((a, b) -> Integer.compare(obstacleOrder(a.obstacleType()), obstacleOrder(b.obstacleType())));
        }

        return new ReviewBundle(featureById, photosByReview, obstaclesByReview);
    }

    public void recomputeFeatureAggregate(Long featureId) {
        ObstacleFeatureEntity obstacleFeature = features.findById(featureId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "FEATURE_NOT_FOUND", "Feature with given id not found"));

        long cnt = reviews.countByFeatureIdAndStatus(featureId, STATUS_APPROVED);
        Double avg = reviews.avgSeverity(featureId, STATUS_APPROVED);
        Instant last = reviews.lastAt(featureId, STATUS_APPROVED);

        obstacleFeature.setReviewsCount((int) cnt);
        obstacleFeature.setLastReviewedAt(last);

        if (avg == null || cnt == 0) {
            obstacleFeature.setSeverityEst(null);
        } else {
            short est = (short) Math.max(1, Math.min(5, Math.round(avg.floatValue())));
            obstacleFeature.setSeverityEst(est);
        }

        features.save(obstacleFeature);
    }

    private int obstacleOrder(String obstacleType) {
        List<String> all = ObstacleType.allNames();
        int index = all.indexOf(obstacleType);
        return index >= 0 ? index : Integer.MAX_VALUE;
    }
}