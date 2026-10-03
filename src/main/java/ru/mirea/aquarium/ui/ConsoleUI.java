package ru.mirea.aquarium.ui;

import ru.mirea.aquarium.exception.BusinessException;
import ru.mirea.aquarium.exception.EntityNotFoundException;
import ru.mirea.aquarium.export.CsvExporter;
import ru.mirea.aquarium.export.ExcelExporter;
import ru.mirea.aquarium.export.Exporter;
import ru.mirea.aquarium.model.*;
import ru.mirea.aquarium.service.AquariumService;
import ru.mirea.aquarium.service.FishService;
import ru.mirea.aquarium.service.ClientService;
import ru.mirea.aquarium.service.ServiceRequestService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

public class ConsoleUI {
    private final ClientService clientService;
    private final ServiceRequestService requestService;
    private final AquariumService aquariumService;
    private final FishService fishService;
    private final Scanner scanner = new Scanner(System.in);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ConsoleUI(ClientService clientService, ServiceRequestService requestService, AquariumService aquariumService, FishService fishService) {
        this.clientService = clientService;
        this.requestService = requestService;
        this.aquariumService = aquariumService;
        this.fishService = fishService;
    }

    public void run() {
        while (true) {
            try {
                System.out.println("\n========================================");
                System.out.println(" СЕРВИС ОБСЛУЖИВАНИЯ АКВАРИУМОВ");
                System.out.println("========================================");
                System.out.println("1. Клиенты");
                System.out.println("2. Заявки");
                System.out.println("3. Поиск заявок");
                System.out.println("4. Фильтрация заявок");
                System.out.println("5. Сортировка заявок");
                System.out.println("6. Статистика");
                System.out.println("7. Экспорт заявок");
                System.out.println("8. Аквариумы");
                System.out.println("9. Рыбы");
                System.out.println("0. Выход");

                int choice = readInt("Выберите действие: ");
                switch (choice) {
                    case 1 -> clientsMenu();
                    case 2 -> requestsMenu();
                    case 3 -> searchMenu();
                    case 4 -> filterMenu();
                    case 5 -> sortMenu();
                    case 6 -> statistics();
                    case 7 -> exportMenu();
                    case 8 -> aquariumsMenu();
                    case 9 -> fishMenu();
                    case 0 -> { System.out.println("Работа завершена."); return; }
                    default -> System.out.println("Нет такого пункта.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Ошибка приложения/БД: " + e.getMessage());
            }
        }
    }

    private void clientsMenu() {
        while (true) {
            System.out.println("\n--- КЛИЕНТЫ ---");
            System.out.println("1. Создать");
            System.out.println("2. Показать всех");
            System.out.println("3. Получить по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("0. Назад");
            int c = readInt("Действие: ");
            try {
                switch (c) {
                    case 1 -> clientService.create(readClient(0));
                    case 2 -> clientService.findAll().forEach(System.out::println);
                    case 3 -> System.out.println(clientService.get(readLong("ID: ")));
                    case 4 -> clientService.update(readClient(readLong("ID: ")));
                    case 5 -> clientService.delete(readLong("ID: "));
                    case 0 -> { return; }
                    default -> System.out.println("Нет такого пункта.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    private void requestsMenu() {
        while (true) {
            System.out.println("\n--- ЗАЯВКИ ---");
            System.out.println("1. Создать");
            System.out.println("2. Показать все");
            System.out.println("3. Получить по ID");
            System.out.println("4. Изменить");
            System.out.println("5. Удалить");
            System.out.println("0. Назад");
            int c = readInt("Действие: ");
            try {
                switch (c) {
                    case 1 -> System.out.println(requestService.create(readRequest(0)));
                    case 2 -> requestService.findAll().forEach(System.out::println);
                    case 3 -> System.out.println(requestService.get(readLong("ID: ")));
                    case 4 -> requestService.update(readRequest(readLong("ID: ")));
                    case 5 -> requestService.delete(readLong("ID: "));
                    case 0 -> { return; }
                    default -> System.out.println("Нет такого пункта.");
                }
            } catch (BusinessException | EntityNotFoundException e) {
                System.out.println("Ошибка: " + e.getMessage());
            } catch (RuntimeException e) {
                System.out.println("Ошибка БД: " + e.getMessage());
            }
        }
    }

    private void searchMenu() {
        System.out.println("\n1. Поиск по описанию");
        System.out.println("2. Поиск по клиенту");
        int c = readInt("Действие: ");
        List<ServiceRequest> result = switch (c) {
            case 1 -> requestService.searchByDescription(readLine("Текст: "));
            case 2 -> requestService.searchByClient(readLine("Имя клиента: "));
            default -> List.of();
        };
        result.forEach(System.out::println);
        System.out.println("Найдено: " + result.size());
    }

    private void filterMenu() {
        System.out.println("\n1. По статусу");
        System.out.println("2. По минимальному приоритету");
        int c = readInt("Действие: ");
        List<ServiceRequest> result;
        if (c == 1) {
            result = requestService.filterByStatus(readEnum(RequestStatus.values(), "Статус"));
        } else if (c == 2) {
            result = requestService.filterByPriority(readInt("Минимальный приоритет (1-5): "));
        } else return;
        result.forEach(System.out::println);
    }

    private void sortMenu() {
        System.out.println("\n1. По плановой дате");
        System.out.println("2. По стоимости");
        System.out.println("3. По имени клиента (А–Я / Я–А)");
        int c = readInt("Действие: ");
        if (c < 1 || c > 3) {
            System.out.println("Нет такого пункта.");
            return;
        }
        int direction;
        do {
            direction = readInt(c == 3
                    ? "1 - от А до Я, 2 - от Я до А: "
                    : "1 - по возрастанию, 2 - по убыванию: ");
            if (direction != 1 && direction != 2) System.out.println("Выберите 1 или 2.");
        } while (direction != 1 && direction != 2);
        boolean asc = direction == 1;
        List<ServiceRequest> result = switch (c) {
            case 1 -> requestService.sortByDate(asc);
            case 2 -> requestService.sortByPrice(asc);
            case 3 -> requestService.sortByClientName(asc);
            default -> throw new IllegalStateException("Неизвестная сортировка");
        };
        result.forEach(System.out::println);
    }
    private void statistics() {
        List<ServiceRequest> all = requestService.findAll();
        System.out.println("\n--- СТАТИСТИКА ---");
        System.out.println("Всего клиентов: " + clientService.findAll().size());
        System.out.println("Всего заявок: " + all.size());
        System.out.println("Всего аквариумов: " + aquariumService.findAll().size());
        System.out.println("Записей о рыбах: " + fishService.findAll().size());
        System.out.println("Всего рыб: " + fishService.findAll().stream().mapToLong(Fish::getQuantity).sum());
        System.out.println("Новых: " + requestService.countStatus(RequestStatus.NEW));
        System.out.println("Подтверждённых: " + requestService.countStatus(RequestStatus.CONFIRMED));
        System.out.println("В работе: " + requestService.countStatus(RequestStatus.IN_PROGRESS));
        System.out.println("Завершённых: " + requestService.countStatus(RequestStatus.COMPLETED));
        System.out.println("Отменённых: " + requestService.countStatus(RequestStatus.CANCELLED));
        System.out.println("Высокого приоритета (4-5): " + requestService.countHighPriority());
        System.out.println("Доход по завершённым: " + requestService.totalRevenueCompleted());
    }

    private void exportMenu() {
        System.out.println("\n--- ЭКСПОРТ ЗАЯВОК ---");
        System.out.println("1. Excel (.xlsx)");
        System.out.println("2. CSV (.csv)");
        int c = readInt("Действие: ");

        // Полиморфизм: переменная объявлена типом интерфейса Exporter,
        // а конкретная реализация подставляется в зависимости от выбора.
        Exporter exporter = switch (c) {
            case 1 -> new ExcelExporter();
            case 2 -> new CsvExporter();
            default -> null;
        };
        if (exporter == null) return;

        String fileName = readLine("Имя файла (без расширения): ");
        String filePath = fileName + "." + exporter.fileExtension();

        List<ServiceRequest> all = requestService.findAll();
        exporter.export(all, filePath);
        System.out.println("Экспортировано " + all.size() + " заявок в файл: " + filePath);
    }

    private void aquariumsMenu() {
        while (true) {
            System.out.println("\n--- АКВАРИУМЫ ---");
            System.out.println("1. Создать\n2. Показать все\n3. Получить по ID\n4. Изменить\n5. Удалить (вместе с рыбами)");
            System.out.println("6. Поиск по названию\n7. По клиенту\n8. По типу\n9. Сортировка по объёму\n0. Назад");
            try {
                switch (readInt("Действие: ")) {
                    case 1 -> System.out.println(aquariumService.create(readAquarium(0)));
                    case 2 -> aquariumService.findAll().forEach(System.out::println);
                    case 3 -> System.out.println(aquariumService.get(readLong("ID: ")));
                    case 4 -> aquariumService.update(readAquarium(readLong("ID: ")));
                    case 5 -> aquariumService.delete(readLong("ID: "));
                    case 6 -> aquariumService.searchByName(readLine("Название: ")).forEach(System.out::println);
                    case 7 -> aquariumService.findByClient(readLong("ID клиента: ")).forEach(System.out::println);
                    case 8 -> aquariumService.filterByType(readEnum(AquariumType.values(), "Тип")).forEach(System.out::println);
                    case 9 -> aquariumService.sortByVolume(readInt("1 — возрастание, 2 — убывание: ") == 1).forEach(System.out::println);
                    case 0 -> { return; }
                    default -> System.out.println("Нет такого пункта.");
                }
            } catch (RuntimeException e) { System.out.println("Ошибка: " + e.getMessage()); }
        }
    }
    private void fishMenu() {
        while (true) {
            System.out.println("\n--- РЫБЫ ---");
            System.out.println("1. Создать\n2. Показать всех\n3. Получить по ID\n4. Изменить\n5. Удалить");
            System.out.println("6. Поиск по виду\n7. По аквариуму\n8. Сортировка по количеству\n9. Сортировка по алфавиту (вид рыбы)\n0. Назад");
            try {
                switch (readInt("Действие: ")) {
                    case 1 -> System.out.println(fishService.create(readFish(0)));
                    case 2 -> fishService.findAll().forEach(System.out::println);
                    case 3 -> System.out.println(fishService.get(readLong("ID: ")));
                    case 4 -> fishService.update(readFish(readLong("ID: ")));
                    case 5 -> fishService.delete(readLong("ID: "));
                    case 6 -> fishService.searchBySpecies(readLine("Вид: ")).forEach(System.out::println);
                    case 7 -> fishService.findByAquarium(readLong("ID аквариума: ")).forEach(System.out::println);
                    case 8 -> fishService.sortByQuantity(readInt("1 — возрастание, 2 — убывание: ") == 1).forEach(System.out::println);
                    case 9 -> fishService.sortBySpecies(readInt("1 — А–Я, 2 — Я–А: ") == 1).forEach(System.out::println);
                    case 0 -> { return; }
                    default -> System.out.println("Нет такого пункта.");
                }
            } catch (RuntimeException e) { System.out.println("Ошибка: " + e.getMessage()); }
        }
    }
    private Aquarium readAquarium(long id) {
        if (id != 0) aquariumService.get(id);
        long clientId = readLong("ID клиента: ");
        String name = readLine("Название аквариума: ");
        AquariumType type = readEnum(AquariumType.values(), "Тип аквариума");
        return new Aquarium(id, clientId, name, type, readDecimal("Объём в литрах: "));
    }
    private Fish readFish(long id) {
        if (id != 0) fishService.get(id);
        return new Fish(id, readLong("ID аквариума: "), readLine("Вид рыбы: "), readInt("Количество: "));
    }
    private BigDecimal readDecimal(String prompt) {
        while (true) {
            try { return new BigDecimal(readLine(prompt).replace(',', '.')); }
            catch (NumberFormatException e) { System.out.println("Введите число, например 120.5."); }
        }
    }

    private Client readClient(long id) {
        String name;
        while (true) {
            name = readLine("ФИО: ");
            if (name.length() >= 3) break;
            System.out.println("ФИО должно быть не короче 3 символов. Повторите.");
        }

        String phone;
        while (true) {
            phone = readLine("Телефон (+79991234567): ");
            if (phone.matches("\\+?\\d{10,15}")) break;
            System.out.println("Телефон должен содержать 10–15 цифр, можно с + в начале. Повторите.");
        }

        String email;
        while (true) {
            email = readLine("Email: ");
            if (email.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$")) break;
            System.out.println("Некорректный email. Повторите.");
        }

        String address;
        while (true) {
            address = readLine("Адрес: ");
            if (!address.isBlank()) break;
            System.out.println("Адрес обязателен. Повторите.");
        }

        return new Client(id, name, phone, email, address);
    }

    private ServiceRequest readRequest(long id) {
        long clientId = readLong("ID клиента: ");
        AquariumType aquarium = readEnum(AquariumType.values(), "Тип аквариума");
        ServiceType service = readEnum(ServiceType.values(), "Тип услуги");
        String description = readLine("Описание: ");
        RequestStatus status = readEnum(RequestStatus.values(), "Статус");
        int priority = readInt("Приоритет 1-5: ");
        LocalDateTime scheduled = readDateTime("Плановая дата (yyyy-MM-dd HH:mm): ");
        BigDecimal price = new BigDecimal(readLine("Стоимость: "));
        LocalDateTime created = id == 0 ? LocalDateTime.now() : requestService.get(id).getCreatedAt();
        return new ServiceRequest(id, clientId, aquarium, service, description,
                status, priority, created, scheduled, price, null);
    }

    private <T extends Enum<T>> T readEnum(T[] values, String label) {
        System.out.println(label + ":");
        for (int i = 0; i < values.length; i++) System.out.println((i + 1) + ". " + values[i]);
        int n = readInt("Выбор: ");
        if (n < 1 || n > values.length) throw new BusinessException("Некорректное значение.");
        return values[n - 1];
    }

    private LocalDateTime readDateTime(String prompt) {
        while (true) {
            try { return LocalDateTime.parse(readLine(prompt), formatter); }
            catch (Exception e) { System.out.println("Ошибка: используйте формат yyyy-MM-dd HH:mm."); }
        }
    }

    private int readInt(String prompt) {
        while (true) {
            try { return Integer.parseInt(readLine(prompt)); }
            catch (NumberFormatException e) { System.out.println("Ошибка: ID/число должно быть целым."); }
        }
    }

    private long readLong(String prompt) {
        while (true) {
            try { return Long.parseLong(readLine(prompt)); }
            catch (NumberFormatException e) { System.out.println("Ошибка: ID должен быть целым числом."); }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}
