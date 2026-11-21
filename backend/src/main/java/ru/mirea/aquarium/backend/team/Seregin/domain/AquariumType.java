package ru.mirea.aquarium.backend.team.Seregin.domain;
import com.fasterxml.jackson.annotation.JsonCreator;import com.fasterxml.jackson.annotation.JsonValue;import java.util.Arrays;
public enum AquariumType{
 FRESHWATER("Пресноводный"),MARINE("Морской"),REPTILE("Для рептилий"),PLANT("Растительный");
 private final String title;AquariumType(String t){title=t;}@JsonValue public String title(){return title;}
 @JsonCreator public static AquariumType parse(String s){return Arrays.stream(values()).filter(v->v.name().equalsIgnoreCase(s)||v.title.equalsIgnoreCase(s)).findFirst().orElseThrow(()->new IllegalArgumentException("Неизвестный тип аквариума"));}
}
