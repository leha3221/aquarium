package ru.mirea.aquarium.backend.team.Seregin.domain;
import com.fasterxml.jackson.annotation.JsonCreator;import com.fasterxml.jackson.annotation.JsonValue;import java.util.Arrays;
public enum RequestStatus{
 NEW("Новая"),CONFIRMED("Подтверждена"),IN_PROGRESS("В работе"),COMPLETED("Выполнена"),CANCELLED("Отменена");
 private final String title;RequestStatus(String t){title=t;}@JsonValue public String title(){return title;}
 @JsonCreator public static RequestStatus parse(String s){return Arrays.stream(values()).filter(v->v.name().equalsIgnoreCase(s)||v.title.equalsIgnoreCase(s)).findFirst().orElseThrow(()->new IllegalArgumentException("Неизвестный статус заявки"));}
}
