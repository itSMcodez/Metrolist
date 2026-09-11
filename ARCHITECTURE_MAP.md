# Metrolist Architecture Map (app module only)

## 1. Project Architecture Overview

Metrolist uses a Compose-first MVVM-style architecture with Hilt for dependency  injection. The UI layer is in Jetpack Compose screens and components, state is held in ViewModels, and data comes from Room (local library/history) plus network-backed services (YouTube/InnerTube, lyrics providers, integrations). Playback is handled by a dedicated Media3 `MusicService` with a `PlayerConnection` façade that exposes flows for UI and viewmodels. Settings are persisted in DataStore and observed across the app.

Key patterns observed:

- MVVM with Compose + ViewModels + Flow/StateFlow

- Repository-like access through `MusicDatabase` + DAO interfaces (no explicit repository layer in app module)

- Service-based playback (`MusicService` + Media3 session) with UI binding via `PlayerConnection`

- Hilt dependency injection (`@HiltAndroidApp`, `@AndroidEntryPoint`, `@Module`)

- Preferences/state propagation via DataStore helpers (`rememberPreference`, `rememberEnumPreference`)

- Long-running or background work via `CoroutineScope` + `Dispatchers.IO` (sync, lyrics, playback)
  
  ## 2. Key Packages and Their Responsibilities

`app/src/main/kotlin/com/metrolist/music`

- `ui/` → Compose UI (screens, player, menus, components, theming).

- `viewmodels/` → Screen/business logic and UI state (per-feature ViewModels).

- `playback/` → Media3 playback service, player connection, queue implementations, download service, cache.

- `db/` → Room database, DAOs, entities, data helpers.

- `lyrics/` → Lyrics providers and helpers (YouTube, LrcLib, KuGou, BetterLyrics, etc.).

- `recognition/` → Music recognition (Shazam-like signatures, audio resampling, recognition service).

- `listentogether/` → Listen Together networking, protocol, and synchronization.

- `eq/` → Equalizer engine, audio processors, EQ profiles.

- `di/` → Hilt modules and qualifiers.

- `api/` → External API integrations (OpenRouter/Mistral/DeepL services).

- `utils/` → Utilities (DataStore, networking, scrobbling, updater, cipher tools, etc.).

- `models/` → Shared models for playback and paging.

- `widget/` + `quicksettings/` → App widgets and quick settings tile integration.

- `constants/` → App constants, preference keys, enums.

- `extensions/` → Kotlin extension helpers.
  
  ## 3. Main Code Flow

App launch

1. `App` (Application) initializes crash handler, cipher deobfuscator, DataStore-driven settings, and notification channel.
2. `MainActivity` initializes app-wide services (database, `PlayerConnection`, download utilities, etc.) and calls `setContent`.
3. `MetrolistApp` in `MainActivity` configures theming, preferences, update checks, and builds the top-level UI scaffold.
4. `NavHost` + `navigationBuilder` wires screens and routes.
5. Screens use ViewModels for state and commands; ViewModels talk to:
   - `MusicDatabase`/DAO for library/history/playlists
   - Network services (YouTube/InnerTube, lyrics providers)
   - `PlayerConnection` for playback state/control
6. Playback is managed by `MusicService` (Media3), which exposes state and media session controls; the UI binds via `PlayerConnection`.

Simplified playback flow:

- UI interaction → ViewModel/Screen → `PlayerConnection` → `MusicService`/Media3 player → audio output

Data sync flow (library/playlists):

- ViewModel calls `SyncUtils` → YouTube/InnerTube APIs → Room updates in `MusicDatabase` → flows update UI

Search flow:

- Search screen → `OnlineSearchViewModel` → YouTube search APIs → `ItemsPage` state for paging → UI renders sections
  
  ## 4. Feature Map

Music Playback

- `playback/MusicService.kt`
- `playback/PlayerConnection.kt`
- `playback/queues/*`
- `ui/player/*`
- `viewmodels` relying on `PlayerConnection` and player state flows

Library & Playlists

- `db/DatabaseDao.kt` and entities
- `ui/screens/library/*`
- `ui/screens/playlist/*`
- `viewmodels/LibraryViewModels.kt`, `PlaylistsViewModel.kt`, `LocalPlaylistViewModel.kt`, etc.
- `utils/SyncUtils.kt` for syncing likes, library, playlists, artists, and podcasts

Search & Browse

- `ui/screens/search/*`
- `viewmodels/OnlineSearchViewModel.kt`, `LocalSearchViewModel.kt`
- `ui/screens/YouTubeBrowseScreen.kt`, `viewmodels/YouTubeBrowseViewModel.kt`

Lyrics

- `lyrics/*` providers and registry
- `viewmodels/LyricsMenuViewModel.kt`
- `ui/component/Lyrics.kt`, `ui/menu/LyricsMenu.kt`
- `lyrics/LyricsHelper.kt` for provider selection and caching

Downloads & Caching

- `playback/ExoDownloadService.kt`
- `playback/DownloadUtil.kt`
- `di/AppModule.kt` (cache provisioning)

Listen Together

- `listentogether/*`
- `viewmodels/ListenTogetherViewModel.kt`
- `ui/screens/ListenTogetherScreen.kt`

Music Recognition

- `recognition/*`
- `ui/screens/recognition/*`
- `quicksettings/MusicRecognizerTileService.kt`
- `widget/MusicRecognizerWidget*`

Equalizer

- `eq/*`
- `ui/screens/equalizer/*`

Wrapped / Stats

- `ui/screens/wrapped/*`
- `viewmodels/StatsViewModel.kt`
- `ui/screens/StatsScreen.kt`
- `ui/screens/wrapped/WrappedManager.kt` + `WrappedAudioService.kt`

Integrations

- LastFM: `utils/ScrobbleManager.kt`, `ui/screens/settings/integrations/LastFMSettings.kt`
- Discord RPC: `utils/DiscordRPC.kt`, `ui/screens/settings/integrations/DiscordSettings.kt`
- Updates: `utils/Updater.kt`, surfaced in `MainActivity` notifications

Settings

- `ui/screens/settings/*`

- `constants/*` + DataStore helpers in `utils/DataStore.kt`
  
  ## 4.1 Quick Reference (Condensed)

Metrolist is a Compose MVVM app with Room + DataStore for state, YouTube/InnerTube for online data, and a Media3 `MusicService` for playback. `PlayerConnection` bridges the service to UI via flows, and `SyncUtils` manages background sync. Routes are centralized in `NavigationBuilder.kt`, and most screens are ViewModel-backed with direct access to `LocalPlayerConnection` for playback.

## 4.2 10‑Line Summary

1. Compose UI + MVVM ViewModels orchestrate feature state.

2. Room is the local source of truth for library, history, playlists, stats.

3. DataStore is the source of truth for preferences and feature toggles.

