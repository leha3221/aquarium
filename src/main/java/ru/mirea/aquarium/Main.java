package ru.mirea.aquarium;

import ru.mirea.aquarium.repository.ClientRepository;
import ru.mirea.aquarium.repository.ServiceRequestRepository;
import ru.mirea.aquarium.service.ClientService;
import ru.mirea.aquarium.service.ServiceRequestService;
import ru.mirea.aquarium.ui.ConsoleUI;
import ru.mirea.aquarium.util.DatabaseManager;

public class Main {
    public static void main(String[] args) {
        try {
            DatabaseManager.testConnection();

            ClientRepository clientRepository = new ClientRepository();
            ServiceRequestRepository requestRepository = new ServiceRequestRepository();

            ClientService clientService = new ClientService(clientRepository);
            ServiceRequestService requestService =
                    new ServiceRequestService(requestRepository, clientRepository);

            new ConsoleUI(clientService, requestService).run();
        } catch (Exception e) {
            System.err.println("Критическая ошибка запуска: " + e.getMessage());
            System.err.println("Проверьте PostgreSQL, БД и настройки DatabaseManager.");
        }
    }
}
