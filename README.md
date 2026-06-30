# Coupon Service

REST serwis do zarządzania kuponami rabatowymi — zadanie rekrutacyjne Empik.

---

## Uruchomienie

### Wymagania

- Java 21
- Docker (do lokalnego PostgreSQL)

### Start lokalny

```bash
# 1. Uruchom bazę danych
docker-compose up -d

# 2. Uruchom aplikację
./gradlew bootRun
```

Serwis działa na `http://localhost:8080`.

### Testy

Testy są rozdzielone na dwa rodzaje:

```bash
# Testy jednostkowe (domena, aplikacja, REST, WireMock) — szybkie, NIE wymagają Dockera
./gradlew test

# Testy integracyjne (Testcontainers: PostgreSQL + pełny @SpringBootTest) — wymagają Dockera
./gradlew integrationTest
```

`./gradlew build` uruchamia tylko testy jednostkowe, więc przechodzi bez działającego Dockera.
Testy integracyjne żyją w osobnym source secie (`src/integrationTest/java`) i odpala się je
świadomie — lokalnie przed pushem lub w pipelinie CI, gdzie Docker jest dostępny.

---

## API

### Utwórz kupon

```http
POST /api/v1/coupons
Content-Type: application/json

{
  "code": "WIOSNA",
  "maxUsages": 100,
  "countryCode": "PL"
}
```

**Odpowiedzi:** `201 Created`, `400 Bad Request`, `409 Conflict` (kod zajęty)

### Użyj kuponu

```http
POST /api/v1/coupons/{code}/usages
Content-Type: application/json

{
  "userId": "user-123"
}
```

Kraj użytkownika określany jest na podstawie IP (header `X-Forwarded-For` lub IP połączenia).

**Odpowiedzi:** `200 OK`, `404 Not Found`, `403 Forbidden` (zły kraj), `409 Conflict` (wyczerpany / już użyty)

### Szczegóły kuponu

```http
GET /api/v1/coupons/{code}
```

---

## Architektura

### Hexagonal Architecture (Ports & Adapters)

```
domain/        ← czysta Java, zero zależności frameworkowych
  model/       ← Coupon (Aggregate Root), CouponCode, CountryCode, UserId, CouponId
  port/        ← CouponRepository, GeoLocationPort (interfejsy wyjściowe)
  exception/   ← wyjątki domenowe

application/   ← CQRS, orkiestracja domeny
  CouponFacade ← jedyny punkt wejścia dla adapterów (ukrywa handlery)
  command/     ← CreateCouponCommandHandler, UseCouponCommandHandler
  query/       ← GetCouponQueryHandler (read model: CouponView)

adapter/       ← implementacje portów
  rest/        ← CouponController, GlobalExceptionHandler (RFC 7807 Problem Details)
  persistence/ ← JpaCouponRepositoryAdapter, CouponJpaEntity (@Version)
  geolocation/ ← IpApiGeoLocationAdapter (ip-api.com)
```

### Kluczowe decyzje architektoniczne

#### PostgreSQL zamiast MongoDB
- Requirement „kto pierwszy ten lepszy" wymaga silnych gwarancji ACID.
- `@Version` (optimistic locking) na encji JPA zapewnia bezpieczeństwo przy jednoczesnych żądaniach — wyścig przegrywa z HTTP 409, klient może ponowić.
- `PRIMARY KEY (coupon_id, user_id)` w tabeli `coupon_usages` jako dodatkowe zabezpieczenie przed duplikatami na poziomie bazy.
- `UNIQUE` na `coupons.code` + normalizacja do uppercase w Value Object zapewnia case-insensitive uniqueness bez potrzeby osobnego indeksu z funkcją.

