# Event RSVP Manager — Design

> Phase 1 design file for the Xperience Educational Program — Task 01.
> Source brief: [README.md](README.md). Implementation (Phase 2) is not covered here.

---

## Step 01 — Setup

**Audience.** Both the course reviewer (graded against the 18-step rubric) and the future implementer (Phase 2 in this repo).

**AI partner.** Claude Code, used as a sounding board. Back-and-forth chat is excluded from this document; only distilled output lands here.

**Document conventions.**
- Each step section separates **Facts**, **Assumptions**, and **Open Questions**. An Assumption is only promoted to a Fact after explicit confirmation.
- High-level prose only. No code, no SQL DDL, no JSON shapes.
- Relative dates are normalized to absolute dates.
- Cross-references use Step numbers (e.g., "see Step 08 invariants").

**Scaffold context (Facts, from repo).**
- Backend: Java 17, Spring Boot 4, Spring MVC, Spring Data JPA.
- Database: PostgreSQL, schema `hero`, single local instance.
- Frontend: React 19, TypeScript, Vite.
- No auth, mail, queueing, or deployment layer is wired in the scaffold yet.

**Out of scope for this document.**
- Visual / UX design.
- Test plan and CI configuration.
- Production operations (monitoring, on-call).

---

## Step 02 — Raw feature brief

**What it is.** A web application for hosting events and collecting RSVPs. A host creates an event, invites people by email, and watches responses come in on a live dashboard. Invitees respond Yes / No / Maybe via a unique link. If the event has a max-capacity, "Yes" responses past the cap go to a waitlist and auto-promote when a confirmed attendee drops. RSVPs lock at the event's start time.

**Who uses it.**
- **Host** — the user who creates the event. Invites people, manages event state, monitors attendance.
- **Invitee** — a person invited by email. Receives a unique link, submits and changes their RSVP until the event starts.

**Why it exists.** To replace ad-hoc RSVP collection (group chats, email threads, spreadsheets) with a single source of truth that enforces capacity, tracks state changes, and gives the host real-time visibility. This motivation is inferred from the feature shape — the source brief does not state a business reason explicitly.

**Touched system areas.**
- **Identity / access** — distinguishing host from invitee; proving an invitee is who the link was sent to.
- **Event lifecycle** — create, open, close, cancel.
- **RSVP state** — per-invitee response with transitions Yes ↔ No ↔ Maybe, plus a waitlisted sub-state under Yes.
- **Capacity & waitlist** — enforce max-capacity, queue overflow, auto-promote on withdrawal.
- **Notifications / outbound messaging** — delivering the unique link to each invitee.
- **Time & scheduling** — comparing "now" to event start to enforce the post-start lock.
- **Host dashboard / read model** — live counts and attendee list.

**Unclear points (resolved later as Assumptions in Step 06 or kept as Open Questions).**
- Q-01. Does an invitee need an account, or is the unique link the entire identity proof?
- Q-02. What's the notification mechanism for invites — email, in-app, both?
- Q-03. How are event times stored and compared — server-local, UTC, host's timezone?
- Q-04. Can the host edit event details after invites are sent? If yes, do existing RSVPs persist?
- Q-05. Is waitlist promotion FIFO, or some other order?
- Q-06. Can an event have multiple hosts, or is "host" strictly the single creator?
- Q-07. Can a cancelled or closed event be reopened?
- Q-08. What does an invitee see — only their own RSVP, or the full attendee list?

---

## Step 03 — Problem statement

Organizing an event today happens across group chats, email threads, and spreadsheets. The host has no single, authoritative view of who is coming; capacity limits are enforced by hand; invitees can't easily change their mind without re-asking the host. The Event RSVP Manager is one web app that gives **one host** a real-time, capacity-aware attendance picture for **one event at a time**, and gives each invitee a private link to set or change their response **until the event starts**.

A correct solution is one where: the live count the host sees matches reality; confirmed attendees never exceed capacity; a waitlisted Yes is promoted automatically when a seat opens; and no RSVP changes are accepted after the event has started.

---

## Step 04 — Goals and non-goals

**Goals (must do).**
- Host can create, configure, close, and cancel an event.
- Invitee can submit and change Yes / No / Maybe via a personal link, pre-start.
- Capacity is enforced; overflow Yes responses go to a waitlist.
- Waitlist auto-promotes on confirmed withdrawal.
- All RSVP writes are rejected after event start.
- Host sees a live dashboard with counts and per-invitee status.

