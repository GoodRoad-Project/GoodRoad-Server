package goodroad.reviews;

import goodroad.obstacle.repository.ObstacleFeatureEntity;
import goodroad.reviews.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ReviewMapper {

    private final ReviewSupportService reviewSupport;
    private final ObstacleReviewPhotoRepo photos;
    private final ObstacleReviewObstacleRepo reviewObstacles;

    public ReviewMapper(
            ReviewSupportService reviewSupport,
            ObstacleReviewPhotoRepo photos,
            ObstacleReviewObstacleRepo reviewObstacles
    ) {
        this.reviewSupport = reviewSupport;
        this.photos = photos;
        this.reviewObstacles = reviewObstacles;
    }

    public List<UserReviewService.ReviewCardResp> buildCards(
            List<ObstacleReviewEntity> rawReviews
    ) {

        ReviewSupportService.ReviewBundle bundle =
                reviewSupport.loadBundle(rawReviews);

        Map<Long, ObstacleFeatureEntity> featureById =
                bundle.featureById();

        Map<Long, List<String>> photosByReview =
                bundle.photosByReview();

        Map<Long, List<ReviewSupportService.ReviewObstacleItem>>
                obstaclesByReview =
                bundle.obstaclesByReview();

        List<UserReviewService.ReviewCardResp> out =
                new ArrayList<>();

        for (ObstacleReviewEntity review : rawReviews) {

            ObstacleFeatureEntity feature =
                    featureById.get(review.getFeatureId());

            if (feature == null) {
                continue;
            }

            List<String> itemPhotos =
                    photosByReview.getOrDefault(
                            review.getId(),
                            List.of()
                    );

            List<UserReviewService.ObstacleSeverityItem>
                    itemObstacles =
                    new ArrayList<>();

            List<ReviewSupportService.ReviewObstacleItem>
                    rawObs =
                    obstaclesByReview.getOrDefault(
                            review.getId(),
                            List.of()
                    );

            for (ReviewSupportService.ReviewObstacleItem item
                    : rawObs) {

                itemObstacles.add(
                        new UserReviewService.ObstacleSeverityItem(
                                item.obstacleType(),
                                item.severity()
                        )
                );
            }

            int awardedPoints =
                    "APPROVED".equals(review.getStatus())
                            ? normalizePoints(review.getAwardedPoints())
                            : 0;

            out.add(
                    new UserReviewService.ReviewCardResp(
                            review.getId().toString(),
                            feature.getId().toString(),
                            new UserReviewService.AddressReq(
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
                    )
            );
        }

        return out;
    }

    public void saveReviewObstacles(
            Long reviewId,
            List<UserReviewService.ObstacleSeverityItem> obstacles
    ) {

        for (UserReviewService.ObstacleSeverityItem item : obstacles) {

            ObstacleReviewObstacleEntity entity =
                    ObstacleReviewObstacleEntity.builder()
                            .id(new ObstacleReviewObstacleKey(
                                    reviewId,
                                    item.obstacleType()
                            ))
                            .severity(item.severity())
                            .build();

            reviewObstacles.save(entity);
        }
    }

    public void savePhotos(
            Long reviewId,
            List<String> photoUrls
    ) {

        for (String url : photoUrls) {

            ObstacleReviewPhotoEntity photo =
                    ObstacleReviewPhotoEntity.builder()
                            .reviewId(reviewId)
                            .url(url)
                            .createdAt(Instant.now())
                            .build();

            photos.save(photo);
        }
    }

    private int normalizePoints(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }
}