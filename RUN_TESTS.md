# Запуск тестов сервера

Из корня `GoodRoad-Server`:

```bash
./gradlew test
```

Если нет прав на запуск wrapper:

```bash
chmod +x gradlew
./gradlew test
```

Успешный результат:

```text
BUILD SUCCESSFUL
```

HTML-отчет:

```text
build/reports/tests/test/index.html
```

Для проверки запуска сервера и БД:

```bash
docker compose up --build
```

Тестовые данные для локальной БД загружаются отдельно:

```bash
./scripts/seed-test-data.sh
```
