# Лабораторная работа 2. И пришла Весна (Spring Boot)

**ФИО:** Данилов Кирилл  
**Группа:** Б9124-02.03.01мо  
**Предметная область:** Ветеринарная клиника (Pet Clinic)

---

## 1. Стек технологий

* **Язык:** Java 21
* **Фреймворк:** Spring Boot 3.3.5
* **Стартеры:**
  * `spring-boot-starter-web` (Spring MVC, встроенный Tomcat, Jackson)
  * `spring-boot-starter-validation` (валидация параметров)
  * `spring-boot-starter-test` (JUnit Jupiter / JUnit 5, AssertJ)
* **Сборщик:** Maven + `spring-boot-maven-plugin`
* **Ограничение:** сторонние библиотеки (включая Lombok) не используются — только стандартный JDK 21 и разрешённые стартеры Spring Boot.

---

## 2. Архитектура приложения

Проект организован по классической слоистой архитектуре с внедрением зависимостей через конструкторы (Spring Dependency Injection):

```
src/main/java/app/
├── PetClinicApplication.java      # Точка входа Spring Boot (@SpringBootApplication)
├── controller/                    # Слой REST-контроллеров (@RestController)
│   ├── PingController.java        # Проверка доступности (GET /ping)
│   ├── PetController.java         # CRUD и фильтрация питомцев (/pets)
│   └── AppointmentController.java # CRUD, переходы состояний и фильтрация (/appointments)
├── service/                       # Слой бизнес-логики (@Service)
│   ├── PetService.java            # Бизнес-правила для питомцев, валидация, каскадная очистка
│   └── AppointmentService.java    # Бизнес-правила записей, валидация, расчет пересечений интервалов
├── repository/                    # Слой доступа к данным (@Repository)
│   ├── PetRepository.java         # Интерфейс репозитория питомцев
│   ├── AppointmentRepository.java # Интерфейс репозитория записей
│   ├── InMemoryPetRepository.java # Thread-safe реализация на ConcurrentHashMap и AtomicLong
│   └── InMemoryAppointmentRepository.java
├── domain/                        # Доменные сущности и enum'ы
│   ├── Pet.java
│   ├── PetType.java               # CAT (прививка не нужна), DOG (прививка обязательна)
│   ├── Appointment.java
│   └── AppointmentStatus.java     # SCHEDULED, COMPLETED, CANCELLED
└── exception/                     # Доменные исключения и централизованная обработка
    ├── NotFoundException.java     # HTTP 404
    ├── ConflictException.java     # HTTP 409
    ├── BadRequestException.java   # HTTP 400
    ├── ErrorResponse.java         # Record ответа с ошибкой {"error": "..."}
    └── GlobalExceptionHandler.java# @RestControllerAdvice для всех ошибок
```

---

## 3. Бизнес-логика и правила предметной области

### Питомцы (Pet)
* **Поля:** `id`, `name`, `type` (`CAT` / `DOG`), `ownerName`, `vaccinated`.
* **Правила вакцинации:**
  * Кошкам (`CAT`) разрешено записываться на приём без прививки.
  * Собакам (`DOG`) без прививки запись запрещена — сервис возвращает `409 Conflict`.
* **Защита от обхода правила вакцинации через `PUT /pets/{id}`:**
  * Нельзя сделать собаку непривитой (`vaccinated: false`) или сменить тип питомца с кошки на непривитую собаку, если у него есть хотя бы одна активная запись на приём (`SCHEDULED`). Возвращается `409 Conflict`.
* **Удаление и целостность данных:**
  * Запрещено удалять питомца, если у него есть активные записи на приём (`SCHEDULED`) — возвращается `409 Conflict`.
  * При удалении питомца, у которого остались только завершённые (`COMPLETED`) или отменённые (`CANCELLED`) приёмы, выполняется **каскадное удаление** этих приёмов из репозитория. В системе не остаётся «висячих» записей с битыми ссылками на несуществующего питомца.

