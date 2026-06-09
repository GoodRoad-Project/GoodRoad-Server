package goodroad.volunteer.repository;

import goodroad.users.repository.UserEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "volunteer_application")
public class VolunteerApplicationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    private UserEntity applicant;

    @Column(name = "dobro_url", nullable = false, length = 512)
    private String dobroUrl;

    @Column(name = "phone", nullable = false, length = 32)
    private String phone;

    @Column(name = "social_nickname", length = 120)
    private String socialNickname;

    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "moderator_comment", length = 1000)
    private String moderatorComment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private UserEntity moderator;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "moderated_at")
    private Instant moderatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<VolunteerApplicationPhotoEntity> photos = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UserEntity getApplicant() { return applicant; }
    public void setApplicant(UserEntity applicant) { this.applicant = applicant; }
    public String getDobroUrl() { return dobroUrl; }
    public void setDobroUrl(String dobroUrl) { this.dobroUrl = dobroUrl; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSocialNickname() { return socialNickname; }
    public void setSocialNickname(String socialNickname) { this.socialNickname = socialNickname; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getModeratorComment() { return moderatorComment; }
    public void setModeratorComment(String moderatorComment) { this.moderatorComment = moderatorComment; }
    public UserEntity getModerator() { return moderator; }
    public void setModerator(UserEntity moderator) { this.moderator = moderator; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getModeratedAt() { return moderatedAt; }
    public void setModeratedAt(Instant moderatedAt) { this.moderatedAt = moderatedAt; }
    public List<VolunteerApplicationPhotoEntity> getPhotos() { return photos; }
    public void setPhotos(List<VolunteerApplicationPhotoEntity> photos) { this.photos = photos; }
}
