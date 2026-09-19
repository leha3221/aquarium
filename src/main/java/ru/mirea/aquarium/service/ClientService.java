package ru.mirea.aquarium.service;

import ru.mirea.aquarium.exception.BusinessException;
import ru.mirea.aquarium.exception.EntityNotFoundException;
import ru.mirea.aquarium.model.Client;
import ru.mirea.aquarium.repository.ClientRepository;

import java.util.List;

public class ClientService {
    private final ClientRepository repository;

    public ClientService(ClientRepository repository) {
        this.repository = repository;
    }

    public Client create(Client c) {
        validate(c);
        return repository.create(c);
    }

    public List<Client> findAll() { return repository.findAll(); }

    public Client get(long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент с ID " + id + " не найден."));
    }

    public void update(Client c) {
        get(c.getId());
        validate(c);
        repository.update(c);
    }

    public void delete(long id) {
        get(id);
        repository.delete(id);
    }

    private void validate(Client c) {
        if (c.getFullName() == null || c.getFullName().isBlank())
            throw new BusinessException("ФИО клиента обязательно.");
        if (c.getFullName().length() < 3)
            throw new BusinessException("ФИО должно быть не короче 3 символов.");

        if (c.getPhone() == null || c.getPhone().isBlank())
            throw new BusinessException("Телефон обязателен.");
        if (!c.getPhone().matches("\\+?\\d{10,15}"))
            throw new BusinessException("Телефон должен содержать 10–15 цифр, можно с + в начале.");

        if (c.getEmail() == null || c.getEmail().isBlank())
            throw new BusinessException("Email обязателен.");
        if (!c.getEmail().matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$"))
            throw new BusinessException("Некорректный email.");

        if (c.getAddress() == null || c.getAddress().isBlank())
            throw new BusinessException("Адрес обязателен.");
    }
}
