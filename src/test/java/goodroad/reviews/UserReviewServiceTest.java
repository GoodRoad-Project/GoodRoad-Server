package goodroad.reviews;

import goodroad.model.ObstacleType;
import goodroad.obstacle.repository.ObstacleFeatureEntity;
import goodroad.obstacle.repository.ObstacleFeatureRepo;
import goodroad.reviews.repository.*;
import goodroad.security.Crypto;
import goodroad.storage.StorageService;
import goodroad.users.repository.UserEntity;
import goodroad.users.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserReviewServiceTest {

    @Mock
    private UserRepo users;

    @Mock
    private ObstacleFeatureRepo features;

    @Mock
    private ObstacleReviewRepo reviews;

    @Mock
    private ObstacleReviewPhotoRepo photos;

    @Mock
    private ObstacleReviewObstacleRepo reviewObstacles;

    @Mock
    private ReviewSupportService reviewSupport;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserReviewService service;

    @Test
    void shouldCreateReview() {
        UserEntity user = user(1L);
        ObstacleFeatureEntity feature = feature(10L);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(features.findByAddressAndType(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.of(feature));
        when(reviews.findByFeatureIdAndAuthorId(10L, 1L)).thenReturn(Optional.empty());
        when(reviews.save(any(ObstacleReviewEntity.class))).thenAnswer(invocation -> {
            ObstacleReviewEntity review = invocation.getArgument(0);
            review.setId(20L);
            return review;
        });
        when(reviewSupport.loadBundle(anyList())).thenReturn(bundle(feature));

        UserReviewService.ReviewCardResp result = service.createReview("+79990000001", request());

        assertEquals("20", result.id());
        assertEquals("10", result.featureId());
        assertEquals("PENDING", result.status());
        verify(reviewObstacles, times(ObstacleType.allNames().size()))
                .save(any(ObstacleReviewObstacleEntity.class));
        verify(photos).save(any(ObstacleReviewPhotoEntity.class));
    }

    @Test
    void shouldRejectDuplicateReview() {
        UserEntity user = user(1L);
        ObstacleFeatureEntity feature = feature(10L);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(features.findByAddressAndType(anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(Optional.of(feature));
        when(reviews.findByFeatureIdAndAuthorId(10L, 1L)).thenReturn(Optional.of(new ObstacleReviewEntity()));

        assertThrows(RuntimeException.class, () -> service.createReview("+79990000001", request()));
    }

    @Test
    void shouldReturnReviewPoints() {
        UserEntity user = user(1L);
        user.setTotalPoints(20);
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(reviews.countByAuthorIdAndStatus(1L, "APPROVED")).thenReturn(2L);

        UserReviewService.ReviewPointsResp result = service.getOwnReviewPoints("+79990000001");

        assertEquals(20, result.totalPoints());
        assertEquals(2, result.approvedReviews());
    }

    @Test
    void shouldUploadReviewPhoto() {
        UserEntity user = user(1L);
        MockMultipartFile file = new MockMultipartFile(
                "file", "review.png", "image/png", new byte[] {1, 2, 3}
        );
        when(users.findByPhoneHash(anyString())).thenReturn(Optional.of(user));
        when(storageService.uploadReviewPhoto(file, "1")).thenReturn("http://photo");

        UserReviewService.ReviewPhotoUploadResp result = service.uploadReviewPhoto("+79990000001", file);

        assertEquals("http://photo", result.photoUrl());
    }

    private UserReviewService.UpsertReviewReq request() {
        return new UserReviewService.UpsertReviewReq(
                59.93,
                30.33,
                new UserReviewService.AddressReq(
                        "Россия", "Санкт-Петербург", "город", "Санкт-Петербург", "Садовая", "12", null
                ),
                (short) 4,
                List.of(new UserReviewService.ObstacleSeverityItem("STAIRS", (short) 3)),
                "Комментарий",
                List.of("http://photo")
        );
    }

    private UserEntity user(Long id) {
        UserEntity user = UserEntity.builder()
                .phoneHash(Crypto.sha256Hex("79990000001"))
                .role("USER")
                .active(true)
                .totalPoints(0)
                .build();
        user.setId(id);
        return user;
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
                .build();
        feature.setId(id);
        return feature;
    }

    private ReviewSupportService.ReviewBundle bundle(ObstacleFeatureEntity feature) {
        return new ReviewSupportService.ReviewBundle(
                java.util.Map.of(feature.getId(), feature),
                java.util.Map.of(20L, List.of("http://photo")),
                java.util.Map.of(20L, List.of(new ReviewSupportService.ReviewObstacleItem("STAIRS", (short) 3)))
        );
    }
}
