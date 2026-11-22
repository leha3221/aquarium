package ru.mirea.aquarium.backend.team.Skvorcsov.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.repository.*;
import java.io.*;
import java.nio.charset.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class FishCsvService {
    private static final String[] HEADERS = {"code", "aquariumCode", "speciesCode", "name", "description",
        "quantity", "status", "requiresObservation", "tags"};
    private static final int MAX_ROWS = 2000;
    private static final long MAX_BYTES = 1048576;
    private final FishService service;
    private final FishRepository fish;
    private final FishAquariumRepository aquariums;
    private final FishSpeciesRepository species;
    private final FishImportBatchRepository batches;
    private final ObjectMapper mapper;
    private final FishEvents events;

    public FishCsvService(FishService service, FishRepository fish, FishAquariumRepository aquariums,
        FishSpeciesRepository species, FishImportBatchRepository batches, ObjectMapper mapper, FishEvents events) {
        this.service = service; this.fish = fish; this.aquariums = aquariums; this.species = species;
        this.batches = batches; this.mapper = mapper; this.events = events;
    }

    @Transactional(readOnly = true)
    public byte[] export(FishFilter filter, String sort, String direction) {
        service.validate(filter);
        var spec = FishSpecifications.matching(filter);
        Pageable paging = FishService.pageable(0, 100, sort, direction);
        try {
            StringWriter out = new StringWriter();
            // UTF-8 BOM supports opening Russian text in desktop spreadsheet programs.
            out.write('\uFEFF');
            try (CSVPrinter csv = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())) {
                Page<Fish> page;
                do {
                    page = fish.findAll(spec, paging);
                    for (Fish f : page) {
                        csv.printRecord(f.getCode(), safeCell(f.getAquarium().getCode()), f.getSpecies().getCode(),
                            safeCell(f.getName()), safeCell(f.getDescription()), f.getQuantity(), f.getStatus().name(),
                            f.isRequiresObservation(), safeCell(String.join("|", new TreeSet<>(f.getTags()))));
                    }
                    paging = page.nextPageable();
                } while (page.hasNext());
            }
            return out.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сформировать CSV", e);
        }
    }

    @Transactional
    public ImportPreview preview(MultipartFile file, ImportMode mode, String actor) {
        if (file.isEmpty() || file.getSize() > MAX_BYTES)
            throw new FishFailure(400, "Размер CSV должен быть от 1 байта до 1 МиБ");
        List<CsvError> errors = new ArrayList<>();
        List<ImportRow> plan = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int invalidRows = 0;
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
            String content = decoder.decode(java.nio.ByteBuffer.wrap(file.getBytes())).toString();
            if (content.startsWith("\uFEFF")) content = content.substring(1);
            CSVFormat format = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(false).setAllowMissingColumnNames(false).setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).get();
            try (CSVParser csv = format.parse(new StringReader(content))) {
                if (!csv.getHeaderNames().equals(List.of(HEADERS)))
                    throw new FishFailure(400, "Ожидаемые колонки CSV: " + String.join(",", HEADERS));
                for (CSVRecord row : csv) {
                    if (row.getRecordNumber() > MAX_ROWS) throw new FishFailure(400, "В CSV допускается не более 2000 записей");
                    long line = row.getRecordNumber() + 1;
                    try {
                        if (!row.isConsistent()) throw new FishFailure(400, "Неверное число колонок");
                        FishWrite data = parse(row);
                        service.validate(data);
                        String code = FishService.code(data.code());
                        if (!seen.add(code)) throw new FishFailure(400, "Повтор кода в файле: " + code);
                        Fish existing = fish.findByCode(code).orElse(null);
                        if (existing != null && mode == ImportMode.NEW_ONLY)
                            throw new FishFailure(409, "Код уже существует: " + code);
                        if (existing != null && existing.isArchived())
                            throw new FishFailure(409, "Группа в архиве: " + code);
                        service.validateWrite(data, existing);
                        plan.add(new ImportRow(line, existing == null ? null : existing.getId(),
                            existing == null ? null : existing.getVersion(), data));
                    } catch (FishFailure | IllegalArgumentException | jakarta.persistence.EntityNotFoundException e) {
                        invalidRows++;
                        errors.add(new CsvError(line, e.getMessage()));
                    }
                }
            }
        } catch (FishFailure e) {
            throw e;
        } catch (IOException | UncheckedIOException | IllegalArgumentException e) {
            throw new FishFailure(400, "Некорректный CSV: требуется UTF-8, запятые и корректные кавычки");
        }
        if (plan.isEmpty() && invalidRows == 0) throw new FishFailure(400, "CSV не содержит записей");
        if (invalidRows > 0) return new ImportPreview(null, plan.size(), invalidRows, null, errors);
        FishImportBatch batch = new FishImportBatch();
        batch.setOwner(actor); batch.setExpiresAt(Instant.now().plus(30, ChronoUnit.MINUTES));
        try {
            batch.setPlan(mapper.writeValueAsString(plan));
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось сохранить план импорта", e);
        }
        batches.saveAndFlush(batch);
        return new ImportPreview(batch.getId(), plan.size(), 0, batch.getExpiresAt(), List.of());
    }

    @Transactional
    public ImportResult apply(UUID batchId, String actor) {
        FishImportBatch batch = batches.lockById(batchId)
            .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Проверка CSV не найдена"));
        if (!batch.getOwner().equals(actor)) throw new FishFailure(403, "Проверка CSV принадлежит другому пользователю");
        if (batch.isConsumed() || !batch.getExpiresAt().isAfter(Instant.now()))
            throw new FishFailure(409, "Проверка CSV уже применена или истекла; проверьте файл заново");
        List<ImportRow> rows;
        try {
            rows = mapper.readValue(batch.getPlan(), new TypeReference<List<ImportRow>>() {});
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось прочитать план импорта", e);
        }
        int created = 0, updated = 0;
        // One transaction: a stale version or changed reference rolls back every row.
        for (ImportRow row : rows) {
            if (row.targetId() == null) {
                service.create(row.data(), actor); created++;
            } else {
                service.update(row.targetId(), new FishUpdate(row.data(), row.version()), actor); updated++;
            }
        }
        batch.setConsumed(true);
        batches.flush();
        events.record(actor, "IMPORT", "FISH_IMPORT", batchId, "Создано: " + created + ", обновлено: " + updated);
        return new ImportResult(created, updated);
    }

    private FishWrite parse(CSVRecord row) {
        // Aquarium codes belong to another module and may be case-sensitive or contain formulas.
        String aquariumCode = unsafeCell(row.get("aquariumCode"));
        String speciesCode = FishService.code(row.get("speciesCode"));
        var aquarium = aquariums.findByCode(aquariumCode)
            .orElseThrow(() -> new FishFailure(400, "Не найден аквариум: " + aquariumCode));
        var reference = species.findByCode(speciesCode)
            .orElseThrow(() -> new FishFailure(400, "Не найден вид рыб: " + speciesCode));
        String bool = row.get("requiresObservation");
        if (!bool.equals("true") && !bool.equals("false")) throw new FishFailure(400, "requiresObservation: true или false");
        String tagCell = unsafeCell(row.get("tags"));
        Set<String> tags = tagCell.isEmpty() ? Set.of() : new LinkedHashSet<>(List.of(tagCell.split("\\|", -1)));
        return new FishWrite(row.get("code"), aquarium.getId(), reference.getId(),
            unsafeCell(row.get("name")), unsafeCell(row.get("description")),
            Integer.valueOf(row.get("quantity")), FishStatus.valueOf(row.get("status")), Boolean.valueOf(bool), tags);
    }

    private static boolean dangerous(String value) {
        if (value.isEmpty()) return false;
        String stripped = value.stripLeading();
        return !stripped.isEmpty() && "=+-@".indexOf(stripped.charAt(0)) >= 0
            || value.charAt(0) == '\t' || value.charAt(0) == '\r' || value.charAt(0) == '\n';
    }
    private static String safeCell(String value) {
        return dangerous(value) || value.startsWith("'") ? "'" + value : value;
    }
    private static String unsafeCell(String value) {
        return value.startsWith("'") && (value.substring(1).startsWith("'") || dangerous(value.substring(1)))
            ? value.substring(1) : value;
    }
}