### Записи на приём (Appointment)
* **Поля:** `id`, `petId`, `vetName`, `dateTime` (ISO-8601), `durationMinutes`, `status`.
* **Валидация:**
  * Дата приёма должна быть строго в будущем (`400 Bad Request`).
  * Длительность приёма должна быть положительной (`> 0`) (`400 Bad Request`).
  * Имя врача и `petId` не могут быть пустыми (`400 Bad Request`).
* **Алгоритм проверки пересечения временных интервалов врача:**
  * Интервал новой записи $[S_{new}, E_{new})$ пересекается с существующей $[S_{old}, E_{old})$ тогда и только тогда, когда:
    $$\text{newStart} < \text{existingEnd} \quad \text{и} \quad \text{existingStart} < \text{newEnd}$$
  * Проверка выполняется регистронезависимо по имени врача (`equalsIgnoreCase`).
  * Отменённые записи (`CANCELLED`) освобождают таймслот врача — в это же время можно записать другого питомца.
  * Разные врачи могут вести приёмы одновременно.
  * Записи «встык» (с нулевым интервалом, например 10:00–10:30 и 10:30–11:00) не конфликтуют.
  * При обновлении записи (`PUT /appointments/{id}`) собственный ID записи исключается из проверки оверлапа, чтобы не возникало ложного конфликта с самой собой.
* **Жизненный цикл записей:**
  * Создание: запись всегда создаётся в статусе `SCHEDULED`.
  * Завершение (`POST /appointments/{id}/complete`): допустимо только из статуса `SCHEDULED`.
  * Отмена (`POST /appointments/{id}/cancel`): допустима только из статуса `SCHEDULED`.
  * Повторная отмена или завершение запрещены (`409 Conflict`).
  * Редактирование разрешено только для записей в статусе `SCHEDULED` (`409 Conflict`).

---

## 4. Как запустить

### Способ 1: через Maven
```bash
mvn spring-boot:run
```

### Способ 2: через исполняемый JAR
```bash
mvn clean package -DskipTests
java -jar target/2_laba_pet_clinic-1.0-SNAPSHOT.jar
```

### Способ 3: через IntelliJ IDEA
Открыть класс `app.PetClinicApplication` и нажать **Run** (`Shift + F10`).

После запуска сервер доступен по адресу: [http://localhost:8080](http://localhost:8080).

---

## 5. Тестирование

### Модульные тесты (JUnit 5 / Unit Tests)
Все модульные тесты сервисов написаны **без запуска контекста Spring** (на чистых POJO и in-memory репозиториях), как строго требует задание. Тесты выполняются за доли секунды (~0.2 сек суммарно).

* [`PetServiceTest`](src/test/java/app/service/PetServiceTest.java) — 15 тестов:
  * Валидация создания, обновления, фильтрация списков, удаление;
  * Защита от обхода вакцинации через `PUT /pets/{id}`;
  * Проверка каскадного удаления исторических приёмов при удалении питомца.
* [`AppointmentServiceTest`](src/test/java/app/service/AppointmentServiceTest.java) — 30 тестов:
  * Валидация полей, вакцинация собак, запись кошек без прививки;
  * 8 сценариев пересечения интервалов времени (включая граничные случаи 1 минута, 0 минут "встык", регистронезависимость);
  * Жизненный цикл (`complete`, `cancel`, повторные переходы, освобождение таймслота);
  * Обновление и предотвращение ложного оверлапа с самим собой.

Запуск всех модульных тестов:
```bash
mvn test
```

### Сквозные тесты REST API (curl)
В корне проекта находится исполняемый bash-скрипт [`requests.sh`](requests.sh), содержащий 12 подробных тестовых блоков, покрывающих все статус-коды (`200`, `201`, `204`, `400`, `404`, `405`, `409`) и бизнес-правила:
```bash
bash requests.sh
```
