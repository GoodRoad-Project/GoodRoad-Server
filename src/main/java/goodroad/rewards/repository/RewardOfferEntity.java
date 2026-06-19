package goodroad.rewards.repository;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "reward_offer")
public class RewardOfferEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "partner_name", nullable = false, length = 180)
    private String partnerName;
    @Column(name = "title", nullable = false, length = 240)
    private String title;
    @Column(name = "description", length = 1000)
    private String description;
    @Column(name = "price", nullable = false)
    private Integer price;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @PrePersist private void prePersist(){ if(createdAt==null) createdAt=Instant.now(); }
    public Long getId(){return id;} public void setId(Long id){this.id=id;}
    public String getPartnerName(){return partnerName;} public void setPartnerName(String partnerName){this.partnerName=partnerName;}
    public String getTitle(){return title;} public void setTitle(String title){this.title=title;}
    public String getDescription(){return description;} public void setDescription(String description){this.description=description;}
    public Integer getPrice(){return price;} public void setPrice(Integer price){this.price=price;}
    public boolean isActive(){return active;} public void setActive(boolean active){this.active=active;}
    public Instant getCreatedAt(){return createdAt;} public void setCreatedAt(Instant createdAt){this.createdAt=createdAt;}
}
