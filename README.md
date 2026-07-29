# codacy-metrics-detekt

This is the docker engine we use at Codacy to have [Detekt](https://github.com/arturbosch/detekt) metrics support.

## Build

### Requirements

* [Docker](https://www.docker.com/)
* [sbt](https://www.scala-sbt.org/)
* Java 8+

### Steps

```bash
sbt Universal / stage
docker build -t codacy-metrics-detekt .
```

## Run

### Requirements

* [Docker](https://www.docker.com/)

### Steps

```bash
docker run -it -v <SRC_DIR>:/src <DOCKER_NAME>:<DOCKER_VERSION>
```

* `<SRC_DIR>` should be the directory where your project is.
* `<DOCKER_NAME>` should be the docker image name you created in the Build step
* `<DOCKER_VERSION>` should be the docker image version from the Build step

Example:

```bash
docker run -it -v $PWD:/src codacy-metrics-detekt:0.1.0
```

## Test

We use the [codacy-plugins-test](https://github.com/codacy/codacy-plugins-test) to test our external tools integration. You can follow the instructions there to make sure your tool is working as expected.

## Agent Playbook: Updating This Repository End-to-End

This section is written for an AI coding agent (or a human) tasked with updating this repo — most commonly bumping the wrapped Detekt version, but also Scala/sbt version, base image, or CI orb bumps. Follow it top to bottom.

### 1. What this repository is

This is a **Codacy METRICS engine**, not a pattern-based linter. It is a thin Scala wrapper (`src/main/scala/codacy/metrics/Engine.scala`, `Detekt.scala`, built on `codacy-metrics-scala-seed`) that embeds [Detekt](https://github.com/detekt/detekt) as a **library** (not a CLI it shells out to) to compute source-code metrics for Kotlin files — lines of code (`ProjectLOCProcessor`), comment lines (`ProjectCLOCProcessor`), cyclomatic complexity (`ProjectComplexityProcessor`), class count, and function/method count. It does **not** run Detekt's rule set and does **not** report "patterns"/issues — there is no allow-list of rules, no severities, no per-rule parameters.

Because of that, this repo has **no `docs/patterns.json`, no `docs/description/*`, and no `DocGenerator`** — those only exist in pattern-based tool engines (e.g. the sibling `codacy-detekt` repo, which lints with Detekt's rules). The only file under `docs/` here is `docs/tests/simple-test.kt`, a small Kotlin fixture used as input for local/manual testing (it is copied into the Docker image via `COPY docs /docs` in the `Dockerfile`) — it is hand-maintained, not generated, and doesn't need to change on a version bump unless you want to exercise a new metric.

### 2. Files that encode versions — check all of these on every update

| File | What it controls | What to check |
|---|---|---|
| `build.sbt` → `detektVersion` val | The Detekt library version whose `detekt-core`/`detekt-api`/`detekt-rules`/`detekt-cli`/`detekt-generator` artifacts are pulled in and embedded | Bump to the target version; confirm the artifacts exist on the resolvers in use (jcenter resolver + default Maven Central). |
| `build.sbt` → `scalaVersion` | Scala version the wrapper compiles against | Only bump if required by the new Detekt release or seed library. |
| `build.sbt` → `com.codacy %% codacy-metrics-scala-seed` | Shared Codacy metrics-engine scaffolding (`MetricsTool`/`DockerMetrics` base classes) | Bump only if the task specifically calls for it. |
| `project/build.properties` → `sbt.version` | sbt version used to build | Bump only if needed for compatibility. |
| `project/plugins.sbt` → `com.codacy % codacy-sbt-plugin` | Codacy's shared sbt plugin (formatting/packaging conventions) | Check the latest published version if bumping. |
| `Dockerfile` → `FROM alpine:...` and `apk add ... openjdkNN` | Runtime base image and JVM version the packaged app runs on | Only bump if the new Detekt version raises its minimum JVM requirement (Detekt has done this before — see history below). |
| `.circleci/config.yml` → `codacy/base` orb | Shared CircleCI steps (checkout, sbt build, docker publish, tag) | Check the latest published version. |
| `.circleci/config.yml` → `codacy/plugins-test` orb | Runs `codacy-plugins-test` in CI | Same as above. |
| `src/main/scala/codacy/metrics/Detekt.scala` | Calls into Detekt's internal APIs (`KtTreeCompiler`, `ProcessingSpecBuilder`, `ProjectSpecBuilder`, `ProcessingSettings`, `Analyzer`, `YamlConfig`, the metric processors) | **A Detekt version bump has historically required source changes here too** — Detekt's internal/tooling APIs are not guarantee-stable between minor versions, so expect compile errors after bumping `detektVersion` and be ready to adapt the construction of `ProcessingSettings`/`KtTreeCompiler`/`Analyzer` to match the new API surface. |

### 3. Step-by-step update procedure

1. **Bump `detektVersion`** (and any other in-scope versions from the table above) in `build.sbt`.
2. **Compile** with `sbt compile` and fix any breakage in `Detekt.scala` caused by Detekt API changes (constructor signatures, package moves, etc. are common between Detekt minor versions).
3. **Format-check**: `sbt scalafmtCheckAll` / `sbt Test/scalafmtCheck` / `sbt scalafmtCheck` (these are exactly what CI runs — see below) — run `sbt scalafmtAll` first if formatting needs fixing.
4. **Stage the app**: `sbt Universal/stage`.
5. **Build the Docker image**: `docker build -t codacy-metrics-detekt .`.
6. **Sanity-check locally**: `docker run -it -v <SRC_DIR>:/src codacy-metrics-detekt:latest` against a small Kotlin project (or `docs/tests/simple-test.kt`) and confirm it emits metrics JSON without errors.
7. **Run `codacy-plugins-test` locally** before pushing — clone https://github.com/codacy/codacy-plugins-test and run its metrics-mode DockerTest command against your local image tag (CI runs it with `run_metrics_tests: true`, `run_json_tests: false`, `run_pattern_tests: false` — there is no pattern/JSON testing for this engine).
8. **Iterate on failures**, re-running only the relevant command after each fix.
9. **Commit** the version bump(s) together with any resulting `Detekt.scala` changes in one change.
10. **Push and open a PR.**
11. **Poll the PR's real CI checks until they all pass — local validation is NOT the finish line.** After every push, run `gh pr checks <pr-url>` and keep re-polling (short sleep while any check is `pending`) until all checks finish. If a check fails, fetch its actual log (don't guess), find the true root cause, fix it, push again (never `--no-verify`, never force-push), and re-poll. Repeat until every check is green. **The CI environment's toolchain can differ from your local one**, so a clean local run does not guarantee CI passes. Only stop iterating when every check passes, or you hit a genuine product/infra decision that needs a human.

### 4. Common failure modes and fixes

- **Compile errors in `Detekt.scala` after bumping `detektVersion`**: Detekt's `core`/`api`/tooling classes (`KtTreeCompiler`, `ProcessingSpecBuilder`, `ProjectSpecBuilder`, `ProcessingSettings`, `Analyzer`, `YamlConfig`, the `*Processor`/`*ProcessorKt` metric key holders) are internal APIs and have changed shape between releases in the past (see commit `cff4a96`, "Bump Detekt 1.23.8", which touched `Detekt.scala` alongside the version bump). Expect to adapt constructor calls/imports, not just the version string.
- **JVM/base-image mismatch**: newer Detekt releases can require a newer JDK. If the build/runtime fails with class-file-version errors, bump `openjdkNN` in the `Dockerfile` (and the base `alpine` tag if the required JDK package isn't available on the current one) — this happened in the same historical bump (`alpine:3.18.3` → `alpine:3.22`, `openjdk11` → `openjdk17`).
- **Formatting failures in CI**: CI runs `scalafmtCheckAll`, `Test/scalafmtCheck`, and `scalafmtCheck` as hard gates before the Docker build even happens — run these locally (or `sbt scalafmtAll` to auto-fix) before pushing.

### 5. Definition of done

- `detektVersion` (and any other in-scope versions) bumped in `build.sbt`, with `Dockerfile`/`.circleci/config.yml`/`project/build.properties`/`project/plugins.sbt` updated as needed.
- `Detekt.scala` compiles against the new Detekt API (adapted if the internal API surface moved).
- `sbt scalafmtCheckAll` / `Test/scalafmtCheck` / `scalafmtCheck` pass.
- `sbt Universal/stage` and `docker build` succeed.
- `codacy-plugins-test` metrics-mode tests pass locally against the freshly built image.
- **After pushing and opening/updating the PR, every CI check on it is green.** Poll `gh pr checks <pr-url>` and iterate on any failure until all pass.

## What is Codacy

[Codacy](https://www.codacy.com/) is an Automated Code Review Tool that monitors your technical debt, helps you improve your code quality, teaches best practices to your developers, and helps you save time in Code Reviews.

### Among Codacy’s features

- Identify new Static Analysis issues
- Commit and Pull Request Analysis with GitHub, BitBucket/Stash, GitLab (and also direct git repositories)
- Auto-comments on Commits and Pull Requests
- Integrations with Slack, HipChat, Jira, YouTrack
- Track issues in Code Style, Security, Error Proneness, Performance, Unused Code and other categories

Codacy also helps keep track of Code Coverage, Code Duplication, and Code Complexity.

Codacy supports PHP, Python, Ruby, Java, JavaScript, and Scala, among others.

### Free for Open Source

Codacy is free for Open Source projects.
