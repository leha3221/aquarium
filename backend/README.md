# Aquarium Backend (КР №3)

Отдельный Spring Boot REST-модуль для основной сущности «Аквариумы». JavaFX-приложение КР №2 остаётся отдельным клиентом.

## Запуск

Нужны Docker Desktop и JDK 21/Maven 3.9 для локальной сборки. В корне проекта создайте `.env` (секреты не коммитить), затем из корня выполните:

```powershell
powershell -ExecutionPolicy Bypass -File .\backend\start-compose.ps1
```

Swagger: `http://localhost:8080/swagger-ui.html`, health: `http://localhost:8080/actuator/health`. База PostgreSQL публикуется на `localhost:55432`; данные сохраняются в Docker volume.

## API аквариумов

- `GET /api/aquariums` — поиск `q`, фильтры `clientId`, `type`, `active`, `archived`, `minVolume`, `maxVolume`; пагинация `page`, `size` (до 100); сортировка по `name`, `code`, `volumeLiters`, `createdAt`, `updatedAt`.
- `GET /api/aquariums/{id}` — карточка.
- `POST /api/aquariums` — создание.
- `PUT /api/aquariums/{id}` — изменение с проверкой `version`.
- `DELETE /api/aquariums/{id}` и `POST /api/aquariums/{id}/restore` — архив/восстановление.

Типы аквариумов в JSON отображаются на русском (например, «Пресноводный»), а в БД остаются стабильные enum-коды.

## Ответственность команды

Исходники КР №3 разделены в `src/main/java/ru/mirea/aquarium/backend/team/`: `Maslov` — аквариумы, `Konstantinov` — клиенты, `Skvorcsov` — рыбы, `Seregin` — заявки. Для последующих частей созданы отдельные папки с README.
