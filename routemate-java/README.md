# RouteMate — Full-Stack Java + React

**Carpooling & parcel-delivery platform**: Spring Boot 3.3.5 backend (Java 17,
JWT security, MySQL) + responsive React 18 frontend (Vite, React-Bootstrap /
Bootstrap 5), with a complete JUnit 5 QA suite covering **TC-01 → TC-08**.

```
routemate-java/
├── backend/    Spring Boot 3.3.5 · Java 17 · Maven · MySQL (H2 for tests/demo)
└── frontend/   React 18 · Vite · React-Bootstrap 5 · react-router 6
```

## 1. Backend (Spring Boot)

```bash
cd backend
mvn spring-boot:run            # MySQL on localhost:3306 (user/pass: root/root)
# — or, zero-setup demo mode (in-memory H2):
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

- Runs on **http://localhost:8081**
- MySQL settings live in `src/main/resources/application.yml`
  (env overrides: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`).
  The schema is created automatically (`ddl-auto: update`).
- JWT secret: `ROUTEMATE_JWT_SECRET` (dev default included).

### API contract (same as the blueprint)

| Method & path | Role | Purpose |
| :-- | :-- | :-- |
| `POST /api/v1/auth/register` | public | `{name, email, password, role}` |
| `POST /api/v1/auth/login` | public | → `{token, userId, role, name}` |
| `POST /api/v1/trips` | DRIVER | publish trip (≥ 2 `routeWaypoints` `{lng,lat}`) |
| `POST /api/v1/trips/search` | any | corridor search `{pickupLat, pickupLng, dropLat, dropLng}` |
| `GET /api/v1/trips/driver/{driverId}` | DRIVER | driver's trip feed |
| `POST /api/v1/bookings` | PASSENGER | SEAT / PARCEL booking → `{bookingId, calculatedFare, otpCode}` |
| `GET /api/v1/bookings/trip/{tripId}` | DRIVER | bookings of the driver's trip |
| `POST /api/v1/bookings/{id}/verify-otp` | DRIVER | OTP settlement → `{"message":"VERIFIED"}` |

**Fares (server-authoritative):** SEAT = `baseFare × bookedSeats`;
PARCEL = `baseFare + kg × ₹15`. Every booking gets a 4-digit `otpCode`.

**Corridor search** is pure-Java geospatial maths (haversine + point-to-polyline):
pickup and drop must both lie within 50 km of the trip polyline, with pickup
before drop along the route.

**Security:** stateless JWT (BCrypt passwords), role checks with `@PreAuthorize`,
403 JSON for missing tokens / role mismatch (TC-03), CORS for the Vite dev
server and Live Server origins.

## 2. Frontend (React + React-Bootstrap)

```bash
cd frontend
npm install
npm run dev          # http://localhost:5173  (proxies /api → :8081)
npm run build        # production bundle in dist/
```

Pages: `login` (sign in / sign up with role picker), `driver` (publish trips
with **preset city dropdowns**, trip feed, bookings + **OTP verification box**),
`passenger` (corridor search, SEAT/PARCEL toggle, **live fare estimate**,
prominent **fare + 4-digit OTP card**). All forms validate **before any fetch**
(latitude −90…90 etc. — TC-05), and 401/403/network failures show distinct
messages (never a generic "Network Error").

## 3. QA suite — JUnit 5 (TC-01 → TC-08)

```bash
cd backend
mvn test
```

| Test class | Scenario |
| :-- | :-- |
| `Tc01CorsPreflightTest` | OPTIONS preflight from `127.0.0.1:5500` → CORS headers, no block |
| `Tc02AuthLoginTest` | valid driver login → JWT + profile; wrong password → 401 |
| `Tc03SecurityNegativeTest` | `POST /trips` without token / with PASSENGER token → **403** |
| `Tc04PublishTripTest` | Pune→Mumbai LineString → 200 + `tripId`, appears in driver feed |
| `Tc05InvalidCoordinateTest` | `lat: 453` → 400 with range message; 1-waypoint → 400 |
| `Tc06BookSeatTest` | SEAT booking → fare `= baseFare×seats`, 4-digit OTP |
| `Tc07BookParcelTest` | PARCEL 5 kg → `bookedSeats 0`, boot 15→10 kg, OTP |
| `Tc08FullCycleTest` | Register→Publish→Register→Search→Book→Capture OTP→**Verify OTP** |

Tests run against the full HTTP stack (security filters + CORS + JSON) on a
random port with an in-memory H2 database — no external services needed.

## 4. Suggested demo flow

1. `mvn spring-boot:run -Dspring-boot.run.profiles=h2` (backend) and `npm run dev` (frontend).
2. Register a **Driver** → publish the default Pune→Mumbai trip.
3. Register a **Passenger** → search Pune→Mumbai → book a seat → note fare + OTP.
4. Switch back to the driver → *View Bookings & Verify OTP* → enter the OTP → settled.
5. `mvn test` to print the green TC-01…TC-08 report.
