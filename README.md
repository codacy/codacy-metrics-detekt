# codacy-metrics-detekt

This is the docker engine we use at Codacy to have [Detekt](https://github.com/arturbosch/detekt) metrics support.

## Build

### Requirements

* [Docker](https://www.docker.com/)
* [sbt](https://www.scala-sbt.org/)
* Java 8+

### Steps

```bash
sbt universal:stage
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
