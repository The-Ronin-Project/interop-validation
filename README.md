[![Lint](https://github.com/projectronin/interop-validation/actions/workflows/lint.yml/badge.svg)](https://github.com/projectronin/interop-validation/actions/workflows/lint.yml)

# interop-validation

## Server

### Running locally

The current validation service can be run locally using the following command:

```shell
./gradlew clean bootJar && docker compose build --no-cache && docker compose up -d --force-recreate
```

After startup, you can access the current Swagger definition at http://localhost:8080/swagger-ui/index.html