4. YouTube/InnerTube provides online content and remote library data.

5. `MusicService` (Media3) owns playback, queue, and media session.

6. `PlayerConnection` exposes playback flows to UI.

7. `SyncUtils` serializes background sync operations via a Channel.

8. Queues abstract playback sources (`YouTubeQueue`, `ListQueue`, etc.).

9. Lyrics/providers, recognition, and integrations are modular utilities.

10. Navigation is centralized in `NavigationBuilder.kt`.
    
    ## 4.3 App Startup Timeline (High Level)

11. `App.onCreate()` installs crash handler, sets up cipher deobfuscation, initializes settings and notification channel.

12. `MainActivity.onCreate()` initializes listen‑together manager, applies locale, sets content.

13. `MainActivity.onStart()` binds to `MusicService`.

14. `MusicService` initializes the player and media session, exposes `isPlayerReady`.

15. `PlayerConnection` is created when the service binds and becomes ready.

16. Compose UI starts observing `PlayerConnection` flows and renders screens.
    
    ## 4.4 Navigation Routes (High Level)

Main tabs (from `Screens`):

- `home`, `search_input`, `listen_together`, `library`

Routes registered in `ui/screens/NavigationBuilder.kt` (current):

- Core: `home`, `search_input`, `library`, `listen_together`, `listen_together_from_topbar`

- Discovery & stats: `history`, `stats`, `mood_and_genres`, `new_release`, `charts_screen`

- Browse/search: `browse/{browseId}`, `search/{query}`, `youtube_browse/{browseId}?params={params}`

- Library entities: `album/{albumId}`, `artist/{artistId}?isPodcastChannel={isPodcastChannel}`, `artist/{artistId}/songs`, `artist/{artistId}/albums`, `artist/{artistId}/items?browseId={browseId}?params={params}`

- Playlists/podcasts: `online_playlist/{playlistId}`, `local_playlist/{playlistId}`, `auto_playlist/{playlist}`, `cache_playlist/{playlist}`, `top_playlist/{top}`, `online_podcast/{podcastId}`

- Settings: `settings`, `settings/appearance`, `settings/appearance/theme`, `settings/content`, `settings/content/romanization`, `settings/ai`, `settings/player`, `settings/storage`, `settings/privacy`, `settings/backup_restore`, `settings/integrations`, `settings/integrations/discord`, `settings/integrations/lastfm`, `settings/integrations/listen_together`, `settings/discord/login`, `settings/updater`, `settings/about`

- Account/login: `account`, `login`

- Wrapped: `wrapped`

- Equalizer (dialog): `equalizer`

- Recognition: `recognition?autoStart={autoStart}`, `recognition_history`
  
  ## 4.5 Deep Link Routing Summary

Entry points:

- `MainActivity.onNewIntent()` → `handleDeepLinkIntent(...)`
- Pending intents handled on first composition in `MainActivity`

Routes:

- `listen` links → Join Listen Together room (code from query or path).

- `playlist` → `online_playlist/{playlistId}` (special-case album playlists via `OLAK5uy_`).

- `browse/{browseId}` → `album/{browseId}`.

- `channel`/`c` → `artist/{artistId}`.

- `search?q=...` → `search/{query}`.

- `watch?v=...` or `youtu.be/{id}` → play queue via `YouTubeQueue`.

- `list=...` (playlist) → play queue via `YouTubeQueue`.
  
  ## 4.6 Playback State & Flow (Service ↔ UI)

Service-side state (`playback/MusicService.kt`):

- `currentMediaMetadata` (current track metadata)
- `playerFlow` (active `ExoPlayer` instance; can swap for crossfade)
- `isPlayerReady` (service init state)
- `isMuted`, `playerVolume`, volume multipliers (sleep timer, audio focus)
- `waitingForNetworkConnection`, `automixItems`

UI connection (`playback/PlayerConnection.kt`):

- Binds to `MusicService` via `MusicBinder`
- Exposes `playbackState`, `isPlaying`, `isEffectivelyPlaying`
- Mirrors queue state (`queueTitle`, `queueWindows`, `currentMediaItemIndex`)
- Observes `currentMediaMetadata` to load DB-backed `currentSong`, `currentLyrics`, `currentFormat`

Flow summary:

- `MusicService` emits player and metadata flows → `PlayerConnection` translates to UI state → screens/controls render + send commands back to service.
  
  ## 4.7 Queue Types (Playback Sources)

- `Queue` interface: defines `getInitialStatus`, `nextPage`, `hasNextPage`, plus filtering helpers.

- `YouTubeQueue` → watch endpoint-based queue; supports “radio” via `RDAMVM` playlist.

- `YouTubePlaylistQueue` → playlist-based queue with continuation paging.

- `YouTubeAlbumRadio` → album song list + radio continuation.

- `LocalAlbumRadio` → starts from local album then extends using YouTube radio.

- `ListQueue` → in-memory list (used for local lists or prebuilt queues).

- `EmptyQueue` → no-op queue.
  
  ## 4.8 Queue Construction & Playback Entry Points

Primary queue entry points (from `MusicService`):

- `playQueue(queue, playWhenReady)` sets `currentQueue`, gets initial items via `Queue.getInitialStatus()`, and loads Media3 player items.
- `startRadioSeamlessly()` replaces the tail of the current queue with a radio queue based on current track.
- `getAutomix(...)` builds a lightweight recommendation list for “Similar content” and inserts when requested.

Queue selection behaviors:

- `Queue` implementations are used to abstract where items come from (YouTube endpoints, playlists, local lists).

- Filters for explicit/video content are applied before setting media items (`HideExplicitKey`, `HideVideoSongsKey`).

- Shuffle behavior can be rebuilt after queue updates (`ShufflePlaylistFirstKey`).
  
  ## 4.9 Playback Entry Points (UI → Queue)

Common UI call sites that construct queues and call `playQueue(...)`:

- Home: mixes `YouTubeQueue`, `ListQueue`, `LocalAlbumRadio`, `YouTubeAlbumRadio` depending on item type and context.
- Album screen: `LocalAlbumRadio(albumWithSongs)` with optional `getAutomix(playlistId)`.
- Artist screen: `YouTubeQueue` for radio/shuffle endpoints; `ListQueue` for local or fetched song lists.
- Library songs: `ListQueue` for filtered library lists (supports shuffle).
- Local search: `ListQueue` from search results.
- Online podcast screen: `ListQueue` for episode lists.
- Stats: `ListQueue` for most-played lists; `YouTubeQueue` for play-once items via endpoints.

These entry points consistently rely on `LocalPlayerConnection` and build queues from:

- Local DB entities → `ListQueue` or `LocalAlbumRadio`

- YouTube endpoints/items → `YouTubeQueue` or `YouTubeAlbumRadio`
  
  ## 4.10 Sync Pipeline (Library/Playlists/Podcasts)

High-level pipeline:

