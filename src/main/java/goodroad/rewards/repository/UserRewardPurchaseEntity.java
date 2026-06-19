package goodroad.rewards.repository;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_reward_purchase")
public class UserRewardPurchaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "reward_offer_id", nullable = false)
    private Long rewardOfferId;
    @Column(name = "price", nullable = false)
    private Integer price;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist private void prePersist(){ if(createdAt==null) createdAt=Instant.now(); }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public Long getUserId(){return userId;} public void setUserId(Long userId){this.userId=userId;}
    public Long getRewardOfferId(){return rewardOfferId;} public void setRewardOfferId(Long rewardOfferId){this.rewardOfferId=rewardOfferId;}
    public Integer getPrice(){return price;} public void setPrice(Integer price){this.price=price;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
}
