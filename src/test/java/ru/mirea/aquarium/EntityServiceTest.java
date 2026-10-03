package ru.mirea.aquarium;

import java.math.BigDecimal;
import java.util.*;
import ru.mirea.aquarium.exception.*;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.repository.*;
import ru.mirea.aquarium.service.*;

/** Run with java -ea. Uses in-memory repositories; no database is required. */
public class EntityServiceTest {
    static class Clients extends ClientRepository {
        @Override public Optional<Client> findById(long id) {
            return id == 1 ? Optional.of(new Client(1, "Test", "+79991234567", "a@b.ru", "Address")) : Optional.empty();
        }
    }
    static class Aquariums extends AquariumRepository {
        final Map<Long, Aquarium> data = new LinkedHashMap<>();
        long next = 1;
        @Override public Aquarium create(Aquarium e) { e.setId(next++); data.put(e.getId(), e); return e; }
        @Override public List<Aquarium> findAll() { return new ArrayList<>(data.values()); }
        @Override public Optional<Aquarium> findById(long id) { return Optional.ofNullable(data.get(id)); }
        @Override public void update(Aquarium e) { data.put(e.getId(), e); }
        @Override public void delete(long id) { data.remove(id); }
    }
    static class Fishes extends FishRepository {
        final Map<Long, Fish> data = new LinkedHashMap<>();
        long next = 1;
        @Override public Fish create(Fish e) { e.setId(next++); data.put(e.getId(), e); return e; }
        @Override public List<Fish> findAll() { return new ArrayList<>(data.values()); }
        @Override public Optional<Fish> findById(long id) { return Optional.ofNullable(data.get(id)); }
        @Override public void update(Fish e) { data.put(e.getId(), e); }
        @Override public void delete(long id) { data.remove(id); }
    }
    static void fails(Class<? extends RuntimeException> type, Runnable action) {
        try { action.run(); } catch (RuntimeException e) {
            if (type.isInstance(e)) return;
            throw new AssertionError("Unexpected exception", e);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    static Aquarium aquarium(long id, long owner, String name, AquariumType type, String volume) {
        return new Aquarium(id, owner, name, type, new BigDecimal(volume));
    }
    public static void main(String[] args) {
        Aquariums ar = new Aquariums();
        Fishes fr = new Fishes();
        AquariumService as = new AquariumService(ar, new Clients(), fr);
        FishService fs = new FishService(fr, ar);
        Aquarium a = as.create(aquarium(0, 1, "Home", AquariumType.FRESHWATER, "200"));
        Aquarium b = as.create(aquarium(0, 1, "Reef", AquariumType.MARINE, "100"));
        assert as.get(a.getId()) == a;
        assert as.findByClient(1).size() == 2;
        assert as.searchByName("HOME").size() == 1;
        assert as.filterByType(AquariumType.MARINE).size() == 1;
        assert as.sortByVolume(true).get(0) == b;
        assert as.sortByVolume(false).get(0) == a;
        fails(EntityNotFoundException.class, () -> as.create(aquarium(0, 99, "Bad", AquariumType.FRESHWATER, "20")));
        fails(BusinessException.class, () -> as.create(aquarium(0, 1, "", AquariumType.FRESHWATER, "20")));
        for (String volume : List.of("0", "-1", "1.001", "100000000")) {
            fails(BusinessException.class, () -> as.create(aquarium(0, 1, "Bad", AquariumType.FRESHWATER, volume)));
        }
        Fish f = fs.create(new Fish(a.getId(), "Guppy", 10));
        Fish g = fs.create(new Fish(b.getId(), "Clownfish", 2));
        assert fs.get(f.getId()) == f;
        assert fs.searchBySpecies("GUP").size() == 1;
        assert fs.findByAquarium(a.getId()).size() == 1;
        assert fs.sortByQuantity(true).get(0) == g;
        assert fs.sortByQuantity(false).get(0) == f;
        fails(EntityNotFoundException.class, () -> fs.create(new Fish(999, "Guppy", 1)));
        fails(BusinessException.class, () -> fs.create(new Fish(a.getId(), "", 1)));
        fails(BusinessException.class, () -> fs.create(new Fish(a.getId(), "Guppy", 0)));
        fails(BusinessException.class, () -> as.update(aquarium(a.getId(), 1, "Home", AquariumType.REPTILE, "200")));
        Aquarium reptile = as.create(aquarium(0, 1, "Reptile", AquariumType.REPTILE, "50"));
        fails(BusinessException.class, () -> fs.create(new Fish(reptile.getId(), "Guppy", 1)));
        fs.update(new Fish(f.getId(), b.getId(), "Guppy", 12));
        assert fs.get(f.getId()).getAquariumId() == b.getId();
        assert fs.findByAquarium(a.getId()).isEmpty();
        as.update(aquarium(a.getId(), 1, "New name", AquariumType.REPTILE, "250"));
        assert as.get(a.getId()).getName().equals("New name");
        fs.delete(f.getId());
        fails(EntityNotFoundException.class, () -> fs.get(f.getId()));
        as.delete(a.getId());
        fails(EntityNotFoundException.class, () -> as.get(a.getId()));
        System.out.println("EntityServiceTest: all checks passed");
    }
}