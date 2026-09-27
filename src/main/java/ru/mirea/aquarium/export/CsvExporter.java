package ru.mirea.aquarium.export;

import ru.mirea.aquarium.model.ServiceRequest;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт заявок в CSV (разделитель ";", кодировка UTF-8).
 */
public class CsvExporter implements Exporter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] HEADERS = {
            "ID", "Клиент", "Аквариум", "Услуга", "Описание",
            "Статус", "Приоритет", "Плановая дата", "Цена"
    };

    @Override
    public void export(List<ServiceRequest> requests, String filePath) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                Path.of(filePath), StandardCharsets.UTF_8)) {

            writer.write(String.join(";", HEADERS));
            writer.newLine();

            for (ServiceRequest r : requests) {
                writer.write(String.join(";",
                        String.valueOf(r.getId()),
                        escape(r.getClientName() != null ? r.getClientName() : String.valueOf(r.getClientId())),
                        String.valueOf(r.getAquariumType()),
                        String.valueOf(r.getServiceType()),
                        escape(r.getDescription()),
                        String.valueOf(r.getStatus()),
                        String.valueOf(r.getPriority()),
                        r.getScheduledAt() != null ? r.getScheduledAt().format(DATE_FORMAT) : "",
                        String.valueOf(r.getPrice())
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка экспорта в CSV: " + e.getMessage(), e);
        }
    }

    @Override
    public String fileExtension() {
        return "csv";
    }

    private String escape(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        return v.contains(";") || v.contains("\"") ? "\"" + v + "\"" : v;
    }
}
