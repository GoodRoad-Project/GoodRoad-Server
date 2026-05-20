package goodroad.reviews;

import goodroad.model.Role;
import goodroad.obstacle.repository.ObstacleFeatureEntity;
import goodroad.reviews.repository.*;
import goodroad.security.Crypto;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
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
class ReviewModerationServiceTest {

    @Mock
    private ObstacleReviewRepo reviews;

    @Mock
    private ObstacleReviewPhotoRepo photos;

    @Mock
    private UserRepo users;

    @Mock
    private ReviewSupportService reviewSupport;

    @InjectMocks
    private ReviewModerationService service;

    @Test
    void shouldTakeReviewInWork() {
        UserEntity moderator = user(2L, Role.MODERATOR.name());
        ObstacleReviewEntity review = review();
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(moderator));
        when(reviews.findByIdForUpdate(10L)).thenReturn(Optional.of(review));
        when(reviewSupport.loadBundle(anyList())).thenReturn(bundle());

        ReviewModerationService.ReviewView result = service.takeInWork("+79990000003", "10");

        assertEquals("10", result.id());
        assertTrue(result.takenByMe());
        assertEquals(2L, review.getTakenByModeratorId());
        verify(reviews).save(review);
    }

    @Test
    void shouldApproveTakenReviewAndAddPoints() {
        UserEntity moderator = user(2L, Role.MODERATOR.name());
        UserEntity author = user(1L, Role.USER.name());
        ObstacleReviewEntity review = review();
        review.setTakenByModeratorId(2L);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(moderator));
        when(reviews.findByIdForUpdate(10L)).thenReturn(Optional.of(review));
        when(photos.existsByReviewId(10L)).thenReturn(true);
        when(users.findById(1L)).thenReturn(Optional.of(author));

        service.approve("+79990000003", "10");

        assertEquals("APPROVED", review.getStatus());
        assertEquals(20, review.getAwardedPoints());
        assertEquals(20, author.getTotalPoints());
        verify(reviewSupport).recomputeFeatureAggregate(100L);
    }

    @Test
    void shouldRejectTakenReview() {
        UserEntity moderator = user(2L, Role.MODERATOR.name());
        ObstacleReviewEntity review = review();
        review.setTakenByModeratorId(2L);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(moderator));
        when(reviews.findByIdForUpdate(10L)).thenReturn(Optional.of(review));

        service.reject("+79990000003", "10", "Некорректные данные");

        assertEquals("REJECTED", review.getStatus());
        assertEquals("Некорректные данные", review.getModeratorComment());
        verify(reviews).save(review);
    }

    @Test
    void shouldReleaseReview() {
        UserEntity moderator = user(2L, Role.MODERATOR.name());
        ObstacleReviewEntity review = review();
        review.setTakenByModeratorId(2L);
        review.setTakenAt(Instant.now());
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(moderator));
        when(reviews.findByIdForUpdate(10L)).thenReturn(Optional.of(review));

        service.release("+79990000003", "10");

        assertNull(review.getTakenByModeratorId());
        assertNull(review.getTakenAt());
        verify(reviews).save(review);
    }

    private UserEntity user(Long id, String role) {
        UserEntity user = UserEntity.builder()
                .phoneHash(Crypto.sha256Hex("79990000003"))
                .role(role)
                .active(true)
                .totalPoints(0)
                .build();
        user.setId(id);
        return user;
    }

    private ObstacleReviewEntity review() {
        ObstacleReviewEntity review = ObstacleReviewEntity.builder()
                .featureId(100L)
                .authorId(1L)
                .severity((short) 4)
                .text("Комментарий")
                .createdAt(Instant.now())
                .status("PENDING")
                .awardedPoints(0)
                .build();
        review.setId(10L);
        return review;
    }

    private ReviewSupportService.ReviewBundle bundle() {
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
                .build();
        feature.setId(100L);
        return new ReviewSupportService.ReviewBundle(
                java.util.Map.of(100L, feature),
                java.util.Map.of(10L, List.of("http://photo")),
                java.util.Map.of(10L, List.of(new ReviewSupportService.ReviewObstacleItem("STAIRS", (short) 3)))
        );
    }
}
