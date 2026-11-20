package ru.mirea.aquarium.backend.team.Konstantinov.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.aquarium.backend.team.Konstantinov.domain.Client;
import ru.mirea.aquarium.backend.team.Konstantinov.dto.ClientRequest;
import ru.mirea.aquarium.backend.team.Konstantinov.repository.ClientRepository;

import java.util.Locale;
import java.util.UUID;

@Service
public class ClientService {
    private final ClientRepository repo;

    public ClientService(ClientRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Page<Client> list(String q, Boolean active, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException("Размер страницы должен быть от 1 до 100");
        Specification<Client> s = (r, x, c) -> c.conjunction();
        if (q != null && !q.isBlank()) {
            String p = "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
            s = s.and((r, x, c) -> c.or(c.like(c.lower(r.get("fullName")), p), c.like(c.lower(r.get("phone")), p), c.like(c.lower(r.get("email")), p)));
        }
        if (active != null) s = s.and((r, x, c) -> c.equal(r.get("active"), active));
        return repo.findAll(s, PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "fullName")));
    }

    @Transactional(readOnly = true)
    public Client get(UUID id) {
        return repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));
    }

    @Transactional
    public Client create(ClientRequest d) {
        Client c = new Client();
        apply(c, d);
        return repo.save(c);
    }

    @Transactional
    public Client update(UUID id, ClientRequest d) {
        Client c = get(id);
        if (d.version() == null || d.version() != c.getVersion())
            throw new OptimisticLockException("Клиент уже изменен другим пользователем");
        apply(c, d);
        return repo.save(c);
    }

    @Transactional
    public Client active(UUID id, boolean value) {
        Client c = get(id);
        c.setActive(value);
        return c;
    }

    private void apply(Client c, ClientRequest d) {
        c.setFullName(d.fullName().trim());
        c.setPhone(d.phone().trim());
        c.setEmail(d.email() == null ? null : d.email().trim());
        c.setAddress(d.address() == null ? null : d.address().trim());
        c.setActive(d.active());
    }
}
