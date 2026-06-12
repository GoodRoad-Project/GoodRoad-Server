package goodroad.tasks.repository;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_task_completion")
public class UserTaskCompletionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist private void prePersist(){ if(createdAt==null) createdAt=Instant.now(); }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public Long getTaskId(){return taskId;} public void setTaskId(Long taskId){this.taskId=taskId;}
    public Integer getPointsAwarded(){return pointsAwarded;} public void setPointsAwarded(Integer pointsAwarded){this.pointsAwarded=pointsAwarded;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
}