**Non-goals (explicitly out).**
- Email / SMS delivery of invitation links — host copies the link from the dashboard (see A-02).
- Multi-host or co-organizer roles.
- Editing event start time or capacity once any RSVP exists (A-04).
- Reopening cancelled or closed-past-start events (A-07).
- Reminders, calendar (.ics) integration, payments.
- Multi-tenant organizations.
- Native mobile clients.

---

## Step 05 — Context and constraints

**Facts (from repo).**
- Java 17, Spring Boot 4, Spring MVC, Spring Data JPA — `hero-backend/pom.xml`.
- PostgreSQL, schema `hero`, default creds `postgres / 1234` — `hero-backend/src/main/resources/application.yml`.
- React 19 + TypeScript + Vite frontend on port 5171; backend on port 8280.
- Hibernate `ddl-auto: update` is enabled — fine in dev, not a migration strategy for production.

**Constraints.**
- No external services may be assumed (no SMTP, no Redis, no message broker) unless added deliberately.
- Single local instance is the target topology.
- The database is the single source of truth — no in-memory caches that could diverge.
- Educational deliverable: correctness, clarity, and traceable reasoning rank above performance.

---

## Step 06 — Facts, assumptions, open questions (consolidated)

**Facts.** The ten bullets from the source brief, plus the scaffold facts in Step 05. Single host is implied by the brief and confirmed in A-06.

**Assumptions (defaults chosen for the MVP — each can be revisited later).**
- **A-01.** Invitee identity = opaque, high-entropy token in the link. No invitee accounts.
- **A-02.** Email delivery is deferred. Host copies the link from the dashboard and shares it manually.
- **A-03.** Times stored in UTC. "Lock after start" is `server_now_utc ≥ event.start_time`.
- **A-04.** Once any RSVP exists, `start_time` and `capacity` become immutable. Description and location remain editable.
- **A-05.** Waitlist promotion is FIFO by Yes-RSVP submission time.
- **A-06.** Single host per event (the creator).
- **A-07.** Cancelled and post-start states are terminal. No reopen.
- **A-08.** An invitee sees event details and only their own RSVP. Other invitees and aggregate counts are not exposed to invitees.
- **A-09.** Host has an account with email + password and an authenticated session. Mechanism details are deferred.

**Open Questions still unresolved.**
- **Q-09.** Host auth specifics (session cookie vs. JWT, password reset flow, account verification).
- **Q-10.** Should the invitee token be single-use or stable across visits? Default assumed **stable** (re-usable).
- **Q-11.** What if the host raises capacity later? Currently moot under A-04; revisit if A-04 is relaxed.
- **Q-12.** Should the dashboard surface "promoted from waitlist" history, or only the current snapshot?

---

## Step 07 — Actors and workflows

**Actors.**
- **Host** — authenticated user, creator of an event. Performs lifecycle operations and views the dashboard.
- **Invitee** — anonymous holder of a per-event-per-person token. Submits and changes their own RSVP.
- **System** — clock-driven enforcement (lock at `start_time`), waitlist promotion.

**Workflows.**
- **W1.** Host creates an event with required fields → event enters `OPEN`.
- **W2.** Host adds an invitee by email → system mints a token → host receives a sharable link in the UI.
- **W3.** Invitee opens the link → server validates the token → page shows event details + current response.
- **W4.** Invitee submits a response → server applies the state transition; if Yes and at capacity, lands on waitlist.
- **W5.** Invitee changes response pre-start → state transitions; if confirmed Yes → No, system promotes the oldest waitlisted Yes (A-05).
- **W6.** Host closes event → no new invitees can be added; existing invitees can still change RSVP until start.
- **W7.** Host cancels event → terminal; invitee page renders "cancelled".
- **W8.** `start_time` reached → all RSVP writes are rejected (I-2).
- **W9.** Host views dashboard → confirmed count, waitlist count, per-invitee status, last-updated timestamps.

---

## Step 08 — Invariants

Each invariant must hold at every committed transaction boundary.

