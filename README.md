# GitNews (`com.outrot.gitnews`)

> Native Android ARM64 приложение для разработчиков на Kotlin & Jetpack Compose (Android 16 / SDK 36).  
> Дизайн-система: **Quiet Luxury & Engineering Precision**.  
> GitHub CI/CD, релизная подпись, интеграция РСЯ (Yandex Mobile Ads).

---

## Ключевые возможности

1. **Строгая авторизация GitHub:**
   - Полноценный OAuth 2.0 Device Authorization Flow (RFC 8628) с автоматическим опросным циклом и поддержкой `slow_down`.
   - Альтернативный вход по Personal Access Token (PAT) с проверкой скоупов `public_repo` и `read:user`.
   - Полное отсутствие гостевого режима. Токены хранятся в аппаратно-защищенном `EncryptedSharedPreferences` (AES-256 GCM) через Android KeyStore.

2. **Движок пресетов и трендов:**
   - Чистый старт: на свежей установке список пресетов пуст, лента автоматически транслирует глобальные тренды GitHub за последние 7 дней.
   - Конструктор пресетов «от А до Я»: фильтрация по ключевым словам, языкам программирования, диапазону звезд, свежести коммитов и отсечению форков.
   - Динамический транслятор пользовательских фильтров в валидный поисковый синтаксис квалификаторов GitHub Search API `q`.

3. **Инженерная карточка репозитория:**
   - Строгая типографика без аватаров автора (текст и метрики).
   - Две независимые целевые кнопки:
     - **GitHub Star:** сетевой вызов `PUT / DELETE /user/starred/{owner}/{repo}` с реактивным обновлением счетчика.
     - **Локальное Избранное:** мгновенное сохранение снимка репозитория в локальную БД Room для оффлайн-доступа.

4. **Экран деталей репозитория (`RepoDetailScreen`):**
   - Полный обзор репозитория со статистикой звезд, форков, открытых issues и веток.
   - Выделенная кнопка **«Релизы» (Releases)** с модальным окном списка релизов (`GET /repos/{owner}/{repo}/releases`), описанием изменений и прямыми ссылками на скачивание ассетов.
   - Кликабельные чипы тегов (Topics), мгновенно открывающие поиск по выбранной теме.
   - Интерактивный просмотрщик `README.md` через Markwon с переключателем «Оригинал / Перевод (RU)».

5. **Динамический мультиязычный On-Device перевод (Google ML Kit):**
   - Автоматическое определение исходного языка через `LanguageIdentification`.
   - Динамическая загрузка языковых моделей (ZH, JA, DE, EN, FR, ES, IT, KO и др.) в связке с `TranslateLanguage.RUSSIAN`.
   - Двухуровневое кэширование: L1 в оперативной памяти (LRU Cache на 500 элементов) + L2 в базе данных Room (SHA-256 хэши).
   - Токенизатор `CodeBlockProtector`: экранирует блоки кода (fenced blocks), инлайн-код, HTML-теги и URL-адреса ссылок, гарантируя 100% сохранность технического синтаксиса.

6. **Фиксированная навигация и режим плавающего окна:**
   - 4 вкладки: **`Лента`**, **`Поиск`**, **`Избранное`**, **`Профиль`**.
   - Адаптивная верстка с поддержкой HyperOS Freeform Floating Window Mode (`--windowingMode 5`).
   - Полный сброс Material ripple, тактильное пружинное сжатие `0.97f` и табличные цифры `tabular-nums`.

---

## Архитектура проекта

```
ru.dev.gitnews/
├── GitNewsApplication.kt            // Application класс
├── MainActivity.kt                  // Точка входа: edge-to-edge, AuthState switch, Root Scaffold
├── di/
│   └── AppContainer.kt              // Service Locator (Retrofit, Room, ML Kit, Repositories)
├── domain/
│   ├── model/                       // RepositoryItem, RepositoryDetail, ReleaseItem, Preset, Bookmark, AuthState
│   └── repository/                  // GitHubRepository, AuthRepository, PresetRepository, BookmarkRepository, ReleaseTrackerRepository
├── data/
│   ├── remote/                      // GitHubApiService, AuthInterceptor, DTOs
│   ├── local/                       // GitNewsDatabase, SecureTokenStorage, DAOs, Entities
│   └── repository/                  // Реализации репозиториев
├── ml/
│   ├── CodeBlockProtector.kt        // Изоляция кода и ссылок в Markdown
│   ├── MlKitTranslationManager.kt   // On-Device мультиязычный переводчик
│   └── MarkdownRenderer.kt          // Мост Markwon -> Compose AndroidView
└── ui/
    ├── theme/                       // Color.kt, Theme.kt, Type.kt, Modifiers.kt
    ├── components/                  // TopBar, BottomNavBar, PresetCarousel, RepositoryCard, MarkdownView
    └── screens/
        ├── auth/                    // Экран авторизации (Device Flow + PAT)
        ├── feed/                    // Главная лента (пустые пресеты -> тренды)
        ├── search/                  // Поисковый конструктор
        ├── bookmarks/               // Вкладка «Избранное»
        ├── profile/                 // Вкладка «Профиль» (аккаунт, квоты API, ML модели)
        └── detail/                  // Экран деталей репозитория + ReleasesBottomSheet
```
