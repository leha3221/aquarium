package ru.mirea.aquarium.backend.team.Skvorcsov.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "fish_species")
public class FishSpecies extends TrackedEntity {
    @Column(nullable = false, unique = true, length = 32)
    private String code;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 1000)
    private String description = "";
    @Column(nullable = false)
    private boolean enabled = true;

    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean value) { enabled = value; }
}
