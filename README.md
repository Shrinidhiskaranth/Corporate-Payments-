# Corporate Payments - Version 6 of 8: Audit log, admin and approval rules

A secure web application for raising, approving and paying corporate payment requests.
Built with **Spring Boot 3.3, JSF (Jakarta Faces 4 via JoinFaces), Spring Data JPA / Hibernate, Spring Security and MySQL**.

JAVA and Spring Framework Lab (CS3603-1), AY 2026-27, Section E.

This is version 6 of 8. Each version is a complete, runnable project that builds on the previous one; version 8 is the finished application. See `../VERSIONS.md` for the full list.

## New in version 6
- Audit log: every login, request change, decision, OTP and payment is recorded; admin audit page with filters; audit trail on each request
- Admin pages: create, activate and deactivate users; create departments with a budget limit
- Workflow rules set by the admin: up to a limit only the manager approves, above a threshold a DIRECTOR approves after finance
- DIRECTOR role and director desk; the route is fixed when a request is submitted and shown on the request page

Everything from the earlier versions is still here.

## What each role can do
| Role | What they can do |
| --- | --- |
| Employee | Create, edit, submit and cancel requests |
| Manager | Approve or reject requests from their own department |
| Finance | Approve within the department budget; pay with OTP |
| Director | Approve requests above the director threshold |
| Admin | Manage users, departments and approval rules; view the audit log |

Workflow: `DRAFT -> PENDING_MANAGER [-> PENDING_FINANCE [-> PENDING_DIRECTOR]] -> APPROVED -> PAID`; the steps depend on the amount (defaults: up to INR 10,000 manager only; up to INR 1,00,000 manager and finance; above that also the director).

## Demo users (password for all: `password123`)
| Email | Role | Department |
| --- | --- | --- |
| employee@corp.com | EMPLOYEE | Information Technology |
| manager@corp.com | MANAGER | Information Technology |
| employee.hr@corp.com | EMPLOYEE | Human Resources |
| manager.hr@corp.com | MANAGER | Human Resources |
| mvinayakrkamath@gmail.com | FINANCE | Finance |
| admin@corp.com | ADMIN | Finance |
| director@corp.com | DIRECTOR | Finance |

On first start, sample requests in different statuses are created. Turn this off with `app.seed.sample-requests=false`.

## Requirements
- JDK 17 or newer
- MySQL 8 running on localhost:3306
- Eclipse IDE for Enterprise Java (or Maven 3.9)

## How to run
1. In `src/main/resources/application.properties`, set `spring.datasource.username`
   and `spring.datasource.password` to your MySQL login.
   This version uses its own database, `corporate_payments_v6`, which is created automatically together with its tables
   (approvals, audit_logs, departments, payment_requests, transactions, users, workflow_settings), so the versions never mix their data.
2. Eclipse: File > Import > Maven > Existing Maven Projects, select this folder.
3. Right-click `CorporatePaymentsApplication.java` > Run As > Java Application
   (or run `mvnw spring-boot:run`, or double-click `run-app.bat` to start MySQL in Docker and the app).
4. Open http://localhost:8080 and click Sign in.

## OTP (simulated SMS)
No SMS gateway is connected. When a finance officer clicks "Send OTP", the code is written to the
application log as `[SIMULATED SMS]` and, because `app.otp.show-on-screen=true`, also shown in the
on-screen message so the demo works without a phone. Codes are 6 digits, valid 5 minutes, 3 attempts.

## Demo walkthrough
1. employee@corp.com: New request -> amount 250000 -> Save. The route is Manager -> Finance -> Director.
2. manager@corp.com approves, then finance approves, then director@corp.com approves on the Director desk.
3. Finance pays it with an OTP.
4. admin@corp.com: Users, Departments, Workflow rules and the Audit log.

## Tests
`mvn test` runs 43 tests on an in-memory H2 database (no MySQL needed).

## Project structure
```
src/main/java/com/nmamit/payments
  bean/        JSF backing beans: AdminBean, ApprovalBean, AuditBean, DashboardBean, FacesUtil, FormatBean, HomeBean, NewRequestBean, PaymentBean, RequestDetailBean, SettingsBean
  config/      security setup and demo data: DataSeeder, SecurityConfig
  controller/  web controllers: HomeController, ReceiptController
  model/       JPA entities and enums: Approval, ApprovalLevel, AuditAction, AuditLog, Decision, Department, PaymentCategory, PaymentRequest, PaymentTransaction, RequestStatus, Role, User, WorkflowSettings
  repository/  Spring Data JPA repositories: ApprovalRepository, AuditLogRepository, DepartmentRepository, PaymentRequestRepository, PaymentTransactionRepository, UserRepository, WorkflowSettingsRepository
  security/    login (user lookup, login audit): CustomUserDetailsService, LoginAuditListener
  service/     business rules: AdminService, ApprovalService, AuditService, CurrentUserService, NotAllowedException, OtpService, PaymentRequestService, PaymentService, ReceiptService, WorkflowException, WorkflowSettingsService
src/main/resources/META-INF/resources   JSF pages (.xhtml)
src/test/java                            JUnit 5 tests
```

## Next version
Version 7 adds vendors, contracts, procurement and the vendor portal.
