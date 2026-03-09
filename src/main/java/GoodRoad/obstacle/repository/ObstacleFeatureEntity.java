package GoodRoad.obstacle.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "obstacle_feature")
public class ObstacleFeatureEntity { // тут храним точку препятствия

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "type", nullable = false, length = 32)
    private String type;

    @Column(name = "latitude", nullable = false)
    private double lat;

    @Column(name = "longitude", nullable = false)
    private double lon;

    @Column(name = "country", length = 120)
    private String country;

    @Column(name = "region", length = 120)
    private String region;

    @Column(name = "locality_type", length = 80)
    private String localityType;

    @Column(name = "city", length = 120)
    private String city;

    @Column(name = "street", length = 120)
    private String street;

    @Column(name = "house", length = 40)
    private String house;

    @Column(name = "place_name", length = 180)
    private String placeName;

    @Column(name = "severity_estimate")
    private Short severityEst;

    @Column(name = "reviews_count", nullable = false)
    private int reviewsCount;

    @Column(name = "last_reviewed_at")
    private Instant lastReviewedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getLocalityType() {
        return localityType;
    }

    public void setLocalityType(String localityType) {
        this.localityType = localityType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getHouse() {
        return house;
    }

    public void setHouse(String house) {
        this.house = house;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public Short getSeverityEst() {
        return severityEst;
    }

    public void setSeverityEst(Short severityEst) {
        this.severityEst = severityEst;
    }

    public int getReviewsCount() {
        return reviewsCount;
    }

    public void setReviewsCount(int reviewsCount) {
        this.reviewsCount = reviewsCount;
    }

    public Instant getLastReviewedAt() {
        return lastReviewedAt;
    }

    public void setLastReviewedAt(Instant lastReviewedAt) {
        this.lastReviewedAt = lastReviewedAt;
    }
}
