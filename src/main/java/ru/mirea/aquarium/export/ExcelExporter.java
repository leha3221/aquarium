package ru.mirea.aquarium.export;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.aquarium.model.ServiceRequest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Экспорт заявок в Excel (.xlsx) с помощью Apache POI.
 */
public class ExcelExporter implements Exporter {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final String[] HEADERS = {
            "ID", "Клиент", "Аквариум", "Услуга", "Описание",
            "Статус", "Приоритет", "Плановая дата", "Цена"
    };

    @Override
    public void export(List<ServiceRequest> requests, String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Заявки");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (ServiceRequest r : requests) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(
                        r.getClientName() != null ? r.getClientName() : String.valueOf(r.getClientId()));
                row.createCell(2).setCellValue(String.valueOf(r.getAquariumType()));
                row.createCell(3).setCellValue(String.valueOf(r.getServiceType()));
                row.createCell(4).setCellValue(r.getDescription());
                row.createCell(5).setCellValue(String.valueOf(r.getStatus()));
                row.createCell(6).setCellValue(r.getPriority());
                row.createCell(7).setCellValue(
                        r.getScheduledAt() != null ? r.getScheduledAt().format(DATE_FORMAT) : "");
                row.createCell(8).setCellValue(
                        r.getPrice() != null ? r.getPrice().doubleValue() : 0.0);
            }

            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                workbook.write(out);
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка экспорта в Excel: " + e.getMessage(), e);
        }
    }

    @Override
    public String fileExtension() {
        return "xlsx";
    }
}
