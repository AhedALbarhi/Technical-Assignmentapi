# Library Resource Booking & Reservation Queue API

## Overview

This project is a REST API for managing library resources such as books, equipment, and AV kits.

The system allows employees to:

* Borrow available resources
* Return borrowed resources
* Join waitlists when resources are unavailable
* Claim reservations when resources become available
* View their current borrowing and reservation activity
* Leave waitlists voluntarily before it's their turn

Built using:

* Java 17
* Spring Boot 3.2.5
* Spring Data JPA
* PostgreSQL
* Maven
* Docker
* DBVisualizer

---

# Technology Stack

| Technology      | Purpose                    |
| --------------- | -------------------------- |
| Spring Boot     | REST API framework         |
| Spring Data JPA | Database access            |
| PostgreSQL      | Relational database        |
| Maven           | Dependency management      |
| Docker          | Database containerization  |
| DBVisualizer    | Database visualization     |
| Lombok          | Boilerplate code reduction |

---

# Running the Project Locally

## Step 1 — Start PostgreSQL with Docker

The database is defined using a `docker-compose.yml` file:

```yaml
-------
```

Run the container:
```bash
docker-compose up -d
```

Verify it is running:
```bash
docker ps
```

You should see `postgres-db` with status `Up`.

---

## Step 2 — Configure Application Properties

Located at `src/main/resources/application.properties`:

```properties
--------
```

---

## Step 3 — Build the Project

```bash
mvn clean install
```

---

## Step 4 — Run the Application

Run the main class `LibraryApiAppApplication.java` in IntelliJ,


Application starts at:
```
http://localhost:8080
```

Tables are created automatically by JPA on startup:
```
employee
resource
borrow
waitlist_entry
reservation
```

---

## Step 5 — Database Visualization

Connect DBVisualizer using:

| Setting  | Value       |
| -------- | ----------- |
| Host     | localhost   |
| Port     | 5432        |
| Database | mydatabase  |
| Username | postgres    |
| Password | 299991login |

---

# API Endpoints

## Employees

### Create Employee
```http
POST /api/employees
```
Body:
```json
{
  "name": "Ali",
  "email": "ali@co.com"
}
```
Response: `201 Created`

---

### View Employee Activity
```http
GET /api/employees/{employeeId}/activity
```
Returns active borrows and pending reservations for the employee.

---

## Resources

### Create Resource
```http
POST /api/resources
```
Body:
```json
{
  "title": "Clean Code",
  "type": "BOOK",
  "totalCopies": 1
}
```
Supported types: `BOOK`, `EQUIPMENT`, `AV_KIT`

Response: `201 Created`

---

### Borrow Resource
```http
POST /api/resources/{resourceId}/borrow?employeeId={employeeId}
```
Behavior:
* If copies are available → borrows immediately, `availableCopies` decreases by 1
* If no copies available → employee joins the waitlist automatically

---

### Return Resource
```http
POST /api/resources/{resourceId}/return?employeeId={employeeId}
```
Behavior:
* Removes the active borrow record
* If waitlist has employees → creates a 24hr reservation for the first in line
* If waitlist is empty → `availableCopies` increases by 1

---

### View Waitlist
```http
GET /api/resources/{resourceId}/waitlist
```
Returns the waitlist in FIFO order (ordered by `joined_at` timestamp).

---

### Leave Waitlist
```http
DELETE /api/resources/{resourceId}/waitlist/{employeeId}
```
Removes the employee from the waitlist voluntarily.

---

## Reservations

### Claim Reservation
```http
POST /api/reservations/{reservationId}/claim?employeeId={employeeId}
```
Rules:
* Only the reservation owner can claim it
* Must be claimed within 24 hours of creation
* Converts the reservation into an active borrow

---

### Process Expired Reservations
```http
POST /api/reservations/process-expired
```
Checks all unclaimed reservations. For each expired one:
* Deletes the expired reservation
* Creates a new reservation for the next employee in the waitlist
* If waitlist is empty → `availableCopies` increases by 1

This endpoint handles the full expiry cascade chain.

---

# Business Rules

## Borrowing

