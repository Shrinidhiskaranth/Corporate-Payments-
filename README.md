# Corporate Payments - Version 2 of 8: Employee payment requests

A secure web application for raising, approving and paying corporate payment requests.
Built with **Spring Boot 3.3, JSF (Jakarta Faces 4 via JoinFaces), Spring Data JPA / Hibernate, Spring Security and MySQL**.

JAVA and Spring Framework Lab (CS3603-1), AY 2026-27, Section E.

This is version 2 of 8. Each version is a complete, runnable project that builds on the previous one; version 8 is the finished application. See `../VERSIONS.md` for the full list.

## New in version 2
- `PaymentRequest` entity with request numbers like PR-2026-0001
- Employees create a request (title, amount, category, beneficiary, due date), save it as a draft, edit it, submit it or cancel it
- Status flow: `DRAFT -> PENDING_MANAGER`, and `CANCELLED`
- Request detail page with view rules (employees see their own, managers their department's, finance and admin everything)
- Managers see the submitted requests of their department on the dashboard

Everything from the earlier versions is still here.

## What each role can do
| Role | What they can do |
| --- | --- |
| Employee | Create requests (draft or submit now), edit drafts, submit, cancel, track status |
| Manager | See the submitted requests of their department |
| Finance / Admin | See request totals; open any request |

Workflow: `DRAFT -> PENDING_MANAGER` (the employee can cancel while `DRAFT` or `PENDING_MANAGER`).

## Demo users (password for all: `password123`)
| Email | Role | Department |
| --- | --- | --- |
| employee@corp.com | EMPLOYEE | Information Technology |
| manager@corp.com | MANAGER | Information Technology |
| employee.hr@corp.com | EMPLOYEE | Human Resources |
| manager.hr@corp.com | MANAGER | Human Resources |
| mvinayakrkamath@gmail.com | FINANCE | Finance |
| admin@corp.com | ADMIN | Finance |

On first start, sample requests in different statuses are created. Turn this off with `app.seed.sample-requests=false`.

## Requirements
- JDK 17 or newer
- MySQL 8 running on localhost:3306
- Eclipse IDE for Enterprise Java (or Maven 3.9)

## How to run
1. In `src/main/resources/application.properties`, set `spring.datasource.username`
   and `spring.datasource.password` to your MySQL login.
   This version uses its own database, `corporate_payments_v2`, which is created automatically together with its tables
   (departments, payment_requests, users), so the versions never mix their data.
2. Eclipse: File > Import > Maven > Existing Maven Projects, select this folder.
3. Right-click `CorporatePaymentsApplication.java` > Run As > Java Application
   (or run `mvnw spring-boot:run`, or double-click `run-app.bat` to start MySQL in Docker and the app).
4. Open http://localhost:8080 and click Sign in.

## Demo walkthrough
1. employee@corp.com: New request -> amount 15000 -> Save. It shows as PENDING_MANAGER.
2. Untick "Submit for approval now" to keep a draft; edit it, then Submit or Cancel from the dashboard.
3. manager@corp.com: the dashboard lists the IT department's submitted requests (manager.hr@corp.com does not see them).

## Tests
`mvn test` runs 19 tests on an in-memory H2 database (no MySQL needed).

## Project structure
```
src/main/java/com/nmamit/payments
  bean/        JSF backing beans: DashboardBean, FacesUtil, HomeBean, NewRequestBean, RequestDetailBean
  config/      security setup and demo data: DataSeeder, SecurityConfig
  controller/  web controllers: HomeController
  model/       JPA entities and enums: Department, PaymentCategory, PaymentRequest, RequestStatus, Role, User
  repository/  Spring Data JPA repositories: DepartmentRepository, PaymentRequestRepository, UserRepository
  security/    login (user lookup): CustomUserDetailsService
  service/     business rules: CurrentUserService, NotAllowedException, PaymentRequestService, WorkflowException
src/main/resources/META-INF/resources   JSF pages (.xhtml)
src/test/java                            JUnit 5 tests
```

## Next version
Version 3 adds manager approval.