#### DDD — Coupon jako Aggregate Root
- `Coupon.use()` enkapsuluje wszystkie reguły biznesowe: weryfikacja kraju → sprawdzenie limitu → sprawdzenie czy użytkownik już użył kuponu.
- Kolejność walidacji jest celowa: kraj sprawdzamy pierwszy, żeby użytkownik z niedozwolonego kraju nie wiedział o stanie limitu.
- Value Objects (`CouponCode`, `CountryCode`, `UserId`) enkapsulują reguły walidacji — niemożliwe jest stworzenie niepoprawnego stanu.

#### Optimistic Locking
- `@Version` na `CouponJpaEntity` → przy konflikcie JPA rzuca `OptimisticLockingFailureException`.
- Handler w `GlobalExceptionHandler` zwraca `409 Conflict` z sugestią retry.
- Alternatywą byłby `SELECT FOR UPDATE` (pessimistic lock), ale to wąskie gardło przy popularnych kuponach — optimistic locking jest bardziej skalowalny.

#### Geo-lokalizacja
- Adapter `IpApiGeoLocationAdapter` wywołuje `http://ip-api.com/json/{ip}` (bezpłatna, nie wymaga API key).
- Prywatne adresy IP (localhost, intranety) są obsługiwane konfiguralnie: `coupon.geo.bypass-private-ips=true` zwraca domyślny kraj (przydatne w środowiskach dev).
- Niedostępność serwisu geo-lokalizacji → HTTP 503.

#### CQRS
- **Commands** (`Create`, `Use`) są `@Transactional` i modyfikują stan przez agregat.
- **Queries** są `@Transactional(readOnly = true)` i zwracają `CouponView` (read model) — izolacja od modelu domenowego.

#### CouponFacade — fasada warstwy aplikacji
- Adapter REST wstrzykuje **wyłącznie `CouponFacade`** zamiast trzech osobnych handlerów. Kontroler zależy od jednej, stabilnej abstrakcji, a złożoność CQRS (które handlery, jak je wołać) zostaje ukryta w warstwie aplikacji.
- Fasada tylko deleguje — granice transakcji pozostają na handlerach (osobne beany, więc proxy Springa działa).
- `IpExtractor` to bezstanowy statyczny helper w adapterze REST (operuje na `HttpServletRequest`), świadomie **nie** trafia do fasady — warstwa aplikacji musi pozostać wolna od zależności webowych (`jakarta.servlet`).

### Skalowalność
- Bezstanowy serwis — można uruchomić wiele instancji za load balancerem.
- Optimistic locking nie blokuje wierszy, więc wiele instancji może bezpiecznie operować na tej samej bazie.
- Connection pool (HikariCP) skonfigurowany z sensownymi wartościami domyślnymi.

### Znane ograniczenia i możliwe ulepszenia
1. **`@ElementCollection` i N+1 przy zapisie** — przy każdym `save()` JPA usuwa i wstawia na nowo wszystkie wiersze `coupon_usages`. Dla kuponów z dużą liczbą użyć można zoptymalizować przez bezpośredni `INSERT INTO coupon_usages` w adapterze.
2. **Retry na optimistic lock** — dla lepszego UX można dodać automatyczny retry w `UseCouponCommandHandler` (np. Spring Retry). Celowo pominięty, by nie ukrywać faktu konfliktu przed klientem — API jest transparentne.
3. **Rate limiting** — bez ograniczenia liczby żądań na IP, co w produkcji wymagałoby np. Redis + Bucket4j.
4. **Obsługa timeoutu geo-lokalizacji** — aktualnie `RestClient` używa domyślnego timeoutu JVM. W produkcji należy skonfigurować `connectTimeout` i `readTimeout`.

---

## Technologie

| Warstwa | Technologia |
|---------|-------------|
| Framework | Spring Boot 3.4 |
| Baza danych | PostgreSQL 16 |
| Migracje | Flyway |
| ORM | Spring Data JPA / Hibernate |
| HTTP Client (geo) | Spring `RestClient` |
| Testy jednostkowe | JUnit 5, Mockito |
| Testy integracyjne | Testcontainers (PostgreSQL), WireMock |
| Build | Gradle (Kotlin DSL) |
| Java | 21 |
