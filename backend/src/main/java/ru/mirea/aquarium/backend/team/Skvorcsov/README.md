# Рыбы — Skvorcsov, КР №3

Одна запись Fish — **группа рыб одного вида в одном аквариуме**, как в КР №1–2.

## Интеграция с командой

Реализован модуль рыб. Общие JWT/refresh-сессии, пользователи, парольная политика, correlation ID и хранилище аудита не входят в эту часть.

**Без общей авторизации API рыб, видов и заметок возвращает 401.** Адаптер FishPermissions использует настоящий servlet Principal и isUserInRole: VIEWER, EDITOR, ADMIN (также ROLE_*). Заголовки с именем/ролью и поля автора не считаются удостоверением личности. Будущий JWT-фильтр должен заполнять principal и роли, например через Spring Security. Сам по себе Bearer-заголовок доступ не открывает. В тестах principal задаётся только средствами MockMvc.

| Роль | Доступ |
|---|---|
| VIEWER | Чтение, справочник, заметки, сводка, экспорт |
| EDITOR | Дополнительно изменения рыб/заметок, архив/восстановление, массовые операции, импорт |
| ADMIN | Дополнительно изменение справочника и окончательное удаление |

FishEvents.Changed содержит автора, тип события/сущности, UUID, время и описание. Общий аудит может подписаться через @TransactionalEventListener(phase = AFTER_COMMIT). События откатившихся операций не должны записываться. Сам журнал аудита здесь не реализован.

## Данные и правила

Fish: UUID, уникальный внешний code, aquariumId, speciesId, name, description, quantity, status, requiresObservation, tags, вычисляемый carePriority, archived, createdAt, updatedAt, version.

- code: 1–32 символа, латинские буквы/цифры/дефис/подчёркивание, первый символ — буква или цифра; приводится к верхнему регистру.
- name: 1–120, description: до 1000, quantity: 1–1 000 000.
- До 20 тегов, каждый до 40 символов, нижний регистр, без | и управляющих символов.
- Новая группа и восстановление требуют активный неархивный аквариум, тип не REPTILE.
- Статусы: NORMAL, ADAPTING, QUARANTINE, TREATMENT.
- Приоритет: TREATMENT → HIGH; ADAPTING/QUARANTINE или requiresObservation=true → MEDIUM; иначе LOW. Это приоритет обслуживания, не медицинский диагноз. Он не редактируется клиентом.
- FishSpecies — отдельный справочник: UUID, code, name, description, enabled, timestamps, version.
- Отключённый вид сохраняется у существующих групп; их можно редактировать с прежним видом. Новые группы и восстановление с таким видом запрещены.
- Архивные карточки и заметки доступны для чтения; изменение требует восстановления.
- Все изменения проверяют version; настоящую гонку дополнительно защищает JPA @Version.
- Окончательное удаление: только ADMIN, только из архива, только при PERMANENT_DELETE_ENABLED=true (по умолчанию false). Теги и заметки удаляются каскадно.
- Автор заметки определяется по principal создателя и не меняется. Любой EDITOR/ADMIN может редактировать заметки.

## API

| Метод и путь | Назначение |
|---|---|
| GET /api/fish | Список |
| GET /api/fish/{id} | Карточка |
| POST /api/fish | Создать |
| PUT /api/fish/{id} | Изменить |
| DELETE /api/fish/{id}?version=0 | Архивировать, вернуть актуальную карточку |
| POST /api/fish/{id}/restore | Восстановить, тело {"version":1} |
| DELETE /api/fish/{id}/permanent?version=2 | Удалить окончательно, ответ 204 |
| POST /api/fish/bulk | Массовая операция |
| GET /api/fish/summary | Сводка |
| GET /api/fish/export | CSV по фильтрам |
| POST /api/fish/import/preview?mode=NEW_ONLY | Проверить multipart-файл, поле file |
| POST /api/fish/import/{batchId}/apply | Применить проверенный файл |
| GET /api/fish-species?enabled=true | Страница справочника |
| POST /api/fish-species | Создать вид, ADMIN |
| PUT /api/fish-species/{id} | Изменить/включить/отключить вид, ADMIN |
| GET /api/fish/{fishId}/notes | Заметки, закреплённые первыми |
| POST /api/fish/{fishId}/notes | Создать заметку |
| PUT /api/fish/{fishId}/notes/{id} | Изменить заметку |
| DELETE /api/fish/{fishId}/notes/{id}?version=0 | Удалить заметку |

Пример создания группы (подставить реальные UUID существующих аквариума и вида):

```json
{
  "code": "FISH-001",
  "aquariumId": "11111111-1111-1111-1111-111111111111",
  "speciesId": "22222222-2222-2222-2222-222222222222",
  "name": "Неоны основного аквариума",
  "description": "Плановый осмотр",
  "quantity": 15,
  "status": "NORMAL",
  "requiresObservation": false,
  "tags": ["стая"]
}
```

- Изменение группы: {"data": {...все поля создания...}, "version": 0}.
- Создание вида: {"code":"NEON","name":"Неон","description":"","enabled":true}.
- Изменение вида: {"data":{...поля вида...},"version":0}.
- Создание заметки: {"text":"Осмотр выполнен","pinned":true}.
- Изменение заметки: {"data":{"text":"Повторный осмотр","pinned":false},"version":0}.

Списки возвращают стабильный DTO: content, number, size, totalElements, totalPages, first, last. JPA-сущности напрямую не возвращаются.

### Фильтры и сортировка

