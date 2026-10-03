package ru.mirea.aquarium.service;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.repository.*;
import ru.mirea.aquarium.exception.*;
import java.util.List;
public class AquariumService {
    private final AquariumRepository repository;
    private final ClientRepository parentRepository;
    private final FishRepository fishRepository;
    public AquariumService(AquariumRepository repository,ClientRepository parentRepository){
        this(repository, parentRepository, new FishRepository());
    }
    public AquariumService(AquariumRepository repository, ClientRepository parentRepository, FishRepository fishRepository) {
        this.repository=repository;this.parentRepository=parentRepository;this.fishRepository=fishRepository;
    }
    public Aquarium create(Aquarium e){validate(e);return repository.create(e);}
    public List<Aquarium> findAll(){return repository.findAll();}
    public Aquarium get(long id){return repository.findById(id).orElseThrow(()->new EntityNotFoundException("Aquarium с ID "+id+" не найден."));}
    public void update(Aquarium e){get(e.getId());validate(e);repository.update(e);}
    public void delete(long id){get(id);repository.delete(id);}
    public List<Aquarium> findByClient(long id){
        parentRepository.findById(id).orElseThrow(()->new EntityNotFoundException("Client с ID "+id+" не найден."));
        return findAll().stream().filter(e -> e.getClientId()==id).toList();
    }
    public List<Aquarium> searchByName(String text) {
        String q=text.toLowerCase(java.util.Locale.ROOT);
        return findAll().stream().filter(e -> e.getName().toLowerCase(java.util.Locale.ROOT).contains(q)).toList();
    }
    public List<Aquarium> filterByType(AquariumType type) {
        return findAll().stream().filter(e -> e.getType()==type).toList();
    }
    public List<Aquarium> sortByVolume(boolean asc) {
        java.util.Comparator<Aquarium> c=java.util.Comparator.comparing(Aquarium::getVolumeLiters);
        return findAll().stream().sorted(asc ? c : c.reversed()).toList();
    }
    private void validate(Aquarium e){
        if(e==null)throw new BusinessException("Данные обязательны.");
        Client parent=parentRepository.findById(e.getClientId()).orElseThrow(()->new EntityNotFoundException("Client не найден."));
        if(e.getName()==null || e.getName().isBlank() || e.getName().length()>120)
            throw new BusinessException("Название обязательно, максимум 120 символов.");
        if(e.getType()==null) throw new BusinessException("Тип обязателен.");
        if(e.getVolumeLiters()==null || e.getVolumeLiters().signum()<=0
            || e.getVolumeLiters().compareTo(new java.math.BigDecimal("99999999.99"))>0
            || e.getVolumeLiters().stripTrailingZeros().scale()>2)
            throw new BusinessException("Объём должен быть больше нуля, до 99999999.99 л, максимум два знака после точки.");
        if(e.getId()!=0 && e.getType()==AquariumType.REPTILE
            && fishRepository.findAll().stream().anyMatch(f -> f.getAquariumId()==e.getId()))
            throw new BusinessException("Перед сменой типа на REPTILE перенесите или удалите рыб.");
    }
}