- **I-1 (Capacity).** `confirmed_count ≤ capacity` whenever capacity is set.
- **I-2 (Lock-after-start).** No RSVP create or change after `event.start_time`.
- **I-3 (Single response).** At most one RSVP per (event, invitee).
- **I-4 (Waitlist exclusivity).** A Yes response is in exactly one of `{confirmed, waitlisted}`.
- **I-5 (Promotion correctness).** When a confirmed Yes is withdrawn, the oldest waitlisted Yes is promoted if and only if capacity then permits.
- **I-6 (Terminal states).** Once `CANCELLED` or `LOCKED` (post-start), the event does not return to `OPEN`.
- **I-7 (Host scoping).** Only the host of an event may mutate event state or read the dashboard.
- **I-8 (Token scoping).** A token only acts on its issuing event, as its issuing invitee.

---

## Step 09 — First-pass architecture

Three deployable units; one process per side; one database.

- **React SPA** (Vite). Two surfaces: host UI (authenticated) and invitee page (token-gated).
- **Spring Boot REST API** (single Java process). Bounded modules:
  - `EventManagement` — create / close / cancel.
  - `Invitation` — token mint and validate.
  - `RSVP` — submit / change, capacity decision, waitlist promotion.
  - `Dashboard` — read-only query.
  - `Auth` — host login.
- **PostgreSQL** — single source of truth. All capacity decisions happen in a transaction here.
- **Clock** — server time, UTC. Lock-at-start is checked at write time; no scheduled job required.

Communication is synchronous request / response only. No queues, no async events in the MVP.

---

## Step 10 — Data ownership and state model

**Aggregates.**
- **Event** (owned by `EventManagement`): id, host_id, title, description, start_time (UTC), location, capacity (nullable), state, version.
- **Invite** (owned by `Invitation`): id, event_id, email, token_hash, issued_at.
- **RSVP** (owned by `RSVP`): invite_id, status, updated_at.

**Event state machine.** `OPEN` → `CLOSED` (host action; no new invites; RSVPs still editable) → `LOCKED` (post-start; terminal). Any non-terminal → `CANCELLED` (terminal). No transition out of `CANCELLED` or `LOCKED` (I-6).

**RSVP state machine.** `NONE` → one of `{YES_CONFIRMED, YES_WAITLISTED, NO, MAYBE}`. Transitions among these are allowed while the event is not `LOCKED`. `YES_WAITLISTED ↔ YES_CONFIRMED` is system-driven (promotion). `YES_*` → `NO` / `MAYBE` is invitee-driven.

**Read model.** Dashboard is a direct query over Invite and RSVP for one event_id. No separate projection in the MVP.

---

## Step 11 — Trust boundaries and security notes

**Zones.**
- Public browser — untrusted.
- Authenticated host session — trusted to act as one specific host_id.
- Server / DB — trusted; all invariants are enforced server-side.

**Boundaries.**
- Host endpoints require an authenticated session bound to `host_id`; server checks `host_id == event.host_id` (I-7).
- Invitee endpoints require a valid token bound to a specific (event_id, invite_id); server checks I-8.
- No invitee endpoint may read other invitees' data (A-08).
- No invitee endpoint may mutate event state.

**Security notes.**
- Token leakage is an accepted MVP risk (R-B in Step 14). Tokens are opaque, ≥128 bits of entropy, **stored hashed** in the DB, never logged.
- Standard web hardening: CSRF on cookie-authenticated host endpoints; server-side input validation; rate-limit the token-exchange endpoint.
- No PII in URLs beyond the opaque token.

---

## Step 12 — Concurrency and correctness

**Races to handle.**
- **R-1 (Capacity race).** Two simultaneous Yes RSVPs when one seat remains. Both must not confirm.
- **R-2 (Promotion race).** A confirmed → No happens at the same instant a new Yes arrives. Promotion and new arrival must not both take the same seat.
- **R-3 (Lock-at-start race).** RSVP change submitted within milliseconds of `start_time`. Outcome must be deterministic.

**Strategy (high-level, MVP).**
- All capacity-affecting writes go through one service method per `event_id`, inside a single DB transaction.
- The transaction acquires a row-level lock on the Event row (or runs at SERIALIZABLE scoped to that event). Inside the lock it: re-reads `now < start_time`, counts confirmed RSVPs for that event, decides confirmed vs. waitlisted, writes the RSVP, and may promote one waitlisted entry.
- Promotion is performed in the **same** transaction as the withdrawal that freed the seat.

**Out of scope for the MVP.** Distributed locking and optimistic-retry storms across instances — single-instance assumption (Step 05).

---

## Step 13 — Scalability and multi-tenancy

