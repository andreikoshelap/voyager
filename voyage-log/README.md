# voyage-log

Telegram-бот «судовой журнал»: регистрация выходов в море, автоматический
overdue-контроль и тревога контактному лицу, справочник гаваней Таллинского залива.

## Стек

Java 21 · Spring Boot 4.1 · PostgreSQL · Flyway · ShedLock · Telegram Bot API (без сторонних SDK, чистый RestClient)

## State machine рейса

```
PLANNED -> AT_SEA -> COMPLETED
               \-> OVERDUE -> ALERTED
```

- `AT_SEA -> OVERDUE`: прошло `eta_return + voyage.grace-period`, шкиперу уходит пинг «ответь /back»
- `OVERDUE -> ALERTED`: прошло ещё `voyage.alert-delay`, контактному лицу уходит тревога
  с данными рейса и телефоном JRCC Tallinn

## Быстрый старт (local, long polling)

1. Создай бота у @BotFather, получи токен.
2. Подними базу: `docker compose up -d db`
3. Сними webhook (если ставился): `curl "https://api.telegram.org/bot<TOKEN>/deleteWebhook"`
4. Запусти:

```bash
TELEGRAM_BOT_TOKEN=123:abc ./gradlew bootRun --args='--spring.profiles.active=local'
```

В local-профиле grace-period = 2 мин и alert-delay = 1 мин — удобно проверить весь
цикл AT_SEA -> OVERDUE -> ALERTED, не дожидаясь 45 минут.

## Команды бота

| Команда | Действие |
|---|---|
| `/start` | регистрация шкипера |
| `/sail Kelnase 6 2` | выход: куда, через сколько часов вернусь, экипаж (опц.) |
| `/back` | check-in, рейс закрыт |
| `/status` | активный рейс |
| `/harbour Aegna` | справка по гавани |

## Prod (webhook)

Приложение при старте само регистрирует webhook `PUBLIC_BASE_URL/telegram/webhook`
и проверяет заголовок `X-Telegram-Bot-Api-Secret-Token`.

```bash
./gradlew bootJar
TELEGRAM_BOT_TOKEN=... TELEGRAM_WEBHOOK_SECRET=$(openssl rand -hex 16) docker compose up -d --build
```

nginx (рядом с gatto):

```nginx
server {
    server_name voyage.gatto-piccolo.com;
    location / {
        proxy_pass http://127.0.0.1:8082;
        proxy_set_header Host $host;
        proxy_set_header X-Telegram-Bot-Api-Secret-Token $http_x_telegram_bot_api_secret_token;
    }
}
```

## Roadmap

- [ ] FSM-визард `/sail`: гавань inline-кнопками, ETA, экипаж (таблица `chat_state` уже готова)
- [ ] Аварийный контакт через онбординг (`/contact`), сейчас добавляется вручную в БД
- [ ] Запрос свободных мест: ретрансляция сообщения хозяину гавани (`harbour.telegram_chat_id`)
- [ ] SMS-канал (Messente) второй реализацией `NotificationPort`
- [ ] Актуальные данные гаваней в V2-миграции (глубины, VHF, цены)

> Бот — дополнение к нормальной морской практике, а не замена: VHF, жилеты,
> заряженный телефон. В реальной беде — 112 / JRCC Tallinn.