- q: поиск подстроки без учёта регистра по коду, названию, описанию и названию вида. % и _ — буквальные символы.
- aquariumId, speciesId, status, requiresObservation, carePriority, tag.
- archive=ACTIVE|ARCHIVED|ALL. По умолчанию список/экспорт — ACTIVE, сводка — ALL.
- quantityOp=EQ|GT|GTE|LT|LTE вместе с quantity.
- page=0, size=20; допустимый size: 1–100.
- sort=code|name|quantity|createdAt|updatedAt; direction=asc|desc. Список по умолчанию createdAt desc; дополнительный порядок по UUID.

Фильтры объединяются через AND, используются Criteria/Specification, пользовательские строки не конкатенируются в SQL/JPQL.

Пример: GET /api/fish?status=QUARANTINE&quantityOp=GTE&quantity=10&tag=новые&page=0&size=20&sort=quantity&direction=desc.

### Массовые операции

Тело: {"items":[{"id":"UUID группы","version":0}],"action":"ADD_TAG","tag":"наблюдение"}.

Действия: ARCHIVE, RESTORE, SET_STATUS (нужен status), ADD_TAG/REMOVE_TAG (нужен tag). До 100 элементов. Каждая запись — отдельная транзакция. Ответ: successes с актуальными карточками и errors с UUID, HTTP-статусом и сообщением. Повтор UUID — ошибка отдельного элемента.

### CSV

UTF-8, запятая, стандартное экранирование кавычками, точный порядок заголовков:

```csv
code,aquariumCode,speciesCode,name,description,quantity,status,requiresObservation,tags
FISH-001,AQ-1,NEON,Неоны,Плановый осмотр,15,NORMAL,false,стая|новые
```

Используются внешние коды аквариума/вида. Экспортирует все совпавшие записи, а не одну страницу; добавляет UTF-8 BOM. Формулы в текстовых ячейках экранируются апострофом; обратный импорт сохраняет текст.

Проверка: до 1 МиБ, до 2000 записей, без изменения fish. Ответ: validRows, invalidRows, errors с номерами логических CSV-записей (заголовок №1; многострочный текст — одна запись). Если есть ошибки, batchId отсутствует: исправить и проверить снова.

Успешный план хранится в БД 30 минут, принадлежит проверившему пользователю, применяется один раз. NEW_ONLY запрещает существующие коды; UPSERT обновляет по code и создаёт новые. Архивные группы автоматически не восстанавливаются.

Применение — одна транзакция. Заново проверяются ссылки/коды и сохранённые UUID/версии. При конфликте весь импорт откатывается; файл проверить снова. Истёкшие планы остаются для последующей общей политики очистки; применять их запрещено.

### Сводка и ошибки

Сводка принимает те же фильтры без пагинации. total/active/archived/requiresObservation считают группы; individuals — сумму quantity, averageQuantity — среднее по группам. bySpecies/byStatus/byCarePriority содержат key, name, groups, individuals. Агрегация выполняется в БД.

Ошибка: {"timestamp":"...","status":409,"error":"Conflict","message":"..."}.
400 — данные/формат, 401 — нет principal, 403 — права/удаление выключено, 404 — запись, 409 — версия/код/ограничения/применённый или истёкший план. SQL и stack trace клиенту не передаются.

## Миграции

- V1 не изменена.
- V2 создаёт недостающие таблицы уже существующих Client/ServiceRequest и внешние ключи: без них чистый backend не проходил Hibernate validate. Если старый volume содержит аквариумы с несуществующим client_id, исправить связи перед V2; вымышленные клиенты не создаются.
- V3 создаёт fish, fish_species, fish_tags, fish_notes, fish_import_batches, ограничения, индексы и начальные виды.
- PostgreSQL-триггеры сохраняют правило REPTILE при изменениях через модуль аквариумов, защищают от гонки с добавлением рыб. Перед архивированием/отключением аквариума нужно архивировать его рыб.
- Индексы поддерживают связи, статус, количество, архив, теги и приоритет. Обычные lower-индексы не ускоряют произвольный поиск %подстроки%; pg_trgm можно согласовать отдельно.
- Перенос данных отдельной БД КР №1 из BIGINT в UUID остаётся общей задачей команды; здесь не реализован.
- Текущий Compose использует PostgreSQL 16; модуль также проверяется на PostgreSQL 17. Версия общего контейнера в этой ветке не меняется.

## Запуск и тесты

Запуск Docker — по backend/README.md и backend/start-compose.ps1. Swagger: http://localhost:8080/swagger-ui.html, health: http://localhost:8080/actuator/health. Для API рыб сначала подключить общую авторизацию, иначе 401.

Java 21; Maven Wrapper скачивает Maven 3.9.11 при первом запуске:

```powershell
cd backend
.\mvnw.cmd verify
```

По умолчанию тесты используют H2 и создают тестовую схему; PostgreSQL-специфичная проверка пропускается.

Для **отдельной пустой тестовой PostgreSQL-БД** (тесты удаляют данные, не использовать рабочую базу):

```powershell
.\mvnw.cmd verify "-Dtest.database.url=jdbc:postgresql://localhost:55432/fish_module_test" "-Dtest.database.username=fish_test" "-Dtest.database.password=ваш_тестовый_пароль" "-Dtest.flyway=true" "-Dtest.ddl=validate" "-Dtest.permanent.delete=true"
```

Проверяются: контекст, вычисляемый приоритет и SQL-фильтр, бизнес-правила, комбинации фильтров, версии, архив, отключённый вид, частичный успех массовых операций, заметки, CSV preview/apply/round-trip/откат/срок/владелец, 401/403/валидация/протокол, сводка. На PostgreSQL дополнительно настоящие миграции, Hibernate validate, триггеры и окончательное удаление с каскадом заметок.
