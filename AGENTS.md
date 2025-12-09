# Repository Guidelines

## Project Structure & Module Organization
Core Spring Boot code resides in `src/main/java/com/unibook`, with packages mirroring layers (`controller`, `service`, `domain`, `repository`) plus `security`, `config`, `validation`, and shared helpers. Templates, static files, SQL migrations, and chatbot artifacts live in `src/main/resources/{templates,static,sql,data}`, while repo-level `data/` stores larger corpora for LangChain jobs. `docs/` houses design notes and API guides, and `scripts/` collects maintenance helpers and benchmark exports.

## Build, Test, and Development Commands
- `./gradlew clean build` – compiles Java 21 sources, runs tests, and emits the Boot jar in `build/libs/`.
- `./gradlew bootRun --args='--spring.profiles.active=local'` or `./run.sh` – launches the local profile with mail/DB overrides.
- `./gradlew test` – executes the full JUnit suite; run it before every push.
- `./gradlew jmh` – runs `src/test/java/com/unibook/benchmark` and writes reports to `build/reports/jmh/`.

## Coding Style & Naming Conventions
Indent with four spaces and lean on Lombok annotations (`@RequiredArgsConstructor`, `@Builder`) for boilerplate. Packages stay lowercase, classes use UpperCamelCase, methods/fields stay camelCase, and Spring beans carry `Controller`, `Service`, or `Repository` suffixes. DTOs belong in `controller/dto` or `domain/dto`, validators live in `validation/`, and configuration keys stay kebab-case in YAML.

## Testing Guidelines
JUnit 5 with Spring Boot Test drives coverage: use `@SpringBootTest` for multi-layer scenarios, `@DataJpaTest` for repositories, and `@WebMvcTest` + MockMvc for controllers. Keep test packages aligned with production ones (`service/UserServiceTests.java`) and end files with `Tests` or `Benchmark`. Any change to controller/service/repository code should introduce or update a regression test; for performance or chatbot work, pair changes with `@Benchmark` cases under `benchmark/`.

## Commit & Pull Request Guidelines
Commits follow `<type>: <summary>` (examples: `feat: RAG 챗봇 GPT 거부 패턴 감지 시스템 구축`, `perf: DataInitializer 교양학부 생성 로직 최적화`) and cover one logical change. PRs explain the problem, outline the solution, list verification commands, and link issues or OKRs. Include screenshots for UI work, metrics tables for recommendation/chatbot tweaks, and call out schema or config updates so reviewers can prep deployments.

## Security & Configuration Tips
Do not commit credentials; load them via environment variables referenced by `application-local.yml`, and keep real `application-prod.yml` values in your secrets manager. Switch contexts with `SPRING_PROFILES_ACTIVE=local|prod` to ensure correct database, mail, and LangChain endpoints. When revisiting `security/`, extend `SecurityConfig` and `UserPrincipal` instead of adding parallel filters, and document any new roles or rules inside the PR.