- UI/ViewModel → `SyncUtils` → YouTube/InnerTube API → Room updates via `MusicDatabase` → UI flows update.

Core mechanics:

- `SyncUtils` maintains a buffered `Channel<SyncOperation>` and processes operations serially.
- `executeFullSync()` sequences liked songs, library songs, uploads, albums, artists, podcasts, episodes, playlists, auto-sync playlists.
- Each sync stage uses `withRetry` and updates a `SyncState` flow for UI.

Selected sync behaviors (examples):

- Liked songs: sync YouTube playlist `LM`, update `SongEntity.liked` and `likedDate`.

- Library songs: sync YouTube library `FEmusic_liked_videos`, update `SongEntity.inLibrary`.

- Uploaded songs/albums: sync “Uploads” tabs and mark `isUploaded`.

- Artists: sync `FEmusic_library_corpus_artists`, update `ArtistEntity` and `bookmarkedAt`.

- Podcasts: merge saved podcasts + subscribed channels; cleanup local items not on server.

- Episodes for later: sync `VLSE` playlist; maintain `SongEntity.isEpisode` and `inLibrary`.

- Playlists: sync `FEmusic_liked_playlists` + per-playlist items into `PlaylistSongMap`.
  
  ## 4.11 Data Model (Room) — Core Tables & Relations

Primary entities:

- `SongEntity` → core library/track record (likes, library membership, playback position, local flags)
- `ArtistEntity`, `AlbumEntity`, `PlaylistEntity` → core library containers
- `PodcastEntity` → podcast library with YouTube playlist backing
- `FormatEntity` → cached format details (itag, codec, bitrate)
- `LyricsEntity` → stored lyrics + provider + translations
- `Event` / `PlayCountEntity` → listening history/statistics
- `RecognitionHistory` → music recognition history
- `SearchHistory` → search query history
- `SpeedDialItem` → pinned home tiles (quick-access shortcuts)

Relationship maps / views:

- `SongArtistMap` (song↔artist many-to-many) with `SortedSongArtistMap` view for ordering.
- `SongAlbumMap` (song↔album many-to-many) with `SortedSongAlbumMap` view for ordering.
- `AlbumArtistMap` (album↔artist many-to-many).
- `PlaylistSongMap` (playlist↔song with position) + `PlaylistSongMapPreview` view.
- `RelatedSongMap` (song↔related song pairs for recommendations).

Embedded/aggregate models:

- `Song` embeds `SongEntity` + relations to artists/album/format.

- `Album` embeds `AlbumEntity` + artists + stats.

- `Artist` embeds `ArtistEntity` + stats.

- `AlbumWithSongs` aggregates album + artists + ordered songs.

- `PlaylistSong` aggregates `PlaylistSongMap` + `Song`.

- `EventWithSong` aggregates history event + `Song`.
  
  ## 4.12 Data Model Cheat‑Sheet (Key Fields)

`SongEntity`:

- `liked`, `inLibrary`, `isDownloaded`, `isUploaded`, `isVideo`, `isEpisode`, `playbackPosition`

`AlbumEntity`:

- `bookmarkedAt`, `inLibrary`, `isUploaded`, `songCount`, `duration`

`ArtistEntity`:

- `bookmarkedAt`, `channelId`, `isPodcastChannel`

`PlaylistEntity`:

- `browseId`, `isEditable`, `isAutoSync`, `remoteSongCount`

`LyricsEntity`:

- `lyrics`, `provider`, `translatedLyrics`, `translationLanguage`
  
  ## 4.13 State Ownership (Source of Truth)

Playback state:

- Source: `MusicService` (player state, current metadata, queue state, automix).
- UI access: `PlayerConnection` exposes Flows/StateFlow to Compose screens.

Library & history data:

- Source: Room (`MusicDatabase` + `DatabaseDao`).
- UI access: ViewModels collect DB flows and expose state to screens.

Preferences & settings:

- Source: DataStore (`utils/DataStore.kt`).
- UI access: `rememberPreference` / `rememberEnumPreference` in Compose.

Network content (online browse/search):

- Source: YouTube/InnerTube client (`com.metrolist.innertube.YouTube`).
- UI access: ViewModels store fetched results in StateFlow / mutable state.

Sync status:

- Source: `SyncUtils` (queue + `SyncState` flow).

- UI access: ViewModels and settings screens observe status and trigger operations.
  
  ## 4.14 ViewModel Data-Source Matrix (Representative)

Home

- `HomeViewModel` → YouTube (home/explore), Room (history/speed dial), SyncUtils (refresh), Wrapped manager.

Library

- `LibrarySongsViewModel`, `LibraryArtistsViewModel`, `LibraryAlbumsViewModel`, `LibraryPlaylistsViewModel`, `LibraryPodcastsViewModel`
- Primary: Room flows + DataStore for filters/sorts.
- Secondary: SyncUtils for on-demand sync, YouTube for refreshes.

Search/Browse

- `OnlineSearchViewModel` → YouTube search + DataStore filters.
- `LocalSearchViewModel` → Room queries.
- `YouTubeBrowseViewModel` → YouTube browse endpoints.

Playback & lyrics

- Playback UI uses `PlayerConnection` directly (not via ViewModel).
- `LyricsMenuViewModel` → Lyrics providers + Room (cached lyrics).

Stats/History

- `StatsViewModel` → Room history/events + playlist stats; optional YouTube for recap playlists.
- `HistoryViewModel` → Room events/history.

Playlists

- `PlaylistsViewModel`, `LocalPlaylistViewModel`, `OnlinePlaylistViewModel`
- Room for local lists; YouTube for online list contents; SyncUtils for sync.

Integrations

- `AccountViewModel`, `AccountSettingsViewModel` → DataStore + YouTube login state.

- Integration screens use service helpers (`ScrobbleManager`, `DiscordRPC`) plus DataStore.
  
  ## 4.15 Feature Mini-Maps (Entry → Flow → Storage)

Music playback:

- Entry: UI (player controls, list clicks) → `PlayerConnection.playQueue(...)`
- Flow: `PlayerConnection` → `MusicService` → Media3 player → audio output
- Storage: queue persistence + playback metadata in Room (`SongEntity`, `FormatEntity`), optional cache files

Library browsing:

- Entry: Library screens + ViewModels
- Flow: ViewModel → Room DAO flows → Compose UI
- Storage: Room tables (`SongEntity`, `AlbumEntity`, `ArtistEntity`, maps)

Search:

- Entry: Search screens
- Flow: Online → `OnlineSearchViewModel` → YouTube search; Local → `LocalSearchViewModel` → Room
- Storage: `SearchHistory` table, plus cached `SongEntity`/`ArtistEntity` as needed

Playlists:

- Entry: Playlist screens + menus
- Flow: Local → Room; Online → YouTube → Room via `SyncUtils`
- Storage: `PlaylistEntity`, `PlaylistSongMap`

