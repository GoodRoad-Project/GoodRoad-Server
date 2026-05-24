package goodroad.volunteer.repository;

import goodroad.users.repository.UserEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "help_request")
public class HelpRequestEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private UserEntity requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id")
    private UserEntity volunteer;

    @Column(name = "from_address", nullable = false, length = 500)
    private String fromAddress;

    @Column(name = "to_address", nullable = false, length = 500)
    private String toAddress;

    @Column(name = "walk_date", nullable = false)
    private LocalDate date;

    @Column(name = "walk_time", nullable = false)
    private LocalTime time;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "social_nickname", length = 120)
    private String socialNickname;

    @Column(name = "comment", nullable = false, length = 2000)
    private String comment;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "OPEN";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "requester_started_at")
    private Instant requesterStartedAt;

    @Column(name = "volunteer_started_at")
    private Instant volunteerStartedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "requester_finished_at")
    private Instant requesterFinishedAt;

    @Column(name = "volunteer_finished_at")
    private Instant volunteerFinishedAt;

    @Column(name = "completed_at")
    private Instant completedAt;


    @Column(name = "planned_route_points", columnDefinition = "text")
    private String plannedRoutePoints;

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserEntity getRequester() { return requester; }
    public void setRequester(UserEntity requester) { this.requester = requester; }
    public UserEntity getVolunteer() { return volunteer; }
    public void setVolunteer(UserEntity volunteer) { this.volunteer = volunteer; }
    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }
    public String getToAddress() { return toAddress; }
    public void setToAddress(String toAddress) { this.toAddress = toAddress; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTime() { return time; }
    public void setTime(LocalTime time) { this.time = time; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSocialNickname() { return socialNickname; }
    public void setSocialNickname(String socialNickname) { this.socialNickname = socialNickname; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public Instant getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(Instant cancelledAt) { this.cancelledAt = cancelledAt; }
    public Instant getRequesterStartedAt() { return requesterStartedAt; }
    public void setRequesterStartedAt(Instant requesterStartedAt) { this.requesterStartedAt = requesterStartedAt; }
    public Instant getVolunteerStartedAt() { return volunteerStartedAt; }
    public void setVolunteerStartedAt(Instant volunteerStartedAt) { this.volunteerStartedAt = volunteerStartedAt; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getRequesterFinishedAt() { return requesterFinishedAt; }
    public void setRequesterFinishedAt(Instant requesterFinishedAt) { this.requesterFinishedAt = requesterFinishedAt; }
    public Instant getVolunteerFinishedAt() { return volunteerFinishedAt; }
    public void setVolunteerFinishedAt(Instant volunteerFinishedAt) { this.volunteerFinishedAt = volunteerFinishedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public String getPlannedRoutePoints() { return plannedRoutePoints; }
    public void setPlannedRoutePoints(String plannedRoutePoints) { this.plannedRoutePoints = plannedRoutePoints; }
}
