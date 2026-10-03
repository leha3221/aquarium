package ru.mirea.aquarium.model;
import java.math.BigDecimal;
public class Aquarium {
    private long id;
    private long clientId;
    private String name;
    private AquariumType type;
    private BigDecimal volumeLiters;
    public Aquarium(long id, long clientId, String name, AquariumType type, BigDecimal volumeLiters) {
        this.id=id;
        this.clientId=clientId;
        this.name=name;
        this.type=type;
        this.volumeLiters=volumeLiters;
    }
    public Aquarium(long clientId, String name, AquariumType type, BigDecimal volumeLiters){this(0, clientId, name, type, volumeLiters);}
    public long getId(){return id;}
    public void setId(long id){this.id=id;}
    public long getClientId(){return clientId;}
    public void setClientId(long value){this.clientId=value;}
    public String getName(){return name;}
    public void setName(String value){this.name=value;}
    public AquariumType getType(){return type;}
    public void setType(AquariumType value){this.type=value;}
    public BigDecimal getVolumeLiters(){return volumeLiters;}
    public void setVolumeLiters(BigDecimal value){this.volumeLiters=value;}
    @Override public String toString(){return "#" + id + " | " + clientId + " | " + name + " | " + type + " | " + volumeLiters;}
}