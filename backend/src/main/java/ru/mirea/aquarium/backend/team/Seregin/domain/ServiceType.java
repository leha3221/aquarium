package ru.mirea.aquarium.backend.team.Seregin.domain;
import com.fasterxml.jackson.annotation.JsonCreator;import com.fasterxml.jackson.annotation.JsonValue;import java.util.Arrays;
public enum ServiceType{
 CLEANING("Чистка аквариума"),WATER_CHANGE("Подмена воды"),EQUIPMENT_REPAIR("Ремонт оборудования"),FISH_HEALTH("Осмотр рыб"),AQUASCAPING("Оформление аквариума");
 private final String title;ServiceType(String t){title=t;}@JsonValue public String title(){return title;}
 @JsonCreator public static ServiceType parse(String s){return Arrays.stream(values()).filter(v->v.name().equalsIgnoreCase(s)||v.title.equalsIgnoreCase(s)).findFirst().orElseThrow(()->new IllegalArgumentException("Неизвестный вид услуги"));}
}
