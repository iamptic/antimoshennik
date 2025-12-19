# Antimoshennik Patterns

База паттернов мошенничества для приложения "Антимошенник".

## Структура

- `patterns.json` - JSON файл с паттернами
- `version.txt` - версия базы (число)

## Как обновить

1. Отредактируйте `patterns.json`
2. Увеличьте число в `version.txt`
3. Сделайте commit и push

Приложение автоматически скачает новую версию при запуске.

## Формат patterns.json

```json
{
  "red_flags": ["опасная фраза 1", "опасная фраза 2"],
  "high_risk_patterns": ["высокий риск"],
  "medium_risk_patterns": ["средний риск"],
  "pressure_tactics": ["давление"],
  "data_requests": ["запрос данных"],
  "money_transfer": ["перевод денег"],
  "victim_responses": ["ответы жертвы"],
  "safe_indicators": ["безопасные фразы"],
  "dangerous_keywords": ["ключевые слова"]
}
```
