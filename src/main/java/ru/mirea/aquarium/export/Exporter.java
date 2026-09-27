package ru.mirea.aquarium.export;

import ru.mirea.aquarium.model.ServiceRequest;

import java.util.List;

/**
 * Абстракция экспорта списка заявок в файл на диске.
 * Полиморфная точка расширения: переменная типа Exporter может
 * ссылаться на ExcelExporter или CsvExporter без изменения кода,
 * которое ей пользуется (Console UI).
 */
public interface Exporter {

    /**
     * Экспортирует заявки в файл по указанному пути.
     *
     * @param requests список заявок для экспорта
     * @param filePath путь к создаваемому файлу
     */
    void export(List<ServiceRequest> requests, String filePath);

    /**
     * Расширение файла, которое использует данная реализация (без точки),
     * например "xlsx" или "csv". Используется UI, чтобы подсказать имя файла.
     */
    String fileExtension();
}