Podcasts & episodes:

- Entry: Podcast screens + library tabs
- Flow: YouTube podcasts/episodes → `SyncUtils` → Room → UI
- Storage: `PodcastEntity`, `SongEntity.isEpisode`, `SetVideoIdEntity`

Lyrics:

- Entry: Player / Lyrics screen
- Flow: `LyricsHelper` → providers → cache → UI
- Storage: `LyricsEntity`

Recognition:

- Entry: Recognition screen/tile/widget → `MusicRecognitionService`
- Flow: audio capture → recognition → DB + UI
- Storage: `RecognitionHistory`

Stats:

- Entry: Stats screen

- Flow: ViewModel → Room (events + play counts) → UI

- Storage: `Event`, `PlayCountEntity`
  
  ## 4.16 Data Lifecycle (Online Item → Playback & Cache)

Example: play an online song from search/home

1. UI click → `PlayerConnection.playQueue(YouTubeQueue(endpoint))`
2. `MusicService` loads initial queue via YouTube next/playlist endpoints.
3. Playback requests stream URL via `YTPlayerUtils.playerResponseForPlayback(...)`.
4. `YTPlayerUtils` performs client fallback + PoToken/n-transform as needed.
5. `DownloadUtil`/`MusicService` may cache format info into `FormatEntity`.
6. UI reflects state via `PlayerConnection.mediaMetadata` and Room-backed flows.

Example: sync then play a playlist

1. `SyncUtils` pulls playlist → writes `PlaylistEntity` + `PlaylistSongMap`.

2. UI displays playlist from Room.

3. User taps play → `ListQueue` built from local `Song` entities.
   
   ## 4.17 System Contracts (What Each Layer Owns)

UI (Compose):

- Renders state, dispatches intents, owns transient UI state only.

ViewModels:

- Own feature state, orchestrate data sources, expose Flow/StateFlow to UI.

Room/Database:

- Source of truth for library, history, playlists, stats, cached formats/lyrics.

Playback service:

- Source of truth for playback state, queue, and media session integration.

YouTube/InnerTube:

- Source of truth for online content, search, and remote library/sync.

DataStore:

- Source of truth for settings and preferences.
  
  ## 4.18 Dependency Surface (UI ↔ Services)

Direct UI → service dependencies:

- `LocalPlayerConnection` used by player screens, lists, and menus.
- `LocalDownloadUtil` used in list items for download state.
- `LocalDatabase` used in some UI components for ad-hoc lookups.

UI → ViewModel dependencies:

- Most screens use `hiltViewModel()` for state and operations.
- ViewModels hide YouTube/Room/SyncUtils complexity.

Service → data dependencies:

- `MusicService` reads DataStore, Room, and YouTube utilities.

- `DownloadUtil` depends on YouTube playback utilities and Room.
  
  ## 4.19 Cross-Cutting Concerns & Error Handling

Crash handling:

- `CrashHandler` installs a global uncaught exception handler and routes to `CrashActivity` with a crash log.

Logging:

- `Timber` is the common logging facade (used heavily in playback, sync, and webview utils).

Network awareness:

- `NetworkConnectivityObserver` exposes a flow of connectivity status used by lyrics and playback logic.
- `NetworkUtils.isInternetAvailable` provides a simple transport-based check.

Playback stream robustness:

- `YTPlayerUtils` handles multi-client fallback for stream URLs, age-restricted handling, and validation.
- `CipherDeobfuscator` and `EjsNTransformSolver` handle signature/n-parameter transforms to avoid throttling.
- `PoTokenGenerator` + `PoTokenWebView` provide integrity tokens for web clients.

Download/cache:

- `DownloadUtil` resolves stream URLs through `YTPlayerUtils`, stores `FormatEntity`, and updates download state.

Retry strategy (sync):

- `SyncUtils.withRetry` wraps network operations with bounded retries and logs errors without crashing the app.
  
  ## 4.20 Error Recovery Paths (Selected)

Playback:

- Multiple client fallback in `YTPlayerUtils` if stream URL fails.
- `CipherDeobfuscator` retries with fresh player JS on failure.
- Queue auto-load handles exceptions and retries in `MusicService`.

Sync:

- `SyncUtils.withRetry` with bounded retries for network ops.
- Sync status exposed so UI can surface failures or partial completion.

Lyrics:

- Provider fallback in `LyricsHelper`, with caching to avoid repeated failures.

- Network connectivity guard to reduce hanging.
  
  ## 4.21 Security & Privacy Touchpoints

- YouTube cookies + tokens stored in DataStore (`InnerTubeCookieKey`, `VisitorDataKey`, `DataSyncIdKey`).

- PoToken/WebView used for web client integrity tokens.

- Proxy settings applied for YouTube requests (support for auth proxy).

- Crash logs routed to in-app `CrashActivity` (no external upload here).
  
  ## 4.22 Caching & Persistence Strategy

- Media stream metadata: `FormatEntity` cached per media ID (bitrate, codecs, loudness).

- Lyrics cache: `LyricsEntity` stored in Room; `LyricsHelper` also keeps an LRU in memory.

- Playback queue: persisted/restore logic in `MusicService` (persistent queue file).

- Images: `Coil` caches (memory + disk) configured in `App`.

- Downloads: `DownloadUtil` manages download cache + state in Media3.
  
  ## 4.23 Concurrency & Threading Model

- UI state: Compose + `StateFlow`/`MutableStateFlow` collected on main thread.

- IO work: `Dispatchers.IO` used for DB/network (sync, lyrics, playback fetches).

- Background loops: `SyncUtils` runs a dedicated `CoroutineScope` + buffered `Channel` to serialize sync operations.

- Playback: `MusicService` hosts player and uses its own `CoroutineScope` for operations and flow bridging.
  
  ## 4.24 Hot Paths & Performance-Sensitive Areas

Potential hot paths (high UI frequency / heavy IO):

- `MusicService` playback loop + queue pagination (frequent `nextPage`, retries, automix).
- Home screen rendering (`HomeScreen.kt`) with multiple sections + grids + recomposition.
- Player UI (`ui/player/*`) with frequent state updates (progress, queue, lyrics).
- Library lists with filtering/sorting (`LibrarySongsViewModel` + DAO sort flows).
- Sync operations (`SyncUtils`) when full sync runs; large DB writes + network calls.

Optimization levers already used:

- Flow-based state, background IO via `Dispatchers.IO`.

- DataStore-driven filters with `distinctUntilChanged`.

- Pagination via YouTube continuation in queues.
  
  ## 4.25 Media Session & External Controls

- Media3 `MediaLibraryService` exposes playback state to system UI.

- Notifications and media controls are managed in `MusicService`.

- Widgets and quick settings tile integrate with playback and recognition flows.
  
  ## 4.26 Listen Together (Multi‑Device Sync)

