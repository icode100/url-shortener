# One-page write-up

## 1. What did you ask the AI to do, and what did you write or decide yourself?

I used Codex as an implementation partner throughout. I asked it to read the supplied brief, inspect the starter repository, turn the requirements into a design, implement the service in verified chunks, write automated tests, containerize it, exercise it in Rancher Desktop through WSL, and maintain an incremental Git history. My explicit constraints were Java 17, Linux/WSL portability, a genuinely runnable container setup, and committing and pushing only after each chunk had been tested.

The implementation is candidly AI-assisted; I did not pretend that large sections were typed independently. The decisions I supplied directly were the runtime, target environments, container requirement, and development/commit workflow. I used the verification results to accept the resulting design: a Spring HTTP service, a relational data model, generated and custom code namespaces, PostgreSQL/Flyway for the container path, H2 for a low-friction local path, and an idempotent duplicate policy. The repository documents those decisions so I can explain or change them rather than treating generated code as a black box.

## 2. Where did you override, correct, or throw away the AI's output — and why?

Two corrections came directly from running the work instead of trusting plausible-looking output. The first persistence approach expected a generated entity ID to be available early enough to derive its short code. Hibernate 7 takes its insert snapshot before that strategy could safely populate the non-null code, so the tests exposed a null/insert-order problem. I replaced it with explicit allocation from a database sequence before insertion and made the entity's new/existing state explicit with Spring Data `Persistable`. The resulting code is available before the insert, works with both H2 and PostgreSQL, and has a simple collision argument.

The first PostgreSQL image also included Flyway libraries but not Spring Boot 4's modular Flyway integration starter. The image built and all 20 tests passed, yet the live Rancher Desktop startup failed because Hibernate tried to validate an unmigrated schema. I corrected the dependency to `spring-boot-starter-flyway`, rebuilt from scratch, watched migration V1 apply, and then repeated the WSL smoke and persistence tests. This was an important reminder that a green unit build is not proof of a deployable system.

I also rejected a random-token-and-retry design in favor of sequence-plus-Base62, and kept the submitted URL separate from its normalized deduplication form. Random tokens hide volume better, but collision handling is probabilistic and test explanations become more complicated. Redirecting to the normalized form would subtly change user input, so normalization is used only as a generated-link identity key.

## 3. The two or three biggest trade-offs you made, and the alternatives you considered

**Collision-free but enumerable codes.** A datastore sequence followed by one-to-one Base62 encoding is compact, URL-safe, safe under concurrency, and easy to prove collision-free. Prefixing generated values with `_` makes their namespace disjoint from aliases. The cost is enumeration and a dependency on the primary database for allocation. Secure random codes with a uniqueness retry, UUIDs, or a distributed ID generator were alternatives; I favored correctness and clarity at this scale.

**Idempotent duplicate handling.** Generated requests for equivalent normalized URLs return the same mapping (`201` when created, `200` when recovered), including under concurrent requests. Reusing the same custom alias for the same URL is also idempotent; using it for another URL is a `409`, while different aliases may target one URL. Always creating a fresh link would make separate campaigns easier, but it would also create unbounded duplicates. The current policy is predictable and deliberately documented.

**Production-shaped deployment without sacrificing local simplicity.** PostgreSQL plus versioned Flyway migrations gives the container path realistic durability and schema control. A named Compose volume survives container recreation, but the stack is heavier than an in-memory demo. Direct Java execution therefore defaults to persistent H2. Testcontainers-based PostgreSQL integration tests would close the remaining dialect gap, at the cost of slower tests and a required container runtime.

One related limitation is analytics with permanent redirects: counts are atomic and accurate for requests that reach the server, but a browser may cache a `301` and skip later requests. A `302`/`307` would improve counting but violate the exercise's explicit `301` requirement.

## 4. What's missing, or what you'd do with another day?

Before exposing this service publicly, I would add authentication and authorization for creation and analytics, rate limits, quotas, reserved-domain and abuse/phishing controls, and an administrative delete/disable path. I would add expiration, audit fields, pagination/search, and destination-risk policies appropriate to the product.

On the engineering side, I would add Testcontainers tests against PostgreSQL, load and soak tests for concurrent shortening/redirects, structured request metrics and tracing, dashboards/alerts, and a cache for hot resolutions with a clear invalidation strategy. At larger scale I would evaluate non-enumerable distributed IDs, asynchronous analytics, replicas, and partitioning. TLS, custom domains, secret management, backup/restore drills, dependency/image scanning, and a deployment manifest for the target orchestrator would complete the production path.
