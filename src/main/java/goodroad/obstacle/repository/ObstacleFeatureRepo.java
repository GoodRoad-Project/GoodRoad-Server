package goodroad.obstacle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
            @Param("latMin") double latMin,
            @Param("latMax") double latMax,
            @Param("lonMin") double lonMin,
            @Param("lonMax") double lonMax,
            @Param("status") String status
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
              and ((:placeName is null and obstacleFeature.placeName is null)
                   or obstacleFeature.placeName = :placeName)
            """)
    Optional<ObstacleFeatureEntity> findByAddressAndType(
            @Param("type") String type,
            @Param("country") String country,
            @Param("region") String region,
            @Param("localityType") String localityType,
            @Param("city") String city,
            @Param("street") String street,
            @Param("house") String house,
            @Param("placeName") String placeName
    );

    @Query("""
            select obstacleFeature
            from ObstacleFeatureEntity obstacleFeature
            where obstacleFeature.lat between :latMin and :latMax
              and obstacleFeature.lon between :lonMin and :lonMax
              and obstacleFeature.reviewsCount <= :maxReviews
            order by obstacleFeature.reviewsCount asc, obstacleFeature.id
            """)
    List<ObstacleFeatureEntity> findLowReviewedByBbox(
            @Param("latMin") Double latMin,
            @Param("latMax") Double latMax,
            @Param("lonMin") Double lonMin,
            @Param("lonMax") Double lonMax,
            @Param("maxReviews") Integer maxReviews
    );
}