**Multi-tenancy — N/A.** There is no organization or tenant model. Each host owns their own events; isolation is per-row, enforced by I-7 and I-8. No `tenant_id` is introduced.

**Scalability — out of scope, with notes.** The educational target is single instance, single DB. The design already places capacity serialization at the DB row (Step 12), so if a future deployment scales horizontally, this part carries over without redesign. Expected per-event volumes are small (tens to a few hundred invitees), so the direct-query dashboard (Step 10) is sufficient.

---

## Step 14 — Risks and failure notes

| ID  | Risk | Likelihood | Impact | Mitigation |
|-----|------|------------|--------|------------|
| R-A | Capacity violation under concurrent Yes responses | Low | High | Step 12 transaction + row lock; integration test for the two-simultaneous-RSVPs case |
| R-B | Invitee forwards their link; impostor RSVPs | Medium | Medium | Accepted in MVP; documented; future option: email-verified one-time login |
| R-C | Server clock skew at event start | Low | Low | Single server, UTC; not load-bearing in MVP |
| R-D | Host bears link-distribution cost (no email) | Certain | Low | Documented (A-02); visible in dashboard UI |
| R-E | FIFO promotion order is wrong for the host's intent | Low | Low | Documented (A-05); future option: host-driven priority |
| R-F | No DB backups, no monitoring | High | Variable | Educational scope; explicitly out |

**Failure modes.**
- DB unreachable → API returns 5xx; frontend shows a retry banner.
- Write conflict on the capacity row → retry once; on a second conflict, return 409.

---

## Step 15 — Alternatives and tradeoffs

- **D-1. Invitee identity = opaque token vs. invitee accounts.** Chose token (A-01). Lighter MVP; trades off link-forwarding risk (R-B). Accounts would add user-management surface area without changing the core feature.
- **D-2. Capacity serialization = DB row lock vs. application-level lock.** Chose DB lock. Survives horizontal scale-out if it ever happens; slightly more contention. App-level lock is simpler today but breaks under any second instance.
- **D-3. Dashboard = direct query vs. event-sourced projection.** Chose direct query. Simplest path; bounded by expected per-event volumes. Projection would only be worth it at much higher scale.

---

## Step 16 — Rollout / migration notes

Greenfield deliverable: no existing users, no production database, no API consumers to keep compatible. Two notes for the future implementer:

- `ddl-auto: update` (currently set in `application.yml`) is acceptable for development. Before any non-local deployment, replace with Flyway or Liquibase migrations so schema drift is reviewable.
- An API versioning prefix (`/api/v1`) is unnecessary now; introduce it only if the API is ever exposed to third parties.

---

## Step 17 — Assembled draft

Steps 01 through 16 above form the first complete draft. Step 18 acts on this draft directly; no additional synthesis is needed here.

---

## Step 18 — Pre-review weakness check

**Self-critique.**
- **W-1.** Invitee auth is the weakest link. Token forwarding is undetected. Acceptable for an MVP; documented (R-B). Future hardening: short-lived link plus email-verified login.
- **W-2.** A-04 (locking `start_time` and capacity after first RSVP) is restrictive. A real host may legitimately want to bump capacity. Acceptable for v1; relaxation would re-open Q-11.
- **W-3.** All capacity correctness rests on **one** service-layer pathway (Step 12). If any future endpoint writes RSVP rows directly, invariants break silently. Mitigation: enforce a single entry point; add an integration test for the two-simultaneous-Yes race.
- **W-4.** No backups, monitoring, or error reporting. Educational scope; explicitly deferred. Flag for any real deployment.
- **W-5.** FIFO promotion (A-05) is correct but not visible to invitees. Surfacing "your waitlist position" on the invitee page would close a UX gap and is cheap.

**README evaluation checklist.**
- [x] Clear problem statement — Step 03
- [x] Bounded scope, explicit non-goals — Step 04
- [x] Assumptions separated from facts — Step 06
- [x] Workflows for all actors — Step 07
- [x] Named invariants — Step 08 (I-1 through I-8)
- [x] Real architecture boundaries — Step 09
- [x] Explicit state ownership — Step 10
- [x] Trust / concurrency / scale treatment — Steps 11, 12, 13
- [x] Visible risks and tradeoffs — Steps 14, 15
- [x] Unresolved open questions listed — Step 06 (Q-09 through Q-12)
