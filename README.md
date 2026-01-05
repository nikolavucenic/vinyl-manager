# Vinyl Manager

Vinyl Manager is a Compose Multiplatform application for Android and iOS that manages a personal vinyl collection offline-first. It follows Clean Architecture (data/domain/presentation) with Koin-powered DI, an in-memory offline store (ready for persistence), and Ktor-based MusicBrainz search.

## Features
- **Dashboard**: daily pick card (skip/mark listened), on-this-day anniversaries, quick stats, suggested-to-sell preview.
- **Search + Add**: MusicBrainz release search with detail import and tracklist storage.
- **Collection**: list albums with metadata; filter/sort foundations in the domain model.
- **Album details**: ratings per track, listening sessions tracking (domain + storage hooks).
- **Settings**: rating scale, sell threshold, stale days; backup/reset hooks via repository entry points.
- **Daily history**: log of past daily picks and actions.

## Architecture
- **data**: in-memory repository with clear boundaries for swapping to a persistent driver, plus the Ktor MusicBrainz client.
- **domain**: pure models plus use cases (average rating, suggested-to-sell, daily pick orchestration).
- **presentation**: sealed UiState/UiAction/UiSideEffect models and a shared `VinylViewModel` consumed by Compose UI with a simple tab-based navigator.
- **DI**: `appModule` wires drivers, database, repository, use cases, and the shared view model via Koin.

## API endpoints
- MusicBrainz releases search: `GET https://musicbrainz.org/ws/2/release?query=<q>&fmt=json`
- MusicBrainz release detail with tracks: `GET https://musicbrainz.org/ws/2/release/{id}?fmt=json&inc=recordings+artist-credits`

## Running
### Android
```bash
./gradlew :composeApp:assembleDebug
```
Install the resulting APK on a device or emulator.

### iOS
Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` target. The entry point uses `MainViewController()` from the shared module.

## Tests
Unit tests live in `composeApp/src/commonTest`. Run them with:
```bash
./gradlew :composeApp:check
```