- `ListenTogetherManager` mediates room/role behavior.

- `PlayerConnection` supports blocking local playback changes for guests.

- UI checks “guest” state before allowing playback actions.
  
  ## 4.27 Widgets & Quick Settings

- Widgets: `widget/*` receivers and manager provide playback/recognition entry points.

- Quick Settings: `quicksettings/MusicRecognizerTileService.kt` provides a recognition tile.
  
  ## 4.28 Text-Based Dependency Diagram (App Module)

UI (Compose Screens/Components)
→ ViewModels (state orchestration)
→ DataStore (preferences)
→ Room (`MusicDatabase` + DAO)
→ YouTube/InnerTube (remote content)
→ Playback (`PlayerConnection` → `MusicService` → Media3)
→ Utilities (SyncUtils, LyricsHelper, DownloadUtil)

## 4.29 Update & Notification Flow

- `MainActivity` checks updates via `Updater` (GitHub API).

- If newer version found and notifications enabled, it posts a notification via `NotificationManagerCompat`.

- The `App` creates a notification channel for updates on startup.
  
  ## 4.30 Composition Locals (App‑Wide Singletons)

- `LocalDatabase` → `MusicDatabase`

- `LocalPlayerConnection` → `PlayerConnection?`

- `LocalDownloadUtil` → `DownloadUtil`

- `LocalSyncUtils` → `SyncUtils`

- `LocalListenTogetherManager` → listen-together manager

- `LocalPlayerAwareWindowInsets` → insets used to pad UI around player

- `LocalChangelogState` → global changelog dialog state
  
  ## 4.31 Settings & Preferences (Focused)

Settings entry screens:

- `ui/screens/settings/SettingsScreen.kt`
- `ui/screens/settings/AppearanceSettings.kt`, `ThemeScreen.kt`
- `ui/screens/settings/ContentSettings.kt`, `RomanizationSettings.kt`
- `ui/screens/settings/PlayerSettings.kt`, `StorageSettings.kt`, `PrivacySettings.kt`
- `ui/screens/settings/BackupAndRestore.kt`, `UpdaterScreen.kt`, `AboutScreen.kt`
- `ui/screens/settings/integrations/*`

Preference storage & usage:

- Keys defined in `constants/PreferenceKeys.kt` and related constants files.
- Values stored in DataStore (`utils/DataStore.kt`).
- UI binds via `rememberPreference` / `rememberEnumPreference`.

Primary keys by screen (non‑exhaustive):

- AppearanceSettings → `DynamicThemeKey`, `EnableDynamicIconKey`, `EnableHighRefreshRateKey`, `SelectedThemeColorKey`, `UseNewPlayerDesignKey`, `UseNewMiniPlayerDesignKey`, `HidePlayerThumbnailKey`, `CropAlbumArtKey`, `PlayerBackgroundStyleKey`, `DefaultOpenTabKey`, `PlayerButtonsStyleKey`, lyrics UI keys (`Lyrics*Key`), slider/swipe keys, playlist visibility keys.
- ThemeScreen → `DarkModeKey`, `PureBlackKey`, `PureBlackMiniPlayerKey`, `SelectedThemeColorKey`, `DynamicThemeKey`.
- ContentSettings → `AppLanguageKey`, `ContentLanguageKey`, `ContentCountryKey`, `HideExplicitKey`, `HideVideoSongsKey`, `HideYoutubeShortsKey`, artist display keys, proxy keys (`Proxy*`), lyrics provider order + enable flags, `QuickPicksKey`, `RandomizeHomeOrderKey`, `ShowWrappedCardKey`.
- RomanizationSettings → `LyricsRomanizeAsMainKey`, `LyricsRomanizeCyrillicByLineKey`.
- PlayerSettings → `AudioQualityKey`, `AudioNormalizationKey`, `Crossfade*Key`, `SkipSilence*Key`, `PersistentQueueKey`, `PersistentShuffleAcrossQueuesKey`, `RememberShuffleAndRepeatKey`, `ShufflePlaylistFirstKey`, `PreventDuplicateTracksInQueueKey`, `AutoLoadMoreKey`, `AutoSkipNextOnErrorKey`, `AutoDownloadOnLikeKey`, `StopMusicOnTaskClearKey`, `ResumeOnBluetoothConnectKey`, sleep timer keys.
- StorageSettings → `MaxImageCacheSizeKey`, `MaxSongCacheSizeKey`, `EnableSongCacheKey`.
- PrivacySettings → `PauseListenHistoryKey`, `PauseSearchHistoryKey`, `DisableScreenshotKey`.
- UpdaterSettings → `CheckForUpdatesKey`, `UpdateNotificationsEnabledKey`.
- AccountSettings → `AccountNameKey`, `AccountEmailKey`, `AccountChannelHandleKey`, `InnerTubeCookieKey`, `VisitorDataKey`, `DataSyncIdKey`, `YtmSyncKey`.
- AiSettings → `AiProviderKey`, `OpenRouterApiKey`, `OpenRouterBaseUrlKey`, `OpenRouterModelKey`, `TranslateLanguageKey`, `TranslateModeKey`, `DeeplApiKey`, `DeeplFormalityKey`.
- DiscordLoginScreen → `DiscordTokenKey`.
- DiscordSettings → `EnableDiscordRPCKey`, `DiscordTokenKey`, `DiscordUsernameKey`, `DiscordNameKey`, `DiscordAvatarKey`, `DiscordStatusKey`, `DiscordUseDetailsKey`, `DiscordAdvancedModeKey`, `DiscordButton*Key`, `DiscordActivity*Key`.
- LastFMSettings → `EnableLastFMScrobblingKey`, `LastFMUsernameKey`, `LastFMSessionKey`, `ScrobbleDelayPercentKey`, `ScrobbleDelaySecondsKey`, `ScrobbleMinSongDurationKey`.
- ListenTogetherSettings → `ListenTogetherServerUrlKey`, `ListenTogetherUsernameKey`, `ListenTogetherAutoApprovalKey`, `ListenTogetherAutoApproveSuggestionsKey`, `ListenTogetherSyncVolumeKey`.

Primary keys by screen (exhaustive; extracted from settings screens):

