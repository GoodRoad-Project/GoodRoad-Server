package GoodRoad.obstacle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ObstacleFeatureRepo extends JpaRepository<ObstacleFeatureEntity, UUID> {

    @Query("""
            select f
            from ObstacleFeatureEntity f
            where f.lat between :latMin and :latMax
              and f.lon between :lonMin and :lonMax
              and exists (
                  select 1
                  from ObstacleReviewEntity r
                  where r.featureId = f.id
                    and r.status = :status
              )
            """)
    List<ObstacleFeatureEntity> findByBboxWithReviewStatus(
            double latMin, double latMax,
            double lonMin, double lonMax,
            String status
    );

    @Query("""
            select f
            from ObstacleFeatureEntity f
            where f.type = :type
              and f.country = :country
              and f.region = :region
              and f.localityType = :localityType
              and f.city = :city
              and f.street = :street
              and f.house = :house
              and ((:placeName is null and f.placeName is null) or f.placeName = :placeName)
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
