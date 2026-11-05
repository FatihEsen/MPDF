# MPDF — Music Player Daemon Client & Sprout AI Curator

A dedicated, cozy Music Player Daemon (MPD) remote client and Android media player built with Jetpack Compose, featuring a Catppuccin Mocha palette with warm Studio Ghibli-inspired details, low-latency asynchronous TCP socket protocol communication, integrated ExoPlayer audio streaming, and an intelligent Sprout AI music curator powered by Gemini 4 Argon with high thinking mode.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions were clarified and confirmed during the initial interview:
> - **Demo Server Simulation Mode**: Enabled by default with realistic sample tracks (FLAC, MP3, Hi-Res tags, album art, folders) alongside an instant toggle to connect to any live LAN MPD instance (`host:port`).
> - **Audio Playback Engine**: Embedded ExoPlayer included for direct device listening (connecting to MPD's HTTP streaming output or demo audio), paired with full remote control capabilities for external DACs, amplifiers, and Raspberry Pi setups.
> - **Sprout AI Curator**: Integrated intelligent curator powered by Gemini (`gemini-4-argon` with `ThinkingLevel.HIGH`) offering contextual playlist generation, track insights, audio format trivia, and intelligent queue recommendations based on listening vibes.

---

### 1. Overview & Core Concept

- **What It Does**: MPDF connects seamlessly to any MPD server over local Wi-Fi / LAN via raw asynchronous TCP sockets on port 6600. It provides real-time playback control, zero-latency queue reordering, hierarchical directory browsing, queue management, volume control, and idle subsystem events. If an MPD server is not currently reachable, the app seamlessly provides a rich Demo Server mode so users can test and enjoy all interface features immediately.
- **Target Audience / Persona**: Audiophiles, self-hosters, Raspberry Pi / NAS music enthusiasts, and cozy music lovers who want a responsive, beautifully styled Android client with tactile controls and deep music curation.
- **Key Value**: Replaces clunky, outdated MPD remotes with a modern Material 3 application dressed in the beloved Catppuccin Mocha theme, enriched with vinyl animations, haptic cues, and an intelligent AI assistant.

---

### 2. User Experience & Visual Design

#### Key User Flows
1. **Connection & Discovery**:
   - On first launch, the app connects to the built-in Ghibli & Lo-Fi demo library.
   - A connection status pill (`demo` or `192.168.1.x:6600`) at the top right displays live status (Green = Connected, Yellow = Connecting, Red = Disconnected).
   - Tapping the status pill or Settings icon opens the Connection Modal with hostname, port, password, latency ping test, and audio streaming URL configuration.
2. **Queue Management ("Sıra")**:
   - Displays current playlist with title, artist, album, duration, and format badges (FLAC 24-bit 96kHz, MP3 320k, etc.).
   - Playing item features an animated Peach-accented glowing card with a mini equalizer.
   - Multi-selection mode: checkmarks appear for batch actions (Move Up, Move Down, Queue Next, Remove).
   - Quick action bar: "Save Queue as Playlist", "Clear Queue" (with confirmation dialog).
3. **Library Explorer ("Kütüphane")**:
   - Hierarchical folder navigation matching MPD's filesystem.
   - Interactive breadcrumb trail (`/Music/Studio Ghibli/Joe Hisaishi`) for swift upward navigation.
   - Per-folder/track action buttons: "Play Now", "Enqueue", "Add Entire Folder to Queue".
   - Database update trigger button in the app bar that animates while scanning.
4. **Sticky Bottom Player ("Now Playing Dock")**:
   - **Vinyl Record Deck**: Rotating vinyl disc with album artwork that spins during playback and decelerates smoothly when paused.
   - **Track Info & Favorites**: Title, artist/album, audio quality pill, and heart toggle.
   - **Seek Bar & Timeline**: Smooth timeline scrubber with elapsed and total duration counters.
   - **Volume Scrubber**: Slider with quick mute toggle and volume percentage.
   - **Transport & Modes**: Shuffle (`random`), Repeat All (`repeat`), Repeat Single (`single`), Consume Mode (`consume`), Previous, Big Play/Pause, Stop, Next.
5. **Sprout AI Curator ("Filiz Küratör")**:
   - Accessible via the sprout emblem (🌱) or floating action bar.
   - Allows users to enter natural language vibe requests (e.g., *"Cozy rainy evening in Kamakura with acoustic strings"* or *"High-energy jazz fusion tracks"*).
   - Uses `gemini-4-argon` with High Thinking to deeply reason over musical mood, tempo, and instruments, generating curated tracklists that can be directly enqueued into MPD with one tap.

#### Visual Identity & Theme (Catppuccin Mocha)
- **Palette**:
  - `Base / Background`: `#1e1e2e` (deep obsidian blue)
  - `Mantle / Surfaces`: `#181825` (card and dialog containers)
  - `Surface0 / Borders`: `#313244` (outlines and divider strokes)
  - `Peach Accent`: `#fab387` (progress indicator, active playing glow, seek bar thumb)
  - `Mauve Accent`: `#cba6f7` (active tab indicators, primary action buttons)
  - `Green Accent`: `#a6e3a1` (sprout emblem, connected badge, active toggles)
  - `Text Primary`: `#cdd6f4` (crisp readable sans-serif)
  - `Text Secondary`: `#a6adc8` (soft muted metadata)
- **Studio Ghibli Touches**:
  - Sprout (🌱) emblem for MPDF identity.
  - Soft rounded corners (`RoundedCornerShape(16.dp)` to `24.dp`).
  - Warm organic textures, subtle vinyl grooves, and tactile haptic clicks on transport buttons.
  - Clean edge-to-edge layout with full support for gesture navigation insets.

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual Protocol Engine (Real TCP Socket Client + Demo Simulator)**
  - *Chosen Approach*: Implement a shared `MpdClient` interface implemented by `TcpMpdClient` (coroutine-based non-blocking Java Sockets over port 6600) and `DemoMpdClient` (in-memory state machine with realistic Ghibli & Lo-Fi sample catalog).
  - *Why*: Allows zero-configuration testing and immediate preview in Android Studio / browser emulator, while delivering authentic low-latency TCP communication for real LAN servers.
  - *Alternatives Considered*: Only supporting real TCP would cause a blank or failed screen in the browser container where port 6600 cannot reach the user's home LAN.
- **Decision 2: Idle Subsystem Event Loop vs Polling**
  - *Chosen Approach*: Implement MPD's native `idle` command in a background coroutine to react instantly to server state mutations (`player`, `mixer`, `playlist`, `options`, `database`), with fallback heartbeat check.
  - *Why*: Eliminates network overhead, conserves mobile battery, and provides instantaneous UI synchronization when other MPD clients or hardware buttons trigger changes.
- **Decision 3: Embedded ExoPlayer for Local Streaming**
  - *Chosen Approach*: Incorporate AndroidX Media3 ExoPlayer with configurable HTTP stream URL (`http://<mpd-host>:8000/mpd.ogg` or `.mp3`).
  - *Why*: Enables users to listen directly through their phone speakers or headphones while controlling the MPD queue, or switch to mute/remote-only mode for hi-fi stereo setups.
- **Decision 4: High Thinking Gemini Integration for Sprout AI Curator**
  - *Chosen Approach*: Integrate Gemini 4 Argon (`gemini-4-argon`) with `thinkingLevel = ThinkingLevel.HIGH` to provide comprehensive music analysis and smart playlist construction.
  - *Why*: Fulfills the user's high-thinking requirement, delivering thoughtful, context-rich musical recommendations and curated queue orders.

---

### 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                                 MainActivity                                │
│                   (Catppuccin M3 Theme + Edge-to-Edge)                      │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                ┌──────────────────────┴──────────────────────┐
                │                                             │
    ┌───────────▼───────────┐                     ┌───────────▼───────────┐
    │     MpdViewModel      │                     │     AiCuratorVm       │
    │  (StateFlow UI State) │                     │ (Gemini 4 Argon/Think)│
    └───────────┬───────────┘                     └───────────┬───────────┘
                │                                             │
    ┌───────────▼───────────┐                     ┌───────────▼───────────┐
    │     MpdRepository     │                     │   Firebase.ai Logic   │
    │  (State Orchestrator) │                     │ (Structured Playlist) │
    └─────┬───────────┬─────┘                     └───────────────────────┘
          │           │
┌─────────▼────────┐ ┌▼──────────────────┐
│   TcpMpdClient   │ │   DemoMpdClient   │
│ (Sockets/Port6600│ │ (In-Memory Engine)│
│ + Idle Coroutine)│ └───────────────────┘
└─────────┬────────┘
          │
┌─────────▼────────┐
│ AudioPlayerEngine│ (AndroidX Media3 ExoPlayer for local HTTP stream playback)
└──────────────────┘
```

#### Core Data Entities
- `Song`: `id`, `file`, `title`, `artist`, `album`, `track`, `durationSec`, `format` (e.g. "FLAC 24/96"), `pos`, `isFavorite`.
- `MpdStatus`: `state` (`PLAY`, `PAUSE`, `STOP`), `songId`, `songPos`, `elapsedSeconds`, `totalSeconds`, `volume` (0..100), `random`, `repeat`, `single`, `consume`, `bitrate`, `audioFormat`, `updatingDb`.
- `LibraryItem`: `Folder` (path, name, trackCount) or `Track` (song metadata).
- `ServerConfig`: `host`, `port`, `password`, `streamUrl`, `isDemoMode`, `autoReconnect`.

#### Interactive State Mapping
- **Transport Bar**: Play/Pause/Stop/Next/Previous dispatches direct commands via `viewModel.sendAction(...)` -> triggers optimistic local UI update, instantly followed by server ACK and `idle` confirmation.
- **Queue Multi-Select**: State maintains `Set<Int>` of selected positions; top app bar morphs into contextual action mode (Move Up, Move Down, Delete Selected, Clear Selection).
- **Seek Scrubber**: Dragging updates temporary scrub state; releasing fires `seekcur <seconds>` over TCP.
- **AI Curator Drawer/Sheet**: User inputs prompt -> Gemini 4 Argon reasons with high thinking -> streams structured track suggestions -> "Add to MPD Queue" inserts songs directly into the active playlist.
