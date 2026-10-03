package ru.mirea.aquarium;

import ru.mirea.aquarium.repository.AquariumRepository;
import ru.mirea.aquarium.repository.FishRepository;
import ru.mirea.aquarium.service.AquariumService;
import ru.mirea.aquarium.service.FishService;
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

            AquariumRepository aquariumRepository = new AquariumRepository();
            AquariumService aquariumService = new AquariumService(aquariumRepository, clientRepository);
            FishService fishService = new FishService(new FishRepository(), aquariumRepository);
            new ConsoleUI(clientService, requestService, aquariumService, fishService).run();
        } catch (Exception e) {
            System.err.println("Критическая ошибка запуска: " + e.getMessage());
            System.err.println("Проверьте PostgreSQL, БД и настройки DatabaseManager.");
        }
    }
}
