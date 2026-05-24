package goodroad.volunteer.repository;

import goodroad.users.repository.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "volunteer_user_state")
public class VolunteerUserStateEntity {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserEntity user;

    @Column(name = "volunteer_warnings", nullable = false)
    private int volunteerWarnings;

    @Column(name = "requester_warnings", nullable = false)
    private int requesterWarnings;

    @Column(name = "volunteer_banned_until")
    private Instant volunteerBannedUntil;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public int getVolunteerWarnings() { return volunteerWarnings; }
    public void setVolunteerWarnings(int volunteerWarnings) { this.volunteerWarnings = volunteerWarnings; }
    public int getRequesterWarnings() { return requesterWarnings; }
    public void setRequesterWarnings(int requesterWarnings) { this.requesterWarnings = requesterWarnings; }
    public Instant getVolunteerBannedUntil() { return volunteerBannedUntil; }
    public void setVolunteerBannedUntil(Instant volunteerBannedUntil) { this.volunteerBannedUntil = volunteerBannedUntil; }
}
