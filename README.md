# gogoTimeline

gogoTimeline is a horizontally scrollable timeline scheduler (Gantt view) for Android, built from scratch with Jetpack
Compose. The first launch creates a Sample Project board, so it can be tried right away.

## Screenshots


<img src="https://github.com/user-attachments/assets/f133bdb8-4b76-4d3c-9203-24436d469022" width=300></img>|<img src="https://github.com/user-attachments/assets/986bad29-53be-4594-95c5-c9c4461198eb" width=300></img>
--- | ---

<img src="https://github.com/user-attachments/assets/8bb9e8a8-5e60-45a6-af9e-1ffc7884efdd" width=1600></img>



## Features

- Scrollable timeline - The time header and the rows scroll together, horizontally and vertically, with fling.
- Zoom - Pinch to zoom; the scale switches between hours, days, weeks and months on its own.
- Drag to edit - Move a task, change its start or end, or reorder rows; times snap to a configurable grid.
- Task editor - Name, dates and times, all-day, color and notes. Changes are saved with ✓.
- Boards - Keep separate timelines and switch between them from the drawer.
- Lock - A locked board can't be edited; tapping a task shows it read only.
- Sort - Manual order, by start time, or by duration.
- Settings - Snap interval per unit, time scale and row height.
- Light and dark themes - Follows the system setting; the 7 task colors are tuned for each theme.
- Tablet and landscape - The editor moves to a floating card when there is room.

## How to use

| Gesture | Result |
|---|---|
| Swipe | Scroll the timeline |
| Pinch | Zoom; the unit (hour / day / week / month) changes on its own |
| Tap a task | Open it in edit mode; a bar off screen is scrolled into view first. In edit mode only: drag the bar sideways to move it in time, or drag the handle at either end to change the start or the end |
| Long press a task, then drag up or down | Reorder (manual order only) |
| Tap the top-left corner of the timeline | Show or hide the full task names |
| ☰ (top left) | Open the boards drawer |
| 🔒 next to the board name | Lock the board; tapping a task then shows it read only |
| Today's date (top right) | Scroll back to today |
| Unit button, e.g. `Day` (top right) | Return to the standard zoom of the current unit, or if already there, switch to the next unit (Hour → Day → Week → Month) |
| ⋮ (top right) | Settings, rename or delete the board, and Sort by |

## Architecture

MVVM with a unidirectional data flow:

```text
UI (Compose)
  ├─ Stateful screen: talks to the ViewModel, collects StateFlow
  └─ Stateless content: takes only state and callbacks → this is what @Preview renders
ViewModel
  ├─ Collects Flows from repositories and builds UiState (StateFlow)
  ├─ Owns transient state (drafts, confirmation dialog)
  └─ Handles events → calls repositories to write
Repository
  └─ Wraps DAOs; multi-row updates run in a transaction
Room (the only source of confirmed data)
```

`TaskDao.observeTasks(boardId)` emits a `Flow` of `TaskEntity` rows through `TaskLocalDataSource`. `TaskRepository` maps
them to domain `Task` objects (timed or all-day), so the layers above it do not depend on Room. `BoardViewModel`
combines repository data with transient UI state, such as the unsaved draft, into a `BoardUiState` exposed as a
`StateFlow`. The stateful `BoardScreen` collects it and passes state and callbacks to the stateless `BoardContent`, which
is also used by previews. Events travel in the other direction: tapping ✓ calls the ViewModel, the repository writes
through the DAO in one transaction, and Room emits the confirmed data back to the UI through the same observation
pipeline.

## Tech stack

| | Version |
|---|---|
| Gradle | 9.6.0 |
| Kotlin | 2.4.21 |
| Java | 17 |
| Jetpack Compose BOM | 2026.09.00 |
| Navigation 3 | 1.2.0 |
| Room | 2.8.5 |
| Hilt | 2.60.1 |
| Kotlin Coroutines | 1.11.0 |
| JUnit / MockK | 4.13.2 / 1.14.11 |

Developed with Claude Code and Codex.

## Version

v1.0.0 (1)
