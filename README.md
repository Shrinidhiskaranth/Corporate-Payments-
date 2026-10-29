# Corporate Payments Approval and Management System — Phase 1

Spring Boot 3.3 + JSF (JoinFaces) + Spring Data JPA + Spring Security + MySQL.

## What Phase 1 contains
- Maven project with all dependencies
- Enums: Role, RequestStatus, PaymentCategory
- Entities: Department, User, PaymentRequest (with created/updated timestamps)
- Repositories for all three entities
- BCrypt password encoder and a basic security setup
- Seed data: 3 departments and 1 user per role (password for all: `password123`)
  - admin@corp.com, finance@corp.com, manager@corp.com, employee@corp.com
- JSF home page showing counts and departments (`/index.xhtml`)
- REST endpoint `/api/summary` returning record counts as JSON
- Automated test using an in-memory H2 database

## Requirements
- JDK 17 or newer
- MySQL 8 running on localhost:3306
- Eclipse IDE for Enterprise Java (or Maven 3.9 on the command line)

## How to run
1. Open `src/main/resources/application.properties` and set
   `spring.datasource.username` and `spring.datasource.password` to your MySQL login.
   The `corporate_payments` database is created automatically.
2. In Eclipse: File > Import > Maven > Existing Maven Projects, select this folder, Finish.
   Wait for Maven to download dependencies (needs internet, first time only).
3. Right-click `CorporatePaymentsApplication.java` > Run As > Java Application.
4. Open http://localhost:8080 in your browser.
   - Home page: http://localhost:8080/index.xhtml
   - JSON check: http://localhost:8080/api/summary

Command line alternative: `mvn spring-boot:run` (or `mvnw` if you add the wrapper).
Run the tests with `mvn test` (no MySQL needed for tests).

## Checking the database
In MySQL Workbench or the MySQL client:
```
USE corporate_payments;
SHOW TABLES;            -- departments, users, payment_requests
SELECT * FROM users;    -- passwords are stored as BCrypt hashes
```

## Next phase
Approval, Transaction and AuditLog entities; database login with role rules;
service layer; JSF pages for request creation and approvals; OTP verification.
# Corporate-Payments-
