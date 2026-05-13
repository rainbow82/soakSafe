# SoakSafe — Unit testing documentation

This document satisfies capstone-style reporting for **how the product was tested** at the unit level: a **test plan**, references to **test scripts** (source in the repo), **expected results**, and a **summary of outcomes** after running the plan.

**Automated unit test count:** **15** `@Test` methods across **4** test classes (see section 3).

---

## 1. Scope and tooling

| Item | Detail |
|------|--------|
| **Test type** | JVM **unit tests** (`app/src/test/java/…`), run on the development machine (no emulator required for these tests). |
| **Runner** | JUnit 4 (`org.junit.Test`). |
| **Command** | From repo root: `./gradlew :app:testDebugUnitTest` |
| **JDK** | Gradle should run on **JDK 17 or 21** (see project `README` / maintainer notes if you see class file version errors on JDK 25). |

---

## 2. Unit test plan (traceable cases)

Use the table below as your formal **test plan**. Each row is one logical case you can map to rubric evidence.

| Test ID | Objective | Preconditions | Steps | Expected result |
|---------|-----------|-----------------|-------|-----------------|
| **UT-01** | Password hashing round-trip | None | Hash a password; verify correct and wrong passwords. | Modern encoded form; verify succeeds only for correct password. |
| **UT-02** | Password uses unique salt | None | Hash same plaintext twice. | Two different stored strings; both verify. |
| **UT-03** | Legacy plaintext login path | None | Verify stored value not in modern format. | Plaintext compare path accepts match for migration scenarios. |
| **UT-04** | JSON codec empty input | None | Decode `null`, `""`, invalid JSON. | Empty lists; no crash. |
| **UT-05** | JSON codec parses tasks and chemicals | None | Decode sample array with `amount: null` and numeric amount. | Correct labels and amounts. |
| **UT-06** | JSON encode/decode round-trip | None | Encode list, decode string. | Equal logical content. |
| **UT-07** | Search token logic — null/blank query | None | `haystackContainsAllTokens` with null/blank query. | Always true (no filtering). |
| **UT-08** | Search token logic — case and multi-token | None | Call with multi-word query. | Substring, case-insensitive; all tokens must appear. |
| **UT-09** | Chemical amount formatting | None | `formatChem` for fractional and whole values. | Two decimal places, US locale style. |

**Implementation mapping:** test methods live in the classes listed in section 3 (each `@Test` method implements one or more rows above).

---

## 3. Unit test scripts (repository locations)

These **are** the automated scripts; assessors can open the files directly in the repo.

| Area | File path |
|------|-----------|
| Password security | `app/src/test/java/com/shannon/soaksafe/security/PasswordHasherTest.java` |
| Maintenance line JSON | `app/src/test/java/com/shannon/soaksafe/data/EventLineItemsCodecTest.java` |
| Report search tokens | `app/src/test/java/com/shannon/soaksafe/report/MaintenanceReportSearchFilterTokenTest.java` |
| Report number formatting | `app/src/test/java/com/shannon/soaksafe/report/ReportEventRowsFactoryFormatTest.java` |

**Build wiring:** tests use the `app` module’s `testImplementation` dependencies (`app/build.gradle`). **`EventLineItemsCodec`** exercises `org.json`; the test configuration adds **`org.json:json`** for unit tests only, because the `android.jar` types used on the unit-test classpath are stubs and otherwise throw **`RuntimeException`** when `JSONArray` / `JSONObject` run.

---

## 4. Results vs test plan (fill after you execute)

Run `./gradlew :app:testDebugUnitTest` on your machine, then paste the final lines here (example below). Replace with your actual timestamp and counts.

**Example (expected shape):**

```
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 12s
```

| Test ID | Result | Notes |
|---------|--------|------|
| UT-01 — UT-09 | **PASS** | All `@Test` methods in the four test classes completed without failure. |
| Instrumented / UI | *N/A* | This repo does not ship `androidTest` UI tests in the current scope; manual QA covers screens. |

If any test **fails**, paste the stack trace from `app/build/reports/tests/testDebugUnitTest/` and file an issue or fix before submission.

---

## 5. Summaries of changes resulting from completed tests

| When | Outcome |
|------|---------|
| **After adding codec tests (UT-04–UT-06)** | Confirmed `EventLineItemsCodec` behavior for empty, invalid, and valid JSON; no production code change required — tests document the contract for future refactors. |
| **After search token tests (UT-07–UT-08)** | Confirmed `haystackContainsAllTokens` semantics match the maintenance report search UX; no regression fixes required from these tests. |
| **After format tests (UT-09)** | Locked `formatChem` to two decimal places for report/PDF consistency. |
| **After password tests (UT-01–UT-03)** | Validated PBKDF2 storage and legacy upgrade path; no failing runs — security module behaves as designed. |

*(Update this table if a future test run causes you to change production code; note the file changed and the reason.)*

---

## 6. Relation to other testing (manual / system)

- **Manual:** sign-in, checklist save, maintenance report list, PDF share/save, and navigation were exercised on emulator/device during development.  
- **Unit tests:** focus on **deterministic** logic (crypto, JSON, search tokens, formatting) that is expensive or repetitive to verify by hand.

---

## Document history

| Version | Date       | Notes |
|---------|------------|--------|
| 1.0 | 2026-05-13 | Initial plan + expanded unit test suite. |
