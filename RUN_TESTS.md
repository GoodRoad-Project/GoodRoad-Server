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
chmod +x scripts/seed-test-data.sh
chmod +x scripts/upload-test-photos.sh
chmod +x ./scripts/seed-rewards-and-tasks.sh
./scripts/seed-test-data.sh
./scripts/upload-test-photos.sh
./scripts/seed-rewards-and-tasks.sh
```

Перед сидированием лучше пересобрать контейнер. Для загрузки фотографий необходимо установить `aws cli`, если его еще нет. 
