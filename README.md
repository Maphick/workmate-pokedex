# Workmate Pokedex (Compose + Paging + Room + Hilt)


Оффлайн‑покедекс с поиском, фильтрами и пагинацией.


## Архитектура
- **Offline‑first**: UI **всегда** читает из Room (единый источник истины).
- При наличии интернета данные синхронизируются из PokeAPI в локальную БД, поэтому
  поиск/фильтры/пагинация работают одинаково и **офлайн не ломаются**.
- **Paging 3 + RemoteMediator** отвечает за подкачку страниц из сети → Room.
- Однократный **bootstrap** загружает полный индекс имён (`/pokemon?limit=2000`) и связи
  Pokémon↔Type (`/type/{name}`) — это даёт полноценный офлайн‑поиск и офлайн‑фильтры.


## Стек
Kotlin, Jetpack Compose, Navigation, Paging 3, Room, Retrofit+Moshi, OkHttp, Hilt, DataStore, Coil.
Версии управляются через **Gradle Version Catalog** (`gradle/libs.versions.toml`).


## Сборка
- Android Studio Koala/IgUana, JDK 17
- `minSdk=26`, `targetSdk=34`
- Запуск: **Run** `MainActivity`


## Фичи
- Грид 2 колонки (картинка + имя)
- Поиск и фильтры **офлайн**
- Pull‑to‑Refresh, индикаторы загрузки/empty, back‑navigation


## API
[PokeAPI v2](https://pokeapi.co/docs/v2)