# Secure REST API

Учебный проект для лабораторной работы №1 по дисциплине «Информационная безопасность»:
защищённый REST API на Java с автоматическими тестами, SAST и SCA в CI/CD.

- Репозиторий: <https://github.com/timur1516-itmo/infosec-lab-1>
- Запуски CI/CD: <https://github.com/timur1516-itmo/infosec-lab-1/actions>

## Стек

- Java 21 и Spring Boot 3;
- Spring Web — HTTP/JSON API;
- Spring Data JPA и SQLite — хранение данных и параметризованные SQL-запросы;
- Spring Security — только явные правила доступа и подключение JWT-фильтра;
- JJWT — создание и криптографическая проверка JWT;
- BCrypt — адаптивное хэширование паролей;
- OWASP Java HTML Sanitizer — удаление HTML из пользовательских строк;
- JUnit, MockMvc — автоматические тесты;
- SpotBugs — SAST;
- OWASP Dependency-Check — SCA.

Для SQLite подключён официальный `natives-all` runtime-артефакт. Multi-stage Docker-сборка
явно выбирает и извлекает библиотеку для `TARGETARCH`, поэтому образ собирается под Linux
AMD64 и ARM64, включая Apple Silicon, без неявного поиска native-файла внутри fat JAR.

## Эндпоинты

| Метод | Путь | Требует JWT | Назначение |
|---|---|---:|---|
| `POST` | `/auth/login` | нет | Проверить логин/пароль и получить JWT |
| `GET` | `/api/data` | да | Получить список записей |
| `POST` | `/api/data` | да | Создать запись |

## Быстрый запуск

### Docker Compose

Требуются Docker и Docker Compose. Создайте локальный файл настроек:

```bash
cp .env.example .env
```

Заполните в `.env` как минимум `APP_JWT_SECRET` (не менее 32 случайных байт) и
`APP_BOOTSTRAP_PASSWORD`, затем запустите приложение:

```bash
docker compose up --build -d
docker compose logs -f api
```

API будет доступен на `http://localhost:8080`. Значение `APP_PORT` в `.env` позволяет
изменить порт на хосте. SQLite хранится в именованном volume `itmo-secure-api_api-data`,
поэтому данные сохраняются при пересоздании контейнера.

Остановка без удаления базы:

```bash
docker compose down
```

Удаление контейнеров вместе с учебной базой:

```bash
docker compose down --volumes
```

Последняя команда необратимо удаляет данные volume.

Контейнер работает от непривилегированного пользователя, без Linux capabilities, с
`no-new-privileges` и read-only корневой файловой системой. Для записи доступны только
volume `/data` и временная файловая система `/tmp`. `.dockerignore` не позволяет добавить
локальный `.env`, Git-историю, БД или Maven-кэш в образ.

### Запуск без Docker

Требуются JDK 21 и Maven 3.9+.

Задайте секрет длиной не менее 32 байт и данные начального пользователя:

```bash
export APP_JWT_SECRET='replace-this-with-a-random-secret-of-32-bytes-or-more'
export APP_BOOTSTRAP_USERNAME='admin'
export APP_BOOTSTRAP_PASSWORD='change-this-strong-password'
mvn spring-boot:run
```

Секрет и пароль передаются через окружение и не сохраняются в репозитории. При первом
запуске пользователь создаётся в `secure-api.db`, а пароль немедленно превращается в
BCrypt-хэш. При последующих запусках существующий пользователь не перезаписывается.

### 1. Вход

```bash
curl -i -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"change-this-strong-password"}'
```

Успешный ответ:

```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900
}
```

### 2. Запись данных

```bash
export TOKEN='paste-token-here'
curl -i -X POST http://localhost:8080/api/data \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Example","content":"Protected data"}'
```

### 3. Чтение данных

```bash
curl -i http://localhost:8080/api/data \
  -H "Authorization: Bearer $TOKEN"
```

Запрос к `/api/data` без заголовка `Authorization` получает `401 Unauthorized`.

## Как устроена безопасность

Реализация специально разделена на небольшие классы, чтобы каждую меру можно было
проверить по исходному коду.

### SQL-инъекции

Контроллеры не создают SQL. Репозитории `UserRepository` и `DataItemRepository` работают
через Spring Data JPA, который передаёт пользовательские значения параметрами prepared
statement. Конкатенации строк для построения запросов в проекте нет. Интеграционный тест
отправляет строку `student' OR '1'='1` как логин и проверяет, что вход не выполняется.

### XSS

Поля `title` и `content` проходят через `PlainTextSanitizer`. Политика OWASP Sanitizer имеет
пустой список разрешённых HTML-тегов и атрибутов, поэтому `<script>`, обработчики событий
вроде `onerror` и прочая разметка удаляются до записи в БД. API отвечает JSON с корректным
`Content-Type`, что дополнительно не позволяет браузеру интерпретировать ответ как HTML.

### Пароли

Открытый пароль существует только во время обработки входного запроса или первоначального
создания пользователя. `PasswordService` использует BCrypt с cost factor 12. В таблице
`users` хранится только `password_hash`. API никогда не сериализует сущность пользователя.

### JWT

`JwtService` явно создаёт токен с полями `sub`, `iat`, `exp` и подписывает его алгоритмом
HS256. Ключ берётся из `APP_JWT_SECRET`; запуск с пустым или слишком коротким ключом
запрещён. Время жизни по умолчанию — 15 минут.

`JwtAuthenticationFilter` на каждом запросе с `Bearer`-токеном проверяет подпись и срок
действия. Только после успешной проверки он помещает имя пользователя в security context.
`SecurityConfig` явно разрешает без токена лишь `/auth/login` и `/error`; все остальные
маршруты требуют аутентификацию. HTTP-сессии отключены.

CSRF отключён осознанно: API не использует cookie-аутентификацию и не принимает неявно
прикрепляемые браузером учётные данные. Клиент обязан явно передать bearer-токен.

### Валидация и ошибки

DTO ограничивают наличие и максимальную длину строк до обращения к БД. Ошибка входа одинакова
для неизвестного логина и неправильного пароля, поэтому API не помогает перебирать имена
пользователей. Стектрейсы и внутренние сообщения исключений наружу не возвращаются.

## Проверки

Локальные тесты:

```bash
mvn test
```

SAST:

```bash
mvn spotbugs:check
```

SCA:

```bash
mvn dependency-check:check
```

OWASP Dependency-Check использует базу NVD. Для стабильной и быстрой работы CI рекомендуется
добавить секрет репозитория `NVD_API_KEY`. Pipeline всё равно запускается без него, но первичная
загрузка базы может быть медленной или ограничиваться NVD.

GitHub Actions автоматически запускает тесты и обе security-проверки при `push` и
`pull_request`. HTML/JSON-отчёт Dependency-Check и XML-отчёт SpotBugs сохраняются в artifact
`security-reports`, в том числе при неуспешном сканировании.

## Сценарии, покрытые тестами

- успешная выдача JWT;
- неверный пароль и SQLi-строка при входе;
- отсутствие или повреждение bearer-токена;
- создание и чтение защищённых данных;
- удаление XSS-разметки;
- отклонение невалидного запроса;
- хранение BCrypt-хэша вместо открытого пароля.

## Материалы для отчёта

После публикации репозитория в `report/screenshots/` следует добавить снимки успешного запуска
GitHub Actions, SpotBugs и OWASP Dependency-Check. В отчёте также нужны ссылка на публичный
репозиторий и ссылка на последний успешный pipeline.
