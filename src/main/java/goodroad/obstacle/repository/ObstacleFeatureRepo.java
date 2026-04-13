package goodroad.obstacle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface ObstacleFeatureRepo extends JpaRepository<ObstacleFeatureEntity, Long> {

    @Query("""
            select obstacleFeature
            from ObstacleFeatureEntity obstacleFeature
            where obstacleFeature.lat between :latMin and :latMax
              and obstacleFeature.lon between :lonMin and :lonMax
              and exists (
                  select 1
                  from ObstacleReviewEntity obstacleReview
                  where obstacleReview.featureId = obstacleFeature.id
                    and obstacleReview.status = :status
              )
            """)
    List<ObstacleFeatureEntity> findByBboxWithReviewStatus(
            double latMin, double latMax,
            double lonMin, double lonMax,
            String status
    );

    @Query("""
            select obstacleFeature
            from ObstacleFeatureEntity obstacleFeature
            where obstacleFeature.type = :type
              and obstacleFeature.country = :country
              and obstacleFeature.region = :region
              and obstacleFeature.localityType = :localityType
              and obstacleFeature.city = :city
              and obstacleFeature.street = :street
              and obstacleFeature.house = :house
              and ((:placeName is null and obstacleFeature.placeName is null) or obstacleFeature.placeName = :placeName)
            """)
    Optional<ObstacleFeatureEntity> findByAddressAndType(
            String type,
            String country,
            String region,
            String localityType,
            String city,
            String street,
            String house,
            String placeName
    );
}
