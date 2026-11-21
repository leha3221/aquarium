package ru.mirea.aquarium.backend.team.Seregin.domain;
import jakarta.persistence.*;import java.math.BigDecimal;import java.time.Instant;import java.time.LocalDateTime;import java.util.UUID;
@Entity @Table(name="service_requests",indexes={@Index(name="ix_requests_client",columnList="client_id"),@Index(name="ix_requests_status_created",columnList="status,created_at")})
public class ServiceRequest{
 @Id@GeneratedValue(strategy=GenerationType.UUID)private UUID id;
 @Column(name="external_code",nullable=false,unique=true,length=32)private String code;
 @Column(name="client_id",nullable=false)private UUID clientId;@Column(name="aquarium_id",nullable=false)private UUID aquariumId;
 @Enumerated(EnumType.STRING)@Column(name="aquarium_type",nullable=false,length=24)private AquariumType aquariumType;
 @Enumerated(EnumType.STRING)@Column(name="service_type",nullable=false,length=32)private ServiceType serviceType;
 @Column(nullable=false,length=1000)private String description;
 @Enumerated(EnumType.STRING)@Column(nullable=false,length=24)private RequestStatus status=RequestStatus.NEW;
 @Column(nullable=false)private int priority=3;@Column(name="scheduled_at")private LocalDateTime scheduledAt;
 @Column(nullable=false,precision=10,scale=2)private BigDecimal price=BigDecimal.ZERO;
 @Column(nullable=false)private boolean archived=false;
 @Column(name="created_at",nullable=false,updatable=false)private Instant createdAt;@Column(name="updated_at",nullable=false)private Instant updatedAt;
 @Version@Column(nullable=false)private long version;
 @PrePersist void created(){createdAt=updatedAt=Instant.now();}@PreUpdate void updated(){updatedAt=Instant.now();}
 public UUID getId(){return id;}public String getCode(){return code;}public void setCode(String v){code=v;}public UUID getClientId(){return clientId;}public void setClientId(UUID v){clientId=v;}public UUID getAquariumId(){return aquariumId;}public void setAquariumId(UUID v){aquariumId=v;}
 public AquariumType getAquariumType(){return aquariumType;}public void setAquariumType(AquariumType v){aquariumType=v;}public ServiceType getServiceType(){return serviceType;}public void setServiceType(ServiceType v){serviceType=v;}
 public String getDescription(){return description;}public void setDescription(String v){description=v;}public RequestStatus getStatus(){return status;}public void setStatus(RequestStatus v){status=v;}
 public int getPriority(){return priority;}public void setPriority(int v){priority=v;}public LocalDateTime getScheduledAt(){return scheduledAt;}public void setScheduledAt(LocalDateTime v){scheduledAt=v;}
 public BigDecimal getPrice(){return price;}public void setPrice(BigDecimal v){price=v;}public boolean isArchived(){return archived;}public void setArchived(boolean v){archived=v;}
 public Instant getCreatedAt(){return createdAt;}public Instant getUpdatedAt(){return updatedAt;}public long getVersion(){return version;}
}