| Rule | Description | Implementation |
|------|-------------|----------------|
| Rule 1 | Employee cannot borrow the same resource twice | `findByEmployeeAndResource()` check in `BorrowService` |
| Rule 2 | Available copies decrease on borrow | `availableCopies - 1` saved to database |
| Rule 3 | Available copies increase on return only if no waitlist | Counter only increases when waitlist is empty |

## Waitlist

| Rule | Description | Implementation |
|------|-------------|----------------|
| Rule 4 | Auto-join waitlist when no copies available | Handled in `borrow()` method |
| Rule 5 | Employee cannot join same waitlist twice | `existsByEmployeeAndResource()` check |
| Rule 6 | Waitlist is strictly FIFO | `findByResourceOrderByJoinedAtAsc()` |
| Rule 7 | Employee can leave waitlist voluntarily | `DELETE /waitlist/{employeeId}` endpoint |

## Reservation

| Rule | Description | Implementation |
|------|-------------|----------------|
| Rule 8 | First in waitlist gets reservation when copy freed | `waitlist.get(0)` in `returnResource()` |
| Rule 9 | Reservation expires after 24 hours | `expiresAt = now + 24 hours` |
| Rule 10 | Only reservation owner can claim | Employee ID check in `claimReservation()` |
| Rule 11 | Reserved copy is locked — not available to others | `availableCopies` not increased when reservation created |
| Rule 12 | Expiry cascades to next in queue | `processExpired()` loops and reassigns |

---

# Expiry and Queue Advancement (Section 3.5)

This is the core challenge of the assignment. A flat `reserved_by` field cannot represent a queue, a FIFO waitlist order, or a chain of expiries.

## How it is solved

Three separate entities handle this:

**WaitlistEntry** stores `joined_at` timestamp → enables strict FIFO ordering.

**Reservation** stores `expires_at` and `claimed` flag → enables time-limited holds.

**The chain is handled by `processExpired()` method inside `BorrowService`:**

```
Reservation expires unclaimed
    → delete expired reservation
    → check waitlist for this resource (ordered by joined_at)
    → if someone waiting:
        → remove them from waitlist
        → create NEW reservation with fresh 24hr window
        → chain continues if they also let it expire
    → if nobody waiting:
        → availableCopies + 1 (copy truly free)
```

# Design Decisions

## 5 separate entities

Instead of storing reservation information directly on the Resource entity:

| Entity | Purpose |
|--------|---------|
| `Employee` | Person borrowing resources |
| `Resource` | Item with limited physical copies |
| `Borrow` | Active borrow record — who holds what |
| `WaitlistEntry` | Queue entry with FIFO timestamp |
| `Reservation` | Time-limited hold with expiry and claimed flag |

This design supports FIFO queues, reservation expiry, cascading reservations, and is queryable and consistent under chained expiries.

## Why business logic lives in the service layer

All business rules are in `BorrowService`. Controllers only receive requests and call the service. This keeps the controller thin and the business logic testable and centralized.

## Transaction management

Critical operations use `@Transactional` to ensure database consistency:
* Returning a resource and creating a reservation happen atomically
* Processing expired reservations and advancing the queue happen atomically

---

# Assumptions Made

### Assumption 1 — Employee removed from waitlist when reservation created
Once a reservation is created for an employee, they are immediately removed from the waitlist. They have effectively reached the front of the queue.

### Assumption 2 — One pending reservation per resource at a time
Only one unclaimed reservation can exist per resource at a time. The copy is held for that employee until claimed or expired.

### Assumption 3 — Manual expiry processing instead of scheduler
Section 3.5 explicitly permits this. `POST /api/reservations/process-expired` triggers the expiry check and queue cascade manually.

### Assumption 4 — Borrow history not required
The specification only discusses active borrows. Returned borrow records are deleted. No history table is maintained.

### Assumption 5 — Return immediately creates reservation for next in waitlist
A returned copy does not temporarily increase `availableCopies` if someone is waiting. The copy goes directly into a reservation for the first person in line.

---

# Author

Graduate Assignment — Library Resource Booking & Reservation Queue API

Built using Java 17, Spring Boot 3.2.5, PostgreSQL, Docker, Maven, and DBVisualizer.