- `AccountSettings.kt` → `AccountChannelHandleKey`, `AccountEmailKey`, `AccountNameKey`, `DataSyncIdKey`, `InnerTubeCookieKey`, `VisitorDataKey`, `YtmSyncKey`.
- `AiSettings.kt` → `AiProviderKey`, `DeeplApiKey`, `DeeplFormalityKey`, `OpenRouterApiKey`, `OpenRouterBaseUrlKey`, `OpenRouterModelKey`, `TranslateLanguageKey`, `TranslateModeKey`.
- `AppearanceSettings.kt` → `ChipSortTypeKey`, `CropAlbumArtKey`, `DefaultOpenTabKey`, `DensityScaleKey`, `DynamicThemeKey`, `EnableDynamicIconKey`, `EnableHighRefreshRateKey`, `GridItemsSizeKey`, `HidePlayerThumbnailKey`, `ListenTogetherInTopBarKey`, `LyricsAnimationStyleKey`, `LyricsClickKey`, `LyricsGlowEffectKey`, `LyricsLineSpacingKey`, `LyricsScrollKey`, `LyricsTextPositionKey`, `LyricsTextSizeKey`, `PlayerBackgroundStyleKey`, `PlayerButtonsStyleKey`, `PureBlackMiniPlayerKey`, `SelectedThemeColorKey`, `ShowCachedPlaylistKey`, `ShowDownloadedPlaylistKey`, `ShowLikedPlaylistKey`, `ShowTopPlaylistKey`, `ShowUploadedPlaylistKey`, `SliderStyleKey`, `SlimNavBarKey`, `SquigglySliderKey`, `SwipeSensitivityKey`, `SwipeThumbnailKey`, `SwipeToRemoveSongKey`, `SwipeToSongKey`, `UseNewMiniPlayerDesignKey`, `UseNewPlayerDesignKey`.
- `ContentSettings.kt` → `AppLanguageKey`, `ContentCountryKey`, `ContentLanguageKey`, `EnableBetterLyricsKey`, `EnableKugouKey`, `EnableLrcLibKey`, `EnableSimpMusicKey`, `HideExplicitKey`, `HideVideoSongsKey`, `HideYoutubeShortsKey`, `LyricsProviderOrderKey`, `ProxyEnabledKey`, `ProxyPasswordKey`, `ProxyTypeKey`, `ProxyUrlKey`, `ProxyUsernameKey`, `QuickPicksKey`, `RandomizeHomeOrderKey`, `ShowArtistDescriptionKey`, `ShowArtistSubscriberCountKey`, `ShowMonthlyListenersKey`, `ShowWrappedCardKey`.
- `DiscordLoginScreen.kt` → `DiscordTokenKey`.
- `PlayerSettings.kt` → `AudioNormalizationKey`, `AudioQualityKey`, `AutoDownloadOnLikeKey`, `AutoLoadMoreKey`, `AutoSkipNextOnErrorKey`, `CrossfadeDurationKey`, `CrossfadeEnabledKey`, `CrossfadeGaplessKey`, `DisableLoadMoreWhenRepeatAllKey`, `EnableGoogleCastKey`, `PersistentQueueKey`, `PersistentShuffleAcrossQueuesKey`, `PreventDuplicateTracksInQueueKey`, `RememberShuffleAndRepeatKey`, `ResumeOnBluetoothConnectKey`, `ShufflePlaylistFirstKey`, `SkipSilenceInstantKey`, `SkipSilenceKey`, `SleepTimerCustomDaysKey`, `SleepTimerDayTimesKey`, `SleepTimerEnabledKey`, `SleepTimerEndTimeKey`, `SleepTimerFadeOutKey`, `SleepTimerRepeatKey`, `SleepTimerStartTimeKey`, `SleepTimerStopAfterCurrentSongKey`, `StopMusicOnTaskClearKey`.
- `PrivacySettings.kt` → `DisableScreenshotKey`, `PauseListenHistoryKey`, `PauseSearchHistoryKey`.
- `RomanizationSettings.kt` → `LyricsRomanizeAsMainKey`, `LyricsRomanizeCyrillicByLineKey`.
- `StorageSettings.kt` → `EnableSongCacheKey`, `MaxImageCacheSizeKey`, `MaxSongCacheSizeKey`.
- `ThemeScreen.kt` → `DarkModeKey`, `DynamicThemeKey`, `PureBlackKey`, `PureBlackMiniPlayerKey`, `SelectedThemeColorKey`.
- `UpdaterSettings.kt` → `CheckForUpdatesKey`, `UpdateNotificationsEnabledKey`.
- `integrations/DiscordSettings.kt` → `DiscordActivityNameKey`, `DiscordActivityTypeKey`, `DiscordAdvancedModeKey`, `DiscordAvatarKey`, `DiscordButton1TextKey`, `DiscordButton1VisibleKey`, `DiscordButton2TextKey`, `DiscordButton2VisibleKey`, `DiscordInfoDismissedKey`, `DiscordNameKey`, `DiscordStatusKey`, `DiscordTokenKey`, `DiscordUseDetailsKey`, `DiscordUsernameKey`, `EnableDiscordRPCKey`.
- `integrations/LastFMSettings.kt` → `EnableLastFMScrobblingKey`, `LastFMSessionKey`, `LastFMUsernameKey`, `ScrobbleDelayPercentKey`, `ScrobbleDelaySecondsKey`, `ScrobbleMinSongDurationKey`.
- `integrations/ListenTogetherSettings.kt` → `ListenTogetherAutoApprovalKey`, `ListenTogetherAutoApproveSuggestionsKey`, `ListenTogetherServerUrlKey`, `ListenTogetherSyncVolumeKey`, `ListenTogetherUsernameKey`.

