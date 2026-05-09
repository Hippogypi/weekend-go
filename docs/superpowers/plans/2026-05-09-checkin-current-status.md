# Checkin Current Status Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add backend APIs for authenticated checkin feedback and recent current-status aggregation per place.

**Architecture:** Keep the feature isolated under `com.weekendgo.checkin`. Use Spring MVC + existing security for endpoints, `JdbcTemplate` + `TransactionTemplate` for persistence, and a repository fallback when `spring.datasource.url` is absent so default test context still starts.

**Tech Stack:** Java 17, Spring Boot 3.3, Spring Security, JdbcTemplate, H2 tests, MockMvc.

---

### Task 1: Controller Contract Tests

**Files:**
- Create: `backend/src/test/java/com/weekendgo/checkin/CheckinControllerTest.java`

- [ ] Write MockMvc tests for authenticated checkin creation, unauthenticated 401, current status with recent data, current status excluding data older than 2 hours, and stable empty response.
- [ ] Run `.\mvnw.cmd -Dtest=CheckinControllerTest test` and verify missing feature failures.

### Task 2: Repository Aggregation Tests

**Files:**
- Create: `backend/src/test/java/com/weekendgo/checkin/JdbcCheckinRepositoryTest.java`

- [ ] Write H2 repository tests that insert recent and stale rows and assert only rows since the cutoff are aggregated.
- [ ] Run `.\mvnw.cmd -Dtest=JdbcCheckinRepositoryTest test` and verify missing class failures.

### Task 3: Minimal Checkin Implementation

**Files:**
- Create: `backend/src/main/java/com/weekendgo/checkin/*.java`

- [ ] Add enum/request/response records.
- [ ] Add `CheckinRepository`, `JdbcCheckinRepository`, and `UnconfiguredCheckinRepository`.
- [ ] Add `CheckinService` with 2-hour cutoff via `Clock`.
- [ ] Add `CheckinController` at `/api/places/{placeId}/checkins` and `/api/places/{placeId}/current-status`.
- [ ] Run focused tests until green.

### Task 4: Full Backend Verification And Commit

**Files:**
- Modify only files in the current feature scope and this plan.

- [ ] Run `.\mvnw.cmd test`.
- [ ] Review `git diff --stat` and `git status`.
- [ ] Commit with `git commit -m "feat: add checkin current status APIs"`.
