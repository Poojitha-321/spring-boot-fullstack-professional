# HR Management System - Construction Workforce

Forked from [amigoscode/spring-boot-fullstack-professional](https://github.com/amigoscode/spring-boot-fullstack-professional) — chosen for its clean Spring Boot + JPA + PostgreSQL structure, easy to extend without rewriting.

---

## Tech Stack
- Java 17 + Spring Boot 2.4.3
- Hibernate/JPA + PostgreSQL (Supabase)
- Redis (active workers cache)

---

## Setup Instructions

### 1. Prerequisites
- Java 17
- Maven
- Docker (for Redis)

### 2. Clone and configure
```bash
git clone https://github.com/Poojitha-321/spring-boot-fullstack-professional
cd spring-boot-fullstack-professional
```

Edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://<your-supabase-host>:5432/postgres
spring.datasource.username=<your-username>
spring.datasource.password=<your-password>
```

### 3. Start Redis
```bash
docker run -d --name redis -p 6379:6379 redis
```

### 4. Run the app
```bash
./mvnw spring-boot:run -P '!build-frontend'
```

App runs on `http://localhost:8080`

### Supabase Connection Setup
- Use port **5432** (direct connection), not 6543
- Username format: `postgres.<project-ref>`
- Get connection string from Supabase → Connect → Transaction pooler

---

## API Endpoints

### Workers
```bash
# Create worker
curl -X POST http://localhost:8080/api/workers \
  -H "Content-Type: application/json" \
  -d '{"name":"Raju Kumar","phone":"9876543210","designation":"MASON","dailyWageRate":600.0,"active":true}'

# Get all workers
curl http://localhost:8080/api/workers
```

### Sites
```bash
# Create site
curl -X POST http://localhost:8080/api/sites \
  -H "Content-Type: application/json" \
  -d '{"siteName":"Greenfield Phase 2","location":"Hyderabad","active":true}'
```

### Attendance
```bash
# Clock in
curl -X POST http://localhost:8080/api/attendance/clock-in \
  -H "Content-Type: application/json" \
  -d '{"workerId":1,"siteId":1}'

# Clock out
curl -X POST http://localhost:8080/api/attendance/clock-out \
  -H "Content-Type: application/json" \
  -d '{"workerId":1}'

# Active workers (from Redis)
curl http://localhost:8080/api/attendance/active

# Attendance history (paginated)
curl "http://localhost:8080/api/attendance/log?workerId=1&from=2026-06-01T00:00:00&to=2026-06-30T23:59:59"
```

### Overtime
```bash
# Monthly summary
curl "http://localhost:8080/api/overtime/summary/1?month=2026-05"

# Settle overtime (only past months)
curl -X POST "http://localhost:8080/api/overtime/settle/1?month=2026-05"
```

---

## Design Decisions

### Schema
- `AttendanceLog` uses explicit `@Column(name=...)` to match Hibernate naming strategy for indexes
- `OvertimeEntry` stores calculated amount at time of creation — not recalculated on read
- Indexes on `worker_id` and `clock_in_time` for fast date-range queries

### Caching Strategy
- Only active workers are cached in Redis — not attendance history or overtime data
- Redis hash key `active_workers` with worker ID as field
- 16-hour TTL as safety net for missed clock-outs
- Graceful degradation: app starts and serves requests even when Redis is offline

### Overtime Calculation
- Standard shift = 8 hours
- First 2 overtime hours → 1.5x hourly rate
- Beyond 2 overtime hours → 2x hourly rate
- Monthly cap of 60 hours enforced at clock-out time
- Hourly rate derived from daily wage ÷ 8

### Transactions
- Settlement is fully atomic — all entries for a worker+month settle together or none do
- SMS notification fires via `@TransactionalEventListener(AFTER_COMMIT)` — never fires if DB rolls back

### Connection Pooling
- HikariCP max-lifetime set to 270s (under Supabase's 300s idle timeout)
- keepalive-time set to 120s to prevent stale connections
- Pool size capped at 5 for Supabase free tier limits

### Things I'd do differently with more time
- Add Spring Security with JWT authentication
- Add worker profile update endpoint with Redis cache invalidation
- Add staging-specific `application-staging.yml` profile
- Add integration tests for overtime calculation edge cases
- Move to DTO pattern instead of returning entities directly

---

## AI Tools Used
- **Claude (Anthropic)** — entity design, service logic, debugging Hibernate/Redis issues, fixing compilation errors
- Used AI for boilerplate (entities, repositories, controllers) and wrote/understood business logic (overtime calc, settlement, Redis strategy) manually
