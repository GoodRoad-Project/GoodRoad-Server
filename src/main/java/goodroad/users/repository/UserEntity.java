package goodroad.users.repository;

import lombok.Builder;
import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "users")
@SuppressWarnings("unused")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "first_name", length = 80)
    private String firstName;

    @Column(name = "last_name", length = 80)
    private String lastName;

    @Column(name = "phone_hash", nullable = false, unique = true, length = 64)
    private String phoneHash;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passHash;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "total_points", nullable = false)
    private Integer totalPoints = 0;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_active_at", nullable = false)
    private Instant lastActiveAt;

    public UserEntity() {
    }

    @Builder
    public UserEntity(
            String firstName,
            String lastName,
            String phoneHash,
            String role,
            String passHash,
            String photoUrl,
            boolean active,
            Integer totalPoints,
            Instant createdAt,
            Instant lastActiveAt
    ) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneHash = phoneHash;
        this.role = role;
        this.passHash = passHash;
        this.photoUrl = photoUrl;
        this.active = active;
        this.totalPoints = totalPoints;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
    }

    // выполняется перед первой вставкой в таблицу в случае, если reatedAt, lastActiveAt не заданы (у модераторов такое возможно, к примеру)
    @PrePersist
    private void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (lastActiveAt == null) {
            lastActiveAt = now;
        }
        if (totalPoints == null || totalPoints < 0) {
            totalPoints = 0;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneHash() {
        return phoneHash;
    }

    public void setPhoneHash(String phoneHash) {
        this.phoneHash = phoneHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPassHash() {
        return passHash;
    }

    public void setPassHash(String passHash) {
        this.passHash = passHash;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(Instant lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}