package goodroad.tasks.repository;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_task_target_completion")
public class UserTaskTargetCompletionEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    @Column(name = "source_type", nullable = false, length = 64)
    private String sourceType;
    @Column(name = "source_id", nullable = false)
    private Long sourceId;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist private void prePersist(){ if(createdAt==null) createdAt=Instant.now(); }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public Long getTaskId(){return taskId;} public void setTaskId(Long taskId){this.taskId=taskId;}
    public Long getTargetId(){return targetId;} public void setTargetId(Long targetId){this.targetId=targetId;}
    public String getSourceType(){return sourceType;} public void setSourceType(String sourceType){this.sourceType=sourceType;}
    public Long getSourceId(){return sourceId;} public void setSourceId(Long sourceId){this.sourceId=sourceId;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
}
