package ru.mirea.aquarium.backend.team.Skvorcsov.service;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.aquarium.backend.team.Skvorcsov.domain.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.dto.FishDtos.*;
import ru.mirea.aquarium.backend.team.Skvorcsov.repository.FishNoteRepository;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FishNoteService {
    private final FishNoteRepository notes;
    private final FishService fish;
    private final FishEvents events;
    public FishNoteService(FishNoteRepository notes, FishService fish, FishEvents events) {
        this.notes = notes; this.fish = fish; this.events = events;
    }

    public Page<NoteResponse> list(UUID fishId, int page, int size) {
        fish.entity(fishId);
        FishService.pageable(page, size, "createdAt", "desc");
        return notes.findByFishId(fishId, PageRequest.of(page, size, Sort.by(
            Sort.Order.desc("pinned"), Sort.Order.desc("createdAt"), Sort.Order.asc("id")))).map(NoteResponse::from);
    }

    @Transactional
    public NoteResponse create(UUID fishId, NoteWrite data, String actor) {
        fish.validate(data);
        FishNote n = new FishNote();
        n.setFish(editable(fishId)); n.setAuthor(actor); apply(n, data);
        notes.saveAndFlush(n);
        events.record(actor, "CREATE", "FISH_NOTE", n.getId(), "Заметка к группе " + fishId);
        return NoteResponse.from(n);
    }

    @Transactional
    public NoteResponse update(UUID fishId, UUID id, NoteUpdate request, String actor) {
        fish.validate(request);
        editable(fishId);
        FishNote n = note(fishId, id);
        FishService.checkVersion(n, request.version());
        apply(n, request.data());
        n.touch();
        notes.flush();
        events.record(actor, "UPDATE", "FISH_NOTE", id, "Заметка к группе " + fishId);
        return NoteResponse.from(n);
    }

    @Transactional
    public void delete(UUID fishId, UUID id, long version, String actor) {
        editable(fishId);
        FishNote n = note(fishId, id);
        FishService.checkVersion(n, version);
        notes.delete(n);
        notes.flush();
        events.record(actor, "DELETE", "FISH_NOTE", id, "Заметка к группе " + fishId);
    }

    private FishNote note(UUID fishId, UUID id) {
        return notes.findByIdAndFishId(id, fishId).orElseThrow(() -> new EntityNotFoundException("Заметка не найдена"));
    }

    private Fish editable(UUID id) {
        Fish f = fish.entity(id);
        if (f.isArchived()) throw new FishFailure(409, "Заметки архивной группы доступны только для чтения");
        return f;
    }

    private void apply(FishNote n, NoteWrite data) { n.setText(data.text().strip()); n.setPinned(data.pinned()); }
}
