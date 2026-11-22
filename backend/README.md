# Aquarium Backend (КР №3)

Spring Boot REST backend основной сущности «Аквариумы». JavaFX-приложение КР №2 находится отдельно и остаётся настольным клиентом.

## Запуск

Нужны Docker Desktop и JDK 21. Создайте `.env` в корне проекта, затем выполните из корня:

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health
- PostgreSQL: localhost:55432 (данные сохраняются в Docker volume)

В backend нет web-фронтенда и общей авторизации: API клиентов/аквариумов/заявок и Swagger открыты без токена.
Модуль «Рыбы» проверяет principal и роли будущей общей авторизации; без неё возвращает 401.

## API рыб

Реализованы группы рыб, справочник видов, фильтры, серверная пагинация, версии, архив,
заметки, массовые операции, двухэтапный CSV-импорт/экспорт и аналитическая сводка.
Контракты запросов, права, миграции и запуск тестов:
[документация модуля «Рыбы»](src/main/java/ru/mirea/aquarium/backend/team/Skvorcsov/README.md).

Проверка с Java 21 без установленного Maven: `cd backend`, затем `.\mvnw.cmd verify`.

## API аквариумов

- `GET /api/aquariums` — поиск `q`, фильтры `clientId`, `type`, `active`, `archived`, `minVolume`, `maxVolume`; пагинация `page`, `size` (до 100); сортировка по `name`, `code`, `volumeLiters`, `createdAt`, `updatedAt`.
- `GET /api/aquariums/{id}` — карточка.
- `POST /api/aquariums` — создание.
- `PUT /api/aquariums/{id}` — изменение с проверкой `version`.
- `DELETE /api/aquariums/{id}` и `POST /api/aquariums/{id}/restore` — архив/восстановление.

Типы аквариумов в JSON отображаются по-русски, при этом коды в БД остаются стабильными.

## Ответственность команды

Исходники КР №3 разделяются в `src/main/java/ru/mirea/aquarium/backend/team/`: `Maslov` — аквариумы, `Konstantinov` — клиенты, `Skvorcsov` — рыбы, `Seregin` — заявки.
