package goodroad.reviews.repository;

import jakarta.persistence.*;
import lombok.Builder;
import java.time.Instant;

@Entity
@Table(name = "obstacle_review")
public class ObstacleReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "feature_id", nullable = false)
    private Long featureId;

    @Column(name = "author_id")
    private Long authorId;

    @Column(name = "severity", nullable = false)
    private Short severity;

    @Column(name = "text", length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @Column(name = "awarded_points", nullable = false)
    private Integer awardedPoints = 0;

    @Column(name = "taken_by_moderator_id")
    private Long takenByModeratorId;

    @Column(name = "taken_at")
    private Instant takenAt;

    @Column(name = "moderated_by")
    private Long moderatedBy;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @Column(name = "moderator_comment", length = 1000)
    private String moderatorComment;

    public ObstacleReviewEntity() {
    }

    @Builder
    public ObstacleReviewEntity(
            Long featureId,
            Long authorId,
            Short severity,
            String text,
            Instant createdAt,
            String status,
            Integer awardedPoints,
            Long takenByModeratorId,
            Instant takenAt,
            Long moderatedBy,
            Instant moderatedAt,
            String moderatorComment
    ) {
        this.featureId = featureId;
        this.authorId = authorId;
        this.severity = severity;
        this.text = text;
        this.createdAt = createdAt;
        this.status = status;
        this.awardedPoints = awardedPoints;
        this.takenByModeratorId = takenByModeratorId;
        this.takenAt = takenAt;
        this.moderatedBy = moderatedBy;
        this.moderatedAt = moderatedAt;
        this.moderatorComment = moderatorComment;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "PENDING";
        }
        if (awardedPoints == null || awardedPoints < 0) {
            awardedPoints = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFeatureId() {
        return featureId;
    }

    public void setFeatureId(Long featureId) {
        this.featureId = featureId;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public Short getSeverity() {
        return severity;
    }

    public void setSeverity(Short severity) {
        this.severity = severity;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAwardedPoints() {
        return awardedPoints;
    }

    public void setAwardedPoints(Integer awardedPoints) {
        this.awardedPoints = awardedPoints;
    }

    public Long getTakenByModeratorId() {
        return takenByModeratorId;
    }

    public void setTakenByModeratorId(Long takenByModeratorId) {
        this.takenByModeratorId = takenByModeratorId;
    }

    public Instant getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Instant takenAt) {
        this.takenAt = takenAt;
    }

    public Long getModeratedBy() {
        return moderatedBy;
    }

    public void setModeratedBy(Long moderatedBy) {
        this.moderatedBy = moderatedBy;
    }

    public Instant getModeratedAt() {
        return moderatedAt;
    }

    public void setModeratedAt(Instant moderatedAt) {
        this.moderatedAt = moderatedAt;
    }

    public String getModeratorComment() {
        return moderatorComment;
    }

    public void setModeratorComment(String moderatorComment) {
        this.moderatorComment = moderatorComment;
    }
}