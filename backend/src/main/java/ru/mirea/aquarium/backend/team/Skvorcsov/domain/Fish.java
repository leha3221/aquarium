package ru.mirea.aquarium.backend.team.Skvorcsov.domain;

import jakarta.persistence.*;
import ru.mirea.aquarium.backend.team.Maslov.domain.Aquarium;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "fish")
public class Fish extends TrackedEntity {
    @Column(name = "external_code", nullable = false, unique = true, length = 32)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aquarium_id", nullable = false)
    private Aquarium aquarium;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "species_id", nullable = false)
    private FishSpecies species;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 1000)
    private String description = "";
    @Column(nullable = false)
    private int quantity;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24)
    private FishStatus status = FishStatus.NORMAL;
    @Column(name = "requires_observation", nullable = false)
    private boolean requiresObservation;
    @Column(nullable = false)
    private boolean archived;
    @ElementCollection
    @CollectionTable(name = "fish_tags", joinColumns = @JoinColumn(name = "fish_id"))
    @Column(name = "tag", nullable = false, length = 40)
    private Set<String> tags = new LinkedHashSet<>();

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public Aquarium getAquarium() { return aquarium; }
    public void setAquarium(Aquarium value) { aquarium = value; }
    public FishSpecies getSpecies() { return species; }
    public void setSpecies(FishSpecies value) { species = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int value) { quantity = value; }
    public FishStatus getStatus() { return status; }
    public void setStatus(FishStatus value) { status = value; }
    public boolean isRequiresObservation() { return requiresObservation; }
    public void setRequiresObservation(boolean value) { requiresObservation = value; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean value) { archived = value; }
    public Set<String> getTags() { return tags; }
    public CarePriority getCarePriority() { return CarePriority.calculate(status, requiresObservation); }
}
