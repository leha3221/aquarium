package ru.mirea.aquarium.backend.team.Skvorcsov.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "fish_notes")
public class FishNote extends TrackedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fish_id", nullable = false)
    private Fish fish;
    @Column(nullable = false, length = 2000)
    private String text;
    @Column(nullable = false, length = 255, updatable = false)
    private String author;
    @Column(nullable = false)
    private boolean pinned;

    public Fish getFish() { return fish; }
    public void setFish(Fish value) { fish = value; }
    public String getText() { return text; }
    public void setText(String value) { text = value; }
    public String getAuthor() { return author; }
    public void setAuthor(String value) { author = value; }
    public boolean isPinned() { return pinned; }
    public void setPinned(boolean value) { pinned = value; }
}
