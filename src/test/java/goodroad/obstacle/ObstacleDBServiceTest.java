package goodroad.obstacle;

import goodroad.model.ReviewStatus;
import goodroad.obstacle.repository.*;
import goodroad.reviews.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObstacleDBServiceTest {

    @Mock
    private ObstacleFeatureRepo features;

    @Mock
    private ObstacleFeatureObstacleScoreRepo obstacleScores;

    @Mock
    private ObstacleReviewRepo reviews;

    @Mock
    private ObstacleReviewPhotoRepo photos;

    @Mock
    private ObstacleReviewObstacleRepo reviewObstacles;

    @InjectMocks
    private ObstacleDBService service;

    @Test
    void shouldListApprovedObstaclesInBox() {
        ObstacleFeatureEntity feature = feature(1L);
        when(features.findByBboxWithReviewStatus(
                59.0, 60.0, 30.0, 31.0, ReviewStatus.APPROVED.name()
        )).thenReturn(List.of(feature));
        when(obstacleScores.findByIdFeatureIdIn(List.of(1L))).thenReturn(List.of(
                ObstacleFeatureObstacleScoreEntity.builder()
                        .id(new ObstacleFeatureObstacleScoreKey(1L, "STAIRS"))
                        .severityEstimate((short) 3)
                        .reviewsCount(1)
                        .build()
        ));

        List<ObstacleDBService.ObstacleMapItemResp> result = service.listInBox(59.0, 60.0, 30.0, 31.0);

        assertEquals(1, result.size());
        assertEquals("1", result.get(0).id());
        assertEquals((short) 3, result.get(0).obstacleSeverityEstimates().get("STAIRS"));
    }

    @Test
    void shouldRejectInvalidBox() {
        assertThrows(RuntimeException.class,
                () -> service.listInBox(61.0, 60.0, 30.0, 31.0));
    }

    @Test
    void shouldGetObstacleCard() {
        ObstacleFeatureEntity feature = feature(1L);
        ObstacleReviewEntity review = review(10L, 1L);
        when(features.findById(1L)).thenReturn(Optional.of(feature));
        when(reviews.countByFeatureIdAndStatus(1L, ReviewStatus.APPROVED.name())).thenReturn(1L);
        when(reviews.findByFeatureIdAndStatusOrderByCreatedAtDesc(1L, ReviewStatus.APPROVED.name()))
                .thenReturn(List.of(review));
        when(photos.findByReviewIdIn(List.of(10L))).thenReturn(List.of(
                ObstacleReviewPhotoEntity.builder()
                        .reviewId(10L)
                        .url("http://photo")
                        .createdAt(Instant.now())
                        .build()
        ));
        when(reviewObstacles.findByIdReviewIdIn(List.of(10L))).thenReturn(List.of(
                ObstacleReviewObstacleEntity.builder()
                        .id(new ObstacleReviewObstacleKey(10L, "STAIRS"))
                        .severity((short) 3)
                        .build()
        ));

        ObstacleDBService.ObstacleCardResp card = service.getCard("1");

        assertEquals("1", card.id());
        assertEquals(1, card.reviews().size());
        assertEquals("http://photo", card.reviews().get(0).photoUrls().get(0));
    }

    private ObstacleFeatureEntity feature(Long id) {
        ObstacleFeatureEntity feature = ObstacleFeatureEntity.builder()
                .type("STAIRS")
                .lat(59.93)
                .lon(30.33)
                .country("Россия")
                .region("Санкт-Петербург")
                .localityType("город")
                .city("Санкт-Петербург")
                .street("Садовая")
                .house("12")
                .severityEst((short) 4)
                .reviewsCount(1)
                .lastReviewedAt(Instant.now())
                .build();
        feature.setId(id);
        return feature;
    }

    private ObstacleReviewEntity review(Long id, Long featureId) {
        ObstacleReviewEntity review = ObstacleReviewEntity.builder()
                .featureId(featureId)
                .authorId(1L)
                .severity((short) 4)
                .text("text")
                .createdAt(Instant.now())
                .status(ReviewStatus.APPROVED.name())
                .awardedPoints(10)
                .build();
        review.setId(id);
        return review;
    }
}
