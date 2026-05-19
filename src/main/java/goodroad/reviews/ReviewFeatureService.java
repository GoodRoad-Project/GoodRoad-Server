package goodroad.reviews;

import goodroad.api.ApiErrors.ApiException;
import goodroad.obstacle.repository.ObstacleFeatureEntity;
import goodroad.obstacle.repository.ObstacleFeatureRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ReviewFeatureService {

    private final ObstacleFeatureRepo features;

    public ReviewFeatureService(ObstacleFeatureRepo features) {
        this.features = features;
    }

    public ObstacleFeatureEntity resolveOrCreateFeature(
            ReviewValidationService.ValidatedReviewInput input
    ) {

        Optional<ObstacleFeatureEntity> existing =
                features.findByAddressAndType(
                        input.primaryObstacleType(),
                        input.address().country(),
                        input.address().region(),
                        input.address().localityType(),
                        input.address().city(),
                        input.address().street(),
                        input.address().house(),
                        input.address().placeName()
                );

        if (existing.isPresent()) {
            return existing.get();
        }

        return createFeature(input);
    }

    private ObstacleFeatureEntity createFeature(
            ReviewValidationService.ValidatedReviewInput input
    ) {

        ObstacleFeatureEntity created =
                ObstacleFeatureEntity.builder()
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
}