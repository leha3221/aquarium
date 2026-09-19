# Информационная система сервиса обслуживания аквариумов

Вариант: **Заявка на обслуживание аквариума**.

## Предметная область
Сервис принимает заявки клиентов на обслуживание аквариумов. Для каждой заявки хранится клиент, адрес, тип обслуживания, тип аквариума, дата создания/плановая дата, стоимость и статус.

## Сущности
- `Client` — клиент сервиса.
- `ServiceRequest` — заявка на обслуживание аквариума.

Связь: `service_requests.client_id -> clients.id`.

## Требования КР1
Реализованы:
- CRUD для клиентов и заявок;
- `enum` для статуса, типа обслуживания и типа аквариума;
- интерфейс `Exporter` и полиморфные реализации Excel/CSV;
- собственные исключения;
- 5+ бизнес-правил;
- 2+ поиска;
- 2+ фильтра;
- 2+ сортировки;
- статистика из 5+ показателей;
- JDBC: `Connection`, `PreparedStatement`, `ResultSet`, try-with-resources;
- PostgreSQL;
- Excel `.xlsx` и CSV;
- многослойная архитектура `Console UI -> Service -> Repository -> Database`.

## Запуск
1. Установить JDK 17+ и PostgreSQL 14+.
2. Создать БД:
   ```sql
   psql -U postgres -f sql/schema.sql
   ```
3. При необходимости изменить логин/пароль в `src/main/java/ru/mirea/aquarium/util/DatabaseManager.java`.
4. Выполнить:
   ```bash
   mvn clean compile
   mvn exec:java
   ```

По умолчанию:
- URL: `jdbc:postgresql://localhost:5432/aquarium_service`
- user: `postgres`
- password: ваш пароль PostgreSQL

## Защита
Главная идея архитектуры:
`Console UI -> Service -> Repository/JDBC -> PostgreSQL`

SQL-запросы находятся в Repository, а бизнес-правила — в Service.