Primary keys by screen (table view):
| Screen | Keys |
| --- | --- |
| `AccountSettings.kt` | `AccountChannelHandleKey`, `AccountEmailKey`, `AccountNameKey`, `DataSyncIdKey`, `InnerTubeCookieKey`, `VisitorDataKey`, `YtmSyncKey` |
| `AiSettings.kt` | `AiProviderKey`, `DeeplApiKey`, `DeeplFormalityKey`, `OpenRouterApiKey`, `OpenRouterBaseUrlKey`, `OpenRouterModelKey`, `TranslateLanguageKey`, `TranslateModeKey` |
| `AppearanceSettings.kt` | `ChipSortTypeKey`, `CropAlbumArtKey`, `DefaultOpenTabKey`, `DensityScaleKey`, `DynamicThemeKey`, `EnableDynamicIconKey`, `EnableHighRefreshRateKey`, `GridItemsSizeKey`, `HidePlayerThumbnailKey`, `ListenTogetherInTopBarKey`, `LyricsAnimationStyleKey`, `LyricsClickKey`, `LyricsGlowEffectKey`, `LyricsLineSpacingKey`, `LyricsScrollKey`, `LyricsTextPositionKey`, `LyricsTextSizeKey`, `PlayerBackgroundStyleKey`, `PlayerButtonsStyleKey`, `PureBlackMiniPlayerKey`, `SelectedThemeColorKey`, `ShowCachedPlaylistKey`, `ShowDownloadedPlaylistKey`, `ShowLikedPlaylistKey`, `ShowTopPlaylistKey`, `ShowUploadedPlaylistKey`, `SliderStyleKey`, `SlimNavBarKey`, `SquigglySliderKey`, `SwipeSensitivityKey`, `SwipeThumbnailKey`, `SwipeToRemoveSongKey`, `SwipeToSongKey`, `UseNewMiniPlayerDesignKey`, `UseNewPlayerDesignKey` |
| `ContentSettings.kt` | `AppLanguageKey`, `ContentCountryKey`, `ContentLanguageKey`, `EnableBetterLyricsKey`, `EnableKugouKey`, `EnableLrcLibKey`, `EnableSimpMusicKey`, `HideExplicitKey`, `HideVideoSongsKey`, `HideYoutubeShortsKey`, `LyricsProviderOrderKey`, `ProxyEnabledKey`, `ProxyPasswordKey`, `ProxyTypeKey`, `ProxyUrlKey`, `ProxyUsernameKey`, `QuickPicksKey`, `RandomizeHomeOrderKey`, `ShowArtistDescriptionKey`, `ShowArtistSubscriberCountKey`, `ShowMonthlyListenersKey`, `ShowWrappedCardKey` |
| `DiscordLoginScreen.kt` | `DiscordTokenKey` |
| `PlayerSettings.kt` | `AudioNormalizationKey`, `AudioQualityKey`, `AutoDownloadOnLikeKey`, `AutoLoadMoreKey`, `AutoSkipNextOnErrorKey`, `CrossfadeDurationKey`, `CrossfadeEnabledKey`, `CrossfadeGaplessKey`, `DisableLoadMoreWhenRepeatAllKey`, `EnableGoogleCastKey`, `PersistentQueueKey`, `PersistentShuffleAcrossQueuesKey`, `PreventDuplicateTracksInQueueKey`, `RememberShuffleAndRepeatKey`, `ResumeOnBluetoothConnectKey`, `ShufflePlaylistFirstKey`, `SkipSilenceInstantKey`, `SkipSilenceKey`, `SleepTimerCustomDaysKey`, `SleepTimerDayTimesKey`, `SleepTimerEnabledKey`, `SleepTimerEndTimeKey`, `SleepTimerFadeOutKey`, `SleepTimerRepeatKey`, `SleepTimerStartTimeKey`, `SleepTimerStopAfterCurrentSongKey`, `StopMusicOnTaskClearKey` |
| `PrivacySettings.kt` | `DisableScreenshotKey`, `PauseListenHistoryKey`, `PauseSearchHistoryKey` |
| `RomanizationSettings.kt` | `LyricsRomanizeAsMainKey`, `LyricsRomanizeCyrillicByLineKey` |
| `StorageSettings.kt` | `EnableSongCacheKey`, `MaxImageCacheSizeKey`, `MaxSongCacheSizeKey` |
| `ThemeScreen.kt` | `DarkModeKey`, `DynamicThemeKey`, `PureBlackKey`, `PureBlackMiniPlayerKey`, `SelectedThemeColorKey` |
| `UpdaterSettings.kt` | `CheckForUpdatesKey`, `UpdateNotificationsEnabledKey` |
| `integrations/DiscordSettings.kt` | `DiscordActivityNameKey`, `DiscordActivityTypeKey`, `DiscordAdvancedModeKey`, `DiscordAvatarKey`, `DiscordButton1TextKey`, `DiscordButton1VisibleKey`, `DiscordButton2TextKey`, `DiscordButton2VisibleKey`, `DiscordInfoDismissedKey`, `DiscordNameKey`, `DiscordStatusKey`, `DiscordTokenKey`, `DiscordUseDetailsKey`, `DiscordUsernameKey`, `EnableDiscordRPCKey` |
| `integrations/LastFMSettings.kt` | `EnableLastFMScrobblingKey`, `LastFMSessionKey`, `LastFMUsernameKey`, `ScrobbleDelayPercentKey`, `ScrobbleDelaySecondsKey`, `ScrobbleMinSongDurationKey` |
| `integrations/ListenTogetherSettings.kt` | `ListenTogetherAutoApprovalKey`, `ListenTogetherAutoApproveSuggestionsKey`, `ListenTogetherServerUrlKey`, `ListenTogetherSyncVolumeKey`, `ListenTogetherUsernameKey` |

## 4.32 Key Preference Toggles (Examples)

Playback:

- `AudioQualityKey`, `AudioNormalizationKey`, `CrossfadeEnabledKey`, `SkipSilenceKey`
- `PersistentQueueKey`, `ShufflePlaylistFirstKey`, `RememberShuffleAndRepeatKey`

Appearance:

- `DarkModeKey`, `DynamicThemeKey`, `PureBlackKey`, `SelectedThemeColorKey`

Content & Privacy:

- `HideExplicitKey`, `HideVideoSongsKey`, `PauseListenHistoryKey`, `PauseSearchHistoryKey`

Integrations:

- `EnableLastFMScrobblingKey`, `EnableDiscordRPCKey`
  
  ## 4.33 Runtime Feature Flags (Behavior Toggles)

- `SimilarContent` (automix enable/disable)

- `UseLoginForBrowse` (YouTube browse uses login)

- `StopMusicOnTaskClearKey` (service teardown on task clear)

- `EnableHighRefreshRateKey` (display refresh control)

- `DisableScreenshotKey` (secure window flag)
  
  ## 4.34 Glossary (Key Concepts)

- `WatchEndpoint` → YouTube play endpoint (video/playlist/radio entry point).

- `browseId` → YouTube browse identifier used to fetch pages (artist/album/playlist).

- `Queue` → abstraction for loading initial playback items + pagination.

- `Automix` → background recommendation list used to extend queue.

- `PoToken` → integrity token for web client playback requests.
  
  ## 4.35 Package → Key Classes (Glanceable Map)

- `com.metrolist.music` → `App`, `MainActivity`

- `com.metrolist.music.playback` → `MusicService`, `PlayerConnection`, `DownloadUtil`

- `com.metrolist.music.db` → `MusicDatabase`, `DatabaseDao`, entities

- `com.metrolist.music.viewmodels` → `HomeViewModel`, `Library*ViewModel`, `OnlineSearchViewModel`, `StatsViewModel`

- `com.metrolist.music.ui.screens` → `HomeScreen`, `LibraryScreen`, `Player` surfaces

- `com.metrolist.music.ui.player` → `Player`, `MiniPlayer`, `Queue`, `Thumbnail`

- `com.metrolist.music.utils` → `SyncUtils`, `YTPlayerUtils`, `DataStore`, `CrashHandler`

- `com.metrolist.music.lyrics` → `LyricsHelper`, providers

- `com.metrolist.music.listentogether` → `ListenTogetherManager`, client/protocol
  
  ## 4.36 Top 10 “Important Files” (App Module)
1. `app/src/main/kotlin/com/metrolist/music/App.kt` — application init, settings bootstrap.

2. `app/src/main/kotlin/com/metrolist/music/MainActivity.kt` — Compose host, theming, navigation, update checks.

3. `app/src/main/kotlin/com/metrolist/music/ui/screens/NavigationBuilder.kt` — routes and navigation graph.

