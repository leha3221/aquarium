package ru.mirea.aquarium.backend.team.Skvorcsov;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import ru.mirea.aquarium.backend.team.Konstantinov.domain.Client;
import ru.mirea.aquarium.backend.team.Konstantinov.repository.ClientRepository;
import ru.mirea.aquarium.backend.team.Maslov.domain.*;
import ru.mirea.aquarium.backend.team.Maslov.repository.AquariumRepository;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.service.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("fish-test")
class FishModuleTest {
    @Autowired FishService service;
    @Autowired FishSpeciesService speciesService;
    @Autowired FishNoteService notes;
    @Autowired FishBulkService bulk;
    @Autowired FishCsvService csv;
    @Autowired FishSummaryService summaries;
    @Autowired ClientRepository clients;
    @Autowired AquariumRepository aquariums;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @org.springframework.beans.factory.annotation.Value("${app.fish.permanent-delete-enabled:false}")
    boolean permanentDeleteEnabled;
    UUID aquariumId, speciesId;

    @BeforeEach
    void fixtures() throws Exception {
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            if (connection.getMetaData().getDatabaseProductName().equals("PostgreSQL")
                && !java.net.URI.create(connection.getMetaData().getURL().substring(5)).getPath().equals("/fish_module_test"))
                throw new IllegalStateException("Tests may clear only the dedicated fish_module_test database");
        }
        for (String table : List.of("fish_import_batches", "fish_notes", "fish_tags", "fish", "fish_species",
            "service_requests", "aquariums", "clients")) jdbc.update("delete from " + table);
        Client client = new Client();
        client.setFullName("Тестовый клиент"); client.setPhone("+79990000000");
        client = clients.saveAndFlush(client);
        Aquarium a = new Aquarium();
        a.setCode("AQ-1"); a.setClientId(client.getId()); a.setName("Домашний");
        a.setType(AquariumType.FRESHWATER); a.setVolumeLiters(new BigDecimal("100.00")); a.setActive(true);
        aquariumId = aquariums.saveAndFlush(a).getId();
        speciesId = speciesService.create(new SpeciesWrite("NEON", "Неон", "", true), "admin").id();
    }

    FishWrite data(String code, int quantity, FishStatus status, boolean observation, Set<String> tags) {
        return new FishWrite(code, aquariumId, speciesId, "Группа " + code, "Описание", quantity, status, observation, tags);
    }
    FishFilter filter(String q, CarePriority priority, String tag, FishFilter.ArchiveMode archive,
        FishFilter.QuantityOp op, Integer quantity) {
        return new FishFilter(q, null, null, null, null, priority, tag, archive, op, quantity);
    }
    FishFilter all() { return filter(null, null, null, FishFilter.ArchiveMode.ALL, null, null); }

    @Test void contextAndPriorityTruthTable() {
        assertThat(service).isNotNull();
        for (FishStatus status : FishStatus.values()) {
            for (boolean observe : List.of(false, true)) {
                CarePriority expected = status == FishStatus.TREATMENT ? CarePriority.HIGH
                    : status != FishStatus.NORMAL || observe ? CarePriority.MEDIUM : CarePriority.LOW;
                service.create(data(status + "-" + observe, 1, status, observe, Set.of()), "editor");
                assertThat(CarePriority.calculate(status, observe)).isEqualTo(expected);
            }
        }
        for (CarePriority priority : CarePriority.values()) {
            var page = service.list(filter(null, priority, null, null, null, null), 0, 100, "code", "asc");
            assertThat(page.getContent()).allSatisfy(f -> assertThat(f.carePriority()).isEqualTo(priority));
            assertThat(page.getTotalElements()).isEqualTo(priority == CarePriority.LOW ? 1 : priority == CarePriority.HIGH ? 2 : 5);
        }
    }

    @Test void rejectsReptileInactiveAquariumAndInvalidQuantity() {
        Aquarium a = aquariums.findById(aquariumId).orElseThrow();
        a.setType(AquariumType.REPTILE); aquariums.saveAndFlush(a);
        assertThatThrownBy(() -> service.create(data("BAD", 1, FishStatus.NORMAL, false, Set.of()), "editor"))
            .isInstanceOf(FishFailure.class).hasMessageContaining("рептилий");
        assertThatThrownBy(() -> service.create(data("BAD", 0, FishStatus.NORMAL, false, Set.of()), "editor"))
            .isInstanceOf(FishFailure.class);
        a = aquariums.findById(aquariumId).orElseThrow();
        a.setType(AquariumType.FRESHWATER); a.setActive(false); aquariums.saveAndFlush(a);
        assertThatThrownBy(() -> service.create(data("BAD", 1, FishStatus.NORMAL, false, Set.of()), "editor"))
            .hasMessageContaining("активным");
    }

    @Test void searchesCombinesFiltersAndDoesNotDuplicateTagPages() {
        service.create(data("ONE", 10, FishStatus.QUARANTINE, true, Set.of(" Новые ", "Стaя")), "editor");
        service.create(data("TWO", 20, FishStatus.NORMAL, false, Set.of("новые")), "editor");
        service.create(data("THREE", 5, FishStatus.NORMAL, false, Set.of()), "editor");
        FishFilter f = new FishFilter("неОн", aquariumId, speciesId, FishStatus.QUARANTINE, true,
            CarePriority.MEDIUM, "НОВЫЕ", null, FishFilter.QuantityOp.GTE, 10);
        var page = service.list(f, 0, 1, "quantity", "desc");
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().code()).isEqualTo("ONE");
        assertThat(service.list(filter("%", null, null, null, null, null), 0, 20, "code", "asc").getTotalElements()).isZero();
        assertThat(service.list(filter("_", null, null, null, null, null), 0, 20, "code", "asc").getTotalElements()).isZero();
        assertThatThrownBy(() -> service.list(all(), 0, 101, "code", "asc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(all(), 0, 20, "code; drop table fish", "asc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.list(filter(null, null, null, null, FishFilter.QuantityOp.GT, null),
            0, 20, "code", "asc")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void updatesUseVersionAndArchiveCanBeRestored() {
        FishResponse f = service.create(data("F1", 10, FishStatus.NORMAL, false, Set.of()), "editor");
        FishResponse updated = service.update(f.id(), new FishUpdate(data("F1", 12, FishStatus.TREATMENT, true, Set.of("тест")), f.version()), "editor");
        assertThat(updated.version()).isGreaterThan(f.version());
        assertThatThrownBy(() -> service.update(f.id(), new FishUpdate(data("F1", 99, FishStatus.NORMAL, false, Set.of()), f.version()), "editor"))
            .isInstanceOf(OptimisticLockException.class);
        assertThatThrownBy(() -> service.archive(f.id(), f.version(), true, "editor")).isInstanceOf(OptimisticLockException.class);
        var archived = service.archive(f.id(), updated.version(), true, "editor");
        assertThat(service.list(filter(null, null, null, null, null, null), 0, 20, "code", "asc").getTotalElements()).isZero();
        assertThat(service.archive(f.id(), archived.version(), false, "editor").archived()).isFalse();
        assertThatThrownBy(() -> service.create(data("f1", 1, FishStatus.NORMAL, false, Set.of()), "editor"))
            .isInstanceOf(FishFailure.class).hasMessageContaining("занят");
    }

    @Test void disabledSpeciesRetainsExistingGroupsButRejectsNewAndRestore() {
        var f = service.create(data("EXISTING", 10, FishStatus.NORMAL, false, Set.of()), "editor");
        var s = speciesService.list(null, 0, 20).getContent().getFirst();
        speciesService.update(s.id(), new SpeciesUpdate(new SpeciesWrite(s.code(), s.name(), "", false), s.version()), "admin");
        assertThatThrownBy(() -> service.create(data("NEW", 10, FishStatus.NORMAL, false, Set.of()), "editor"))
            .isInstanceOf(FishFailure.class).hasMessageContaining("отключен");
        var changed = service.update(f.id(), new FishUpdate(data("EXISTING", 11, FishStatus.NORMAL, false, Set.of()), f.version()), "editor");
        var archived = service.archive(f.id(), changed.version(), true, "editor");
        assertThatThrownBy(() -> service.archive(f.id(), archived.version(), false, "editor")).hasMessageContaining("отключен");
    }

    @Test void bulkCommitsSuccessfulItemsAndReportsStaleMissingAndDuplicate() {
        var f = service.create(data("ONE", 10, FishStatus.NORMAL, false, Set.of()), "editor");
        var stale = service.create(data("TWO", 10, FishStatus.NORMAL, false, Set.of()), "editor");
        service.update(stale.id(), new FishUpdate(data("TWO", 11, FishStatus.NORMAL, false, Set.of()), stale.version()), "editor");
        var result = bulk.apply(new BulkRequest(List.of(new BulkItem(stale.id(), stale.version()),
            new BulkItem(f.id(), f.version()), new BulkItem(UUID.randomUUID(), 0L),
            new BulkItem(f.id(), f.version())), BulkAction.ADD_TAG, null, "Стая"), "editor");
        assertThat(result.successes()).hasSize(1);
        assertThat(result.errors()).extracting(ItemError::status).containsExactly(409, 404, 400);
        assertThat(service.get(f.id()).tags()).containsExactly("стая");
        assertThat(service.get(f.id()).version()).isGreaterThan(f.version());
    }

    @Test void notesKeepAuthorAndCannotCrossFishBoundary() {
        var one = service.create(data("ONE", 1, FishStatus.NORMAL, false, Set.of()), "editor");
        var two = service.create(data("TWO", 1, FishStatus.NORMAL, false, Set.of()), "editor");
        var n = notes.create(one.id(), new NoteWrite("Осмотр", true), "alice");
        var changed = notes.update(one.id(), n.id(), new NoteUpdate(new NoteWrite("Повторный осмотр", false), n.version()), "bob");
        assertThat(changed.author()).isEqualTo("alice");
        assertThat(changed.updatedAt()).isAfterOrEqualTo(n.createdAt());
        assertThatThrownBy(() -> notes.delete(two.id(), n.id(), changed.version(), "bob"))
            .isInstanceOf(jakarta.persistence.EntityNotFoundException.class);
        assertThatThrownBy(() -> notes.delete(one.id(), n.id(), n.version(), "bob")).isInstanceOf(OptimisticLockException.class);
        notes.delete(one.id(), n.id(), changed.version(), "bob");
        assertThat(notes.list(one.id(), 0, 20).getTotalElements()).isZero();
    }

    @Test void summaryDistinguishesGroupsAndIndividuals() {
        var one = service.create(data("ONE", 10, FishStatus.TREATMENT, true, Set.of()), "editor");
        var two = service.create(data("TWO", 20, FishStatus.NORMAL, false, Set.of()), "editor");
        service.archive(two.id(), two.version(), true, "editor");
        var result = summaries.summary(filter(null, null, null, null, null, null));
        assertThat(result.total()).isEqualTo(2);
        assertThat(result.active()).isEqualTo(1);
        assertThat(result.archived()).isEqualTo(1);
        assertThat(result.requiresObservation()).isEqualTo(1);
        assertThat(result.individuals()).isEqualTo(30);
        assertThat(result.averageQuantity()).isEqualTo(15);
        assertThat(result.bySpecies()).singleElement().satisfies(g -> assertThat(g.individuals()).isEqualTo(30));
        assertThat(result.byCarePriority()).hasSize(2);
        assertThat(result.byStatus()).hasSize(2);
    }

    @Test void csvPreviewReportsRowsAndDoesNotWriteFish() {
        String header = "code,aquariumCode,speciesCode,name,description,quantity,status,requiresObservation,tags\r\n";
        var preview = csv.preview(upload(header
            + "OK,AQ-1,NEON,Группа,,10,NORMAL,false,стая\r\n"
            + "BAD,AQ-1,NEON,Ошибка,,0,NORMAL,false,\r\n"
            + "OK,AQ-1,NEON,Повтор,,1,NORMAL,false,\r\n"), ImportMode.NEW_ONLY, "editor");
        assertThat(preview.validRows()).isEqualTo(1);
        assertThat(preview.invalidRows()).isEqualTo(2);
        assertThat(preview.batchId()).isNull();
        assertThat(service.list(all(), 0, 20, "code", "asc").getTotalElements()).isZero();
    }

    @Test void csvApplyIsOwnedSingleUseAndUpdatesByCode() {
        String header = "code,aquariumCode,speciesCode,name,description,quantity,status,requiresObservation,tags\r\n";
        var preview = csv.preview(upload(header + "F1,AQ-1,NEON,Группа,,10,NORMAL,false,стая\r\n"), ImportMode.NEW_ONLY, "alice");
        assertThatThrownBy(() -> csv.apply(preview.batchId(), "bob")).isInstanceOf(FishFailure.class).hasMessageContaining("другому");
        assertThat(csv.apply(preview.batchId(), "alice").created()).isEqualTo(1);
        assertThatThrownBy(() -> csv.apply(preview.batchId(), "alice")).hasMessageContaining("применена");
        var duplicate = csv.preview(upload(header + "F1,AQ-1,NEON,Группа,,20,NORMAL,false,\r\n"), ImportMode.NEW_ONLY, "alice");
        assertThat(duplicate.invalidRows()).isEqualTo(1);
        var upsert = csv.preview(upload(header + "F1,AQ-1,NEON,Группа,,20,NORMAL,false,\r\n"), ImportMode.UPSERT, "alice");
        assertThat(csv.apply(upsert.batchId(), "alice").updated()).isEqualTo(1);
        assertThat(service.list(all(), 0, 20, "code", "asc").getContent().getFirst().quantity()).isEqualTo(20);
    }

    @Test void csvStalePlanRollsBackAllRowsAndCanBeRevalidated() {
        var f = service.create(data("F1", 10, FishStatus.NORMAL, false, Set.of()), "editor");
        String file = "code,aquariumCode,speciesCode,name,description,quantity,status,requiresObservation,tags\n"
            + "NEW,AQ-1,NEON,Новая,,1,NORMAL,false,\n"
            + "F1,AQ-1,NEON,Изменение,,20,NORMAL,false,\n";
        var preview = csv.preview(upload(file), ImportMode.UPSERT, "editor");
        service.update(f.id(), new FishUpdate(data("F1", 11, FishStatus.NORMAL, false, Set.of()), f.version()), "editor");
        assertThatThrownBy(() -> csv.apply(preview.batchId(), "editor")).isInstanceOf(OptimisticLockException.class);
        assertThat(service.list(all(), 0, 20, "code", "asc").getTotalElements()).isEqualTo(1);
        assertThat(service.get(f.id()).quantity()).isEqualTo(11);
    }

    @Test void csvRoundTripsQuotedRussianTextAndFormulaEscapingAndUsesFilter() {
        Aquarium aquarium = aquariums.findById(aquariumId).orElseThrow();
        aquarium.setCode("=sum(1,2)");
        aquariums.saveAndFlush(aquarium);
        FishWrite d = new FishWrite("F1", aquariumId, speciesId, "=SUM(1,2)", "Описание \"рыб\",\nновая строка",
            10, FishStatus.NORMAL, false, Set.of("стая"));
        service.create(d, "editor");
        service.create(data("OTHER", 1, FishStatus.NORMAL, false, Set.of()), "editor");
        byte[] exported = csv.export(filter("F1", null, null, null, null, null), "code", "asc");
        String text = new String(exported, StandardCharsets.UTF_8);
        assertThat(text).contains("'=SUM", "'=sum").doesNotContain("OTHER");
        var preview = csv.preview(new MockMultipartFile("file", "fish.csv", "text/csv", exported), ImportMode.UPSERT, "editor");
        assertThat(preview.invalidRows()).isZero();
        csv.apply(preview.batchId(), "editor");
        assertThat(service.list(filter("F1", null, null, null, null, null), 0, 20, "code", "asc")
            .getContent().getFirst().name()).isEqualTo(d.name());
    }

    @Test void httpRequiresRealPrincipalAndRolesAndReturnsUniformErrors() throws Exception {
        mvc.perform(get("/api/fish")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        mvc.perform(post("/api/fish").with(actor("VIEWER")).contentType("application/json")
            .content(mapper.writeValueAsBytes(data("F1", 1, FishStatus.NORMAL, false, Set.of()))))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/fish-species").with(actor("EDITOR")).contentType("application/json")
            .content(mapper.writeValueAsBytes(new SpeciesWrite("TEST", "Тест", "", true))))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/fish").with(actor("EDITOR")).contentType("application/json")
            .content(mapper.writeValueAsBytes(data("F1", 1, FishStatus.NORMAL, false, Set.of()))))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.carePriority").value("LOW"));
        mvc.perform(get("/api/fish").with(actor("VIEWER")).param("quantityOp", "GTE").param("quantity", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/fish").with(actor("VIEWER")).param("archive", "WRONG"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400)).andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(get("/api/fish/not-a-uuid").with(actor("VIEWER"))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/fish").with(actor("EDITOR")).contentType("application/json").content("{"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/fish/" + UUID.randomUUID()).with(actor("VIEWER"))).andExpect(status().isNotFound());
    }

    @Test void deletionRequiresAdminArchiveAndEnabledFlag() throws Exception {
        var f = service.create(data("F1", 1, FishStatus.NORMAL, false, Set.of()), "editor");
        notes.create(f.id(), new NoteWrite("Заметка", true), "editor");
        mvc.perform(delete("/api/fish/" + f.id() + "/permanent").with(actor("EDITOR"))
            .param("version", Long.toString(f.version()))).andExpect(status().isForbidden());
        mvc.perform(delete("/api/fish/" + f.id() + "/permanent").with(actor("ADMIN"))
            .param("version", Long.toString(f.version())))
            .andExpect(status().is(permanentDeleteEnabled ? 409 : 403));
        var archived = service.archive(f.id(), f.version(), true, "editor");
        mvc.perform(delete("/api/fish/" + f.id() + "/permanent").with(actor("ADMIN"))
            .param("version", Long.toString(archived.version())))
            .andExpect(status().is(permanentDeleteEnabled ? 204 : 403));
        assertThat(jdbc.queryForObject("select count(*) from fish", Integer.class)).isEqualTo(permanentDeleteEnabled ? 0 : 1);
        assertThat(jdbc.queryForObject("select count(*) from fish_notes", Integer.class)).isEqualTo(permanentDeleteEnabled ? 0 : 1);
    }

    @Test void httpErrorsCoverValidationVersionAndProtocol() throws Exception {
        var f = service.create(data("F1", 1, FishStatus.NORMAL, false, Set.of()), "editor");
        var changed = service.update(f.id(), new FishUpdate(data("F1", 2, FishStatus.NORMAL, false, Set.of()), f.version()), "editor");
        mvc.perform(put("/api/fish/" + f.id()).with(actor("EDITOR")).contentType("application/json")
            .content(mapper.writeValueAsBytes(new FishUpdate(data("F1", 3, FishStatus.NORMAL, false, Set.of()), f.version()))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.trace").doesNotExist());
        mvc.perform(post("/api/fish").with(actor("EDITOR")).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/fish/" + f.id()).with(actor("EDITOR")).param("version", "-1"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/fish").with(actor("VIEWER")).param("quantity", "oops"))
            .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/fish/" + f.id()).with(actor("EDITOR")))
            .andExpect(status().isMethodNotAllowed());
        mvc.perform(post("/api/fish").with(actor("EDITOR")).contentType("text/plain").content("test"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test void csvRejectsExpiredPlanMalformedFileAndChangedReferences() {
        String content = "code,aquariumCode,speciesCode,name,description,quantity,status,requiresObservation,tags\n"
            + "F1,AQ-1,NEON,Группа,,1,NORMAL,false,\n";
        var expired = csv.preview(upload(content), ImportMode.NEW_ONLY, "editor");
        jdbc.update("update fish_import_batches set expires_at = ? where id = ?",
            java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)), expired.batchId());
        assertThatThrownBy(() -> csv.apply(expired.batchId(), "editor")).hasMessageContaining("истекла");
        assertThatThrownBy(() -> csv.preview(upload("wrong,headers\n1,2"), ImportMode.NEW_ONLY, "editor"))
            .isInstanceOf(FishFailure.class);
        var preview = csv.preview(upload(content), ImportMode.NEW_ONLY, "editor");
        var s = speciesService.list(null, 0, 20).getContent().getFirst();
        speciesService.update(s.id(), new SpeciesUpdate(new SpeciesWrite(s.code(), s.name(), "", false), s.version()), "admin");
        assertThatThrownBy(() -> csv.apply(preview.batchId(), "editor")).hasMessageContaining("отключен");
        assertThat(service.list(all(), 0, 20, "code", "asc").getTotalElements()).isZero();
    }

    @Test void postgresMigrationsAndParentProtectionAreEffective() throws Exception {
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            Assumptions.assumeTrue(connection.getMetaData().getDatabaseProductName().equals("PostgreSQL"));
        }
        assertThat(jdbc.queryForObject("select count(*) from flyway_schema_history where success", Integer.class)).isEqualTo(3);
        var f = service.create(data("F1", 1, FishStatus.NORMAL, false, Set.of()), "editor");
        assertThatThrownBy(() -> jdbc.update("update aquariums set type = 'REPTILE' where id = ?", aquariumId))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("update aquariums set archived = true where id = ?", aquariumId))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("delete from aquariums where id = ?", aquariumId))
            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        service.archive(f.id(), f.version(), true, "editor");
        jdbc.update("update aquariums set archived = true where id = ?", aquariumId);
    }

    private MockMultipartFile upload(String content) {
        return new MockMultipartFile("file", "fish.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }
    private org.springframework.test.web.servlet.request.RequestPostProcessor actor(String role) {
        return request -> { request.setUserPrincipal(() -> "test-user"); request.addUserRole(role); return request; };
    }
}
