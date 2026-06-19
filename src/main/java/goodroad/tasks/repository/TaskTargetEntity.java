package goodroad.tasks.repository;

import jakarta.persistence.*;

@Entity
@Table(name = "task_target")
public class TaskTargetEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    @Column(name = "target_type", nullable = false, length = 32)
    private String targetType;
    @Column(name = "target_id", nullable = false)
    private Long targetId;
    @Column(name = "title", nullable = false, length = 500)
    private String title;
    @Column(name = "latitude")
    private Double latitude;
    @Column(name = "longitude")
    private Double longitude;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
    @Column(name = "status", nullable = false, length = 16)
    private String status = "ACTIVE";
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getTaskId(){return taskId;} public void setTaskId(Long taskId){this.taskId=taskId;}
    public String getTargetType(){return targetType;} public void setTargetType(String targetType){this.targetType=targetType;}
    public Long getTargetId(){return targetId;} public void setTargetId(Long targetId){this.targetId=targetId;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public Double getLatitude(){return latitude;} public void setLatitude(Double latitude){this.latitude=latitude;}
    public Double getLongitude(){return longitude;} public void setLongitude(Double longitude){this.longitude=longitude;}
    public Integer getSortOrder(){return sortOrder;} public void setSortOrder(Integer sortOrder){this.sortOrder=sortOrder;}
    public String getStatus(){return status;} public void setStatus(String status){this.status=status;}
}
