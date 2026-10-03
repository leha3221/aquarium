package ru.mirea.aquarium.service;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.repository.*;
import ru.mirea.aquarium.exception.*;
import java.util.List;
public class FishService {
    private final FishRepository repository;
    private final AquariumRepository parentRepository;
    public FishService(FishRepository repository,AquariumRepository parentRepository){
        this.repository=repository;this.parentRepository=parentRepository;
    }
    public Fish create(Fish e){validate(e);return repository.create(e);}
    public List<Fish> findAll(){return repository.findAll();}
    public Fish get(long id){return repository.findById(id).orElseThrow(()->new EntityNotFoundException("Fish с ID "+id+" не найден."));}
    public void update(Fish e){get(e.getId());validate(e);repository.update(e);}
    public void delete(long id){get(id);repository.delete(id);}
    public List<Fish> findByAquarium(long id){
        parentRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Aquarium с ID "+id+" не найден."));
        return findAll().stream().filter(e -> e.getAquariumId()==id).toList();
    }
    public List<Fish> searchBySpecies(String text) {
        String q=text.toLowerCase(java.util.Locale.ROOT);
        return findAll().stream().filter(e -> e.getSpecies().toLowerCase(java.util.Locale.ROOT).contains(q)).toList();
    }
    public List<Fish> sortByQuantity(boolean asc) {
        java.util.Comparator<Fish> c=java.util.Comparator.comparingInt(Fish::getQuantity);
        return findAll().stream().sorted(asc ? c : c.reversed()).toList();
    }
    public List<Fish> sortBySpecies(boolean asc) {
        java.text.Collator collator=java.text.Collator.getInstance(java.util.Locale.forLanguageTag("ru"));
        collator.setStrength(java.text.Collator.SECONDARY);
        java.util.Comparator<Fish> c=java.util.Comparator.comparing(Fish::getSpecies, collator);
        return findAll().stream().sorted(asc ? c : c.reversed()).toList();
    }
    private void validate(Fish e){
        if(e==null)throw new BusinessException("Данные обязательны.");
        Aquarium parent=parentRepository.findById(e.getAquariumId()).orElseThrow(()->new EntityNotFoundException("Aquarium не найден."));
        if(e.getSpecies()==null || e.getSpecies().isBlank() || e.getSpecies().length()>120)
            throw new BusinessException("Вид рыбы обязателен, максимум 120 символов.");
        if(e.getQuantity()<=0) throw new BusinessException("Количество должно быть положительным.");
        if(parent.getType()==AquariumType.REPTILE)
            throw new BusinessException("Рыб нельзя размещать в аквариуме для рептилий.");
    }
}
