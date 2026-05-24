package goodroad.volunteer.repository;

import goodroad.users.repository.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "volunteer_sos_notification")
public class SosNotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private HelpRequestEntity request;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @Column(name = "reason", nullable = false, length = 64)
    private String reason;

    @Column(name = "comment", length = 1000)
    private String comment;

    @Column(name = "status", nullable = false, length = 16)
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private UserEntity moderator;

    @Column(name = "moderator_comment", length = 1000)
    private String moderatorComment;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "OPEN";
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public HelpRequestEntity getRequest() { return request; }
    public void setRequest(HelpRequestEntity request) { this.request = request; }
    public UserEntity getSender() { return sender; }
    public void setSender(UserEntity sender) { this.sender = sender; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UserEntity getModerator() { return moderator; }
    public void setModerator(UserEntity moderator) { this.moderator = moderator; }
    public String getModeratorComment() { return moderatorComment; }
    public void setModeratorComment(String moderatorComment) { this.moderatorComment = moderatorComment; }
    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