4. `app/src/main/kotlin/com/metrolist/music/playback/MusicService.kt` — playback engine, session, queue, caching.

5. `app/src/main/kotlin/com/metrolist/music/playback/PlayerConnection.kt` — UI-facing playback API + flows.

6. `app/src/main/kotlin/com/metrolist/music/db/MusicDatabase.kt` — Room schema and DB wrapper.

7. `app/src/main/kotlin/com/metrolist/music/db/DatabaseDao.kt` — core data queries and sorting.

8. `app/src/main/kotlin/com/metrolist/music/utils/SyncUtils.kt` — background sync pipeline.

9. `app/src/main/kotlin/com/metrolist/music/utils/YTPlayerUtils.kt` — stream URL resolution with fallbacks.

10. `app/src/main/kotlin/com/metrolist/music/ui/screens/HomeScreen.kt` — highest complexity UI surface.
    
    ## 4.37 Suggested Onboarding Read Order

11. `app/src/main/kotlin/com/metrolist/music/App.kt`

12. `app/src/main/kotlin/com/metrolist/music/MainActivity.kt`

13. `app/src/main/kotlin/com/metrolist/music/ui/screens/NavigationBuilder.kt`

14. `app/src/main/kotlin/com/metrolist/music/playback/MusicService.kt`

15. `app/src/main/kotlin/com/metrolist/music/playback/PlayerConnection.kt`

16. `app/src/main/kotlin/com/metrolist/music/db/MusicDatabase.kt`

17. `app/src/main/kotlin/com/metrolist/music/db/DatabaseDao.kt`

18. `app/src/main/kotlin/com/metrolist/music/utils/SyncUtils.kt`

19. `app/src/main/kotlin/com/metrolist/music/utils/YTPlayerUtils.kt`

20. `app/src/main/kotlin/com/metrolist/music/ui/screens/HomeScreen.kt`
    
    ## 4.38 Risky Hotspots (Change with Care)
- `MusicService.kt`: heavy logic, crossfade, queue persistence, media session.

- `YTPlayerUtils.kt`: stream URL resolution and client fallback logic.

- `SyncUtils.kt`: large DB write batches and YouTube sync consistency.

- `HomeScreen.kt`: high complexity UI with many sections.

- Room migrations in `MusicDatabase.kt`: schema changes can break upgrades.
  
  ## 4.39 Debugging Entry Points

- Playback: `MusicService.kt`, `PlayerConnection.kt`, `YTPlayerUtils.kt`

- UI state: `HomeScreen.kt`, `Player.kt`, `Queue.kt`

- Sync: `SyncUtils.kt`

- Data issues: `MusicDatabase.kt`, `DatabaseDao.kt`

- Deep links: `MainActivity.handleDeepLinkIntent(...)`
  
  ## 4.40 Test Checklist (What to Verify When Changing X)

Playback (`MusicService`, `PlayerConnection`, queues):

- Play/pause/seek/shuffle/repeat across local and online content.
- Queue restore works after app kill.
- Radio/automix still populates new items.
- Cast behavior unaffected if enabled.

Sync (`SyncUtils`, DAO updates):

- Full sync and individual sync tasks complete without data loss.
- Local vs remote playlist consistency (map positions).
- Episodes for later + podcasts sync both directions.

Search & Browse:

- Online search paging works; filters apply.
- Local search returns correct items and plays correct index.

UI performance:

- Home screen scroll performance and recomposition hotspots.
- Player screen updates (progress, lyrics) stay smooth.

Data & migrations:

- Room migrations apply; no data loss on upgrade.
  
  ## 4.41 Module Boundary (App Module Only)

This architecture map only covers the `app` module as requested. Other modules (e.g., `innertube`, `lastfm`, `lrclib`, etc.) are used by the app but intentionally not analyzed here.

## 4.42 “Add a Feature” Playbook

1. UI: add a screen or component in `ui/screens/` or `ui/component/`.

2. State: create a ViewModel in `viewmodels/` and expose Flow/StateFlow.

3. Data: query Room via `MusicDatabase` or call YouTube via `YouTube` client.

4. Playback: create a `Queue` and call `PlayerConnection.playQueue(...)` if needed.

5. Settings: add preference keys in `constants/PreferenceKeys.kt`, wire with DataStore helpers.

6. Navigation: register a route in `ui/screens/NavigationBuilder.kt`.
   
   ## 5. Important Classes
- `App.kt` — Application setup: crash handler, cipher deobfuscation, DataStore-driven settings, notification channels.

- `MainActivity.kt` — Top-level Compose host, theming, update checks, and Navigation setup.

- `ui/screens/NavigationBuilder.kt` — Central NavGraph with screen routes.

- `playback/MusicService.kt` — Foreground Media3 service responsible for playback, session, notifications, cache, and integrations (LastFM/Discord).

- `playback/PlayerConnection.kt` — UI-facing controller that exposes playback state via Flows and mediates commands.

- `db/MusicDatabase.kt` — Room database wrapper; exposes DAO, migrations, and concurrency settings.

- `db/DatabaseDao.kt` — Core query layer for library, playlists, history, and statistics.

- `di/AppModule.kt` — App-wide dependencies (database, cache, Listen Together manager).

- `lyrics/LyricsHelper.kt` + `LyricsProviderRegistry.kt` — Orchestrate lyrics provider selection and retrieval.

- `viewmodels/HomeViewModel.kt` — Aggregates home content (quick picks, discovery, community playlists, podcasts).

- `viewmodels/LibraryViewModels.kt` — Library filtering/sorting and sync triggers.

- `viewmodels/OnlineSearchViewModel.kt` — Online search, filters, and pagination.

- `utils/SyncUtils.kt` — Central sync queue for likes, library, playlists, subscriptions, and podcasts.

- `utils/DataStore.kt` — Preference storage and Compose helpers for reactive settings.
  
  ## 6. Simplified Mental Model

Metrolist is a Compose UI wrapped around a Media3 playback service. The UI is just state + intents, ViewModels translate those intents into data operations (DB/network) and playback commands, and `PlayerConnection` bridges the UI to `MusicService`. The database persists library and history, while network integrations fetch YouTube content, lyrics, and external services.

## 7. Potential Extension Points

- Add a new content source: add provider in `api/` or `lyrics/` and surface in ViewModel + screen.
- Add playback features: extend `MusicService`, queues in `playback/queues/`, or new UI in `ui/player/`.
- Add new screens: register routes in `ui/screens/NavigationBuilder.kt` and create matching ViewModel.
- Add settings: define preference keys in `constants/PreferenceKeys.kt`, use DataStore helpers in `utils/`.
- Add new widgets or tiles: extend `widget/` or `quicksettings/`.
- Add new sync behaviors: add `SyncOperation` in `utils/SyncUtils.kt` and hook from ViewModels.
