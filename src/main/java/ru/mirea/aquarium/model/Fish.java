package ru.mirea.aquarium.model;
import java.math.BigDecimal;
public class Fish {
    private long id;
    private long aquariumId;
    private String species;
    private int quantity;
    public Fish(long id, long aquariumId, String species, int quantity) {
        this.id=id;
        this.aquariumId=aquariumId;
        this.species=species;
        this.quantity=quantity;
    }
    public Fish(long aquariumId, String species, int quantity){this(0, aquariumId, species, quantity);}
    public long getId(){return id;}
    public void setId(long id){this.id=id;}
    public long getAquariumId(){return aquariumId;}
    public void setAquariumId(long value){this.aquariumId=value;}
    public String getSpecies(){return species;}
    public void setSpecies(String value){this.species=value;}
    public int getQuantity(){return quantity;}
    public void setQuantity(int value){this.quantity=value;}
    @Override public String toString(){return "#" + id + " | " + aquariumId + " | " + species + " | " + quantity;}
}