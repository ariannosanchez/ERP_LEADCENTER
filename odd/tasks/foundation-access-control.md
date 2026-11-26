# Feature: foundation-access-control

First slice of the LeadCenter ERP: monorepo foundation (Spring Boot API + React SPA), authentication, users, configurable roles and permissions.
Later slices (lead webhook + pipeline, lead→client conversion, quotes/contracts/receipts/sales) build on this one and are out of scope here.

## Specs

- **S1 — Stack.** User: "En el backend vamos a utilizar java spring boot y para el frontend pienso en algo como react". Agreed: Java 25 + Spring Boot 4.2 + Spring Modulith (Gradle wrapper) in `backend/`; React + Vite + TypeScript SPA in `frontend/`; PostgreSQL with Flyway migrations; Docker Compose for local services.
- **S2 — Robustness.** User: "este debe ser un sistema escalable y robusto." Agreed meaning: modular monolith; each business domain is a Spring Modulith module whose boundaries are verified by a test; schema changes only through versioned Flyway migrations.
- **S3 — Login.** User selected: "Email + contraseña". Accounts are created only by an administrator; there is no public sign-up. Session via httpOnly cookie issued by the backend; passwords stored hashed, never returned by the API.
- **S4 — Initial roles.** User selected: "Administrador", "Comercial / Ventas". Both roles exist after the first migration. An initial Administrador account is created at first start from environment configuration (never a hard-coded password).
- **S5 — Configurable roles.** User selected: "Configurables (recomendado)" — "El admin crea roles y les asigna permisos granulares (ej. leads.ver, leads.asignar)". Permissions come from a fixed catalog defined in code; roles and their permission sets are data managed through the API. The system must never be left without an active user holding user/role administration permissions.
- **S6 — Ownership scope.** User selected for leads: "Solo los suyos" — "Gerencia/admin ven todos; protege la cartera de cada comercial". This slice only defines the catalog entries that express it (a "view own" vs "view all" permission pair for leads); enforcement on leads belongs to the leads slice.
- **S7 — User management.** An administrator can list, create, edit, deactivate/reactivate users, assign roles, and set a new password for a user. A deactivated user cannot log in.
- **S8 — Frontend.** Login screen, authenticated layout, and admin screens for users and roles, consuming a client generated from the backend OpenAPI contract.
- **S9 — Exclusion.** User: "no usaremos nada de n8n ... no quiero que se mezcle en lo absoluto en este proyecto".

## Tasks

- [ ] T1 — S1, S2, S9 — Scaffold monorepo: `backend/` (Spring Boot, Modulith, Flyway, Postgres via Compose, health endpoint, modulith verification test), `frontend/` (Vite React TS), root README and `.gitignore`. Route: inline. Commit: —
- [ ] T2 — S4, S5, S6 — Identity schema + permission catalog + seeded roles + bootstrap admin from env. Route: inline, Risk item 2. Commit: —
- [ ] T3 — S3 — Authentication: login/logout/current-user endpoints, session cookie, CSRF, password hashing, inactive users rejected. Route: inline, Risk item 2 → verify. Commit: —
- [ ] T4 — S5, S7 — Users and roles administration API with permission checks and last-admin protection; OpenAPI published. Route: inline, Risk item 2 → verify. Commit: —
- [ ] T5 — S8 — Frontend: generated API client, login, layout, users and roles screens. Route: inline. Commit: —
- [ ] T6 — S1–S9 — Docs (run/setup), full verification pass. Route: gentle-ai-verify. Commit: —

## Log

- L1 (user, original request, verbatim): "Tercero, los modulos a trabajar inicialmente son Leads, Clientes, Ventas, Usuarios, Permisos, es decir todo parte desde la llegada de un lead que provienen de los formularios que utilizamos para generar prospectos que puede ser de meta ads, google ads, etc, la cuestión es que esa data llega a nuestra herramienta de automatización make y nosotros lo jalamos hacia un googlesheet para que el área comercial pueda contactarse con los prospectos y hacer lo propio, que es basicamente contactarse y generar una cita y es ahi donde se coordina una cita que en su mayoria es precencial, llamada de telefono o reunión virtual a traves de meet, etc, pasado esto si la cita logra ser efectiva y se da generan una cotización de los servicios de interes del cliente para luego enviarselo si este acepta, se le realiza un contrato formal para que pueda leerlo y firmarlo, asi mismo se le genera la facturación que puede ser RHE o Factura en donde se espera que este abone para activar el servicio,  luego de todo esto  lo registran en un spreadsheet cada venta que realiza este es otra hoja de calculo, donde registran datos del cliente, descripción del plan de las áreas que intervienen para ello, además de subir los archivos adjuntos como brief, cotizaciones, contrato formal y comprobante de pago."
- L2 (user): "Estamos usando Next.js para todo, sin embargo nos estamos olvidando del backend, actualmente solo te he mostrado una porción de lo que es el negocio y su necesidad de urgencia aun faltan modulos por definir, e implementar, por lo que este debe ser un sistema escalable y robusto."
- L3 (user): "En el backend vamos a utilizar java spring boot y para el frontend pienso en algo como react, que opinas?" — then confirmed prior Java/Spring Boot experience.
- L4 (decision): slice order agreed — this slice first (foundation + access control), then lead webhook + pipeline with appointments, then lead→client conversion. SUNAT e-invoicing and digital signature deferred.
- L5 (user answers): roles "Administrador", "Comercial / Ventas"; "Configurables (recomendado)"; leads visibility "Solo los suyos"; login "Email + contraseña".
- L6 (environment): JDK 25.0.4 LTS, Node 24, pnpm 10, Docker 29, no global Gradle/Maven (use wrapper). Spring Initializr default Boot 4.2.0.
- L7 (decision): Spring Boot 4.1.3 + Spring Modulith 2.1.1 (GA) chosen over Boot 4.2.0, whose compatible Modulith was only 2.2.0-M1.
- L8 (user, verbatim): "Aun no hemos terminado de definir el stack y la arquitectura a utilizar, ve dejando el entorno de spring listo, antes de seguir con el codigo para primero definir esos puntos". T2-T6 paused until stack and architecture are defined; T1 limited to a ready Spring environment.
