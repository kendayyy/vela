# Vela Implementation Plan

Moi task phai qua gate `compile -> unit test lien quan -> lint/static check` truoc task tiep theo. Moi phase phai qua toan bo test cua phase; khong danh dau hoan thanh neu con loi.

## Baseline da khoa

- Source chi nam trong `E:/vela/code`.
- `minSdk 24`, `compileSdk/targetSdk 36`, JDK toolchain 17.
- AGP 9.3.3, Gradle 9.7.1, Kotlin 2.4.20, Compose BOM 2026.06.01.
- Room 2.8.5, Navigation 2.9.8, Hilt 2.60.1, CameraX 1.6.2, Media3 1.11.1, ML Kit bundled text recognition 16.0.1, OpenCV 4.12.0.
- Ban stable only; version catalog la source of truth.

## Phase 0 - Build foundation

1. Scaffold Gradle wrapper, app module, manifest va resource co ban.
2. Them version catalog/dependencies va verify dependency resolution.
3. Tao Hilt application, single activity, Compose theme va root navigation.
4. Them test/lint config, Room schema export va CI-like command local.

**Gate:** `assembleDebug`, unit tests va lint pass; app khong co Internet permission.

## Phase 1 - Data foundation

1. Tao Note/Block/Attachment entities, type converters, indexes va FTS entity.
2. Tao DAO transaction cho note detail, block order, save va search index.
3. Tao repository, Clock/UUID abstractions va attachment staging store.
4. Test CRUD, order, FTS, soft/permanent delete va orphan cleanup.

**Gate:** database/repository tests pass; Room schema version 1 export thanh cong.

## Phase 2 - Vela shell va collections UI

1. Tao design tokens amber/deep teal, typography, edge-to-edge va Focus Capsule.
2. Tao folder/collection large-title screen va note list rows.
3. Tao search bar co Huy, grouped rows va empty/error/loading states.
4. Tao swipe archive/delete va Vela action bottom sheet.

**Gate:** Compose tests va screenshot checks light/dark, cutout/font scale pass.

## Phase 3 - Block editor va autosave

1. Tao title + ordered text block editor, formatting bar va IME insets.
2. Chen/xoa media placeholder tai con tro, tach/noi text block dung quy tac.
3. Autosave debounce 500ms, serialized writes va explicit Back flush.
4. Them attachment block renderers va failure/retry states.

**Gate:** virtual-time autosave, immediate Back, stale write va block-order tests pass.

## Phase 4 - Photo, camera va file

1. Photo Picker single/multiple + copy/normalize/strip GPS metadata.
2. SAF OpenDocument + size/MIME validation va app-private copy.
3. CameraX photo capture + permission/rotation/error handling.
4. Preview/open/share/remove media block va cleanup lifecycle.

**Gate:** cancellation, permission denial, low storage, process recreation va orphan tests pass.

## Phase 5 - Audio

1. Runtime microphone permission va foreground recorder state machine.
2. AAC/M4A pause/resume/stop, 60-minute limit va crash-safe temp file.
3. Waveform peak extraction va Focus Capsule timer.
4. Media3 playback, seek va 0.5x/1x/1.5x/2x speed.

**Gate:** unit state-machine tests va manual real-device recording test pass.

## Phase 6 - Scanner va OCR

1. CameraX scanner preview + OpenCV frame analyzer/backpressure.
2. Stable quadrilateral detection, auto/manual capture va corner editor.
3. Perspective correction, rotate, filters, page reorder/delete va PDF export.
4. Bundled OCR, progress/error state, FTS update va scan block preview.

**Gate:** fixture image tests, Vietnamese OCR search, 50-page guardrail va scanner performance checks pass.

## Phase 7 - v0.1 hardening

1. Hoan thien trash/undo toi thieu va permanent cleanup.
2. Accessibility, TalkBack, keyboard, predictive Back va process death.
3. Full regression: test, lint, build, dependency report va manual offline smoke test.
4. Doi chieu docs/ADR va ghi lai moi sai khac da sua.

**Gate:** khong con test/lint/build error; full offline happy path hoat dong.

## Phase 8 - Notes-first UI redesign

1. Repair baseline compile/test trước khi redesign.
2. Đổi theme sang cobalt/neutral, giữ amber scanner, teal state và paper editor.
3. Thêm `NoteSummary` từ batch note/block queries, không migration Room.
4. Đổi start destination sang All Notes; thêm chips, FTS search, filter/sort, content cards và fixed CTA.
5. Đồng bộ editor, insert sheet, camera, scanner tray, audio và Focus Capsule presentation.
6. Audit tablet/landscape/font scale 2.0/dark/TalkBack/IME và thêm Compose tests.

**Gate:** `assembleDebug`, toàn bộ unit, `lintDebug`, compile + connected instrumentation và screenshot QA pass; không đổi nghiệp vụ offline/media/scanner.

## Phase 9 - Checklist trong ghi chú

1. Giữ checklist trong Markdown text block; không tạo tab, route, Room table hay migration riêng.
2. Nhận diện chặt marker đầu dòng `- [ ] ` và `- [x] `, toggle trực tiếp từ checkbox editor mà không đổi selection/cursor.
3. Chiếu `TodoProgress` từ batch text blocks vào `NoteSummary`; search FTS tiếp tục dùng Markdown source.
4. Hiện `x/y việc hoàn tất` trên note card; hoàn tất dùng semantic teal, menu/swipe hiện có giữ nguyên.
5. Audit TalkBack, font scale, IME, dark mode và autosave/Back flush.

**Gate:** parser/reducer/repository unit tests, Compose checkbox/card tests, `assembleDebug`, `testDebugUnitTest`, `lintDebug` và `connectedDebugAndroidTest` pass; không có Room migration.

## Phase 10 - App Lock

1. Them AndroidX Biometric, lifecycle-process, DataStore settings va process-lifecycle coordinator.
2. Dung biometric manh hoac device credential Android cho lock/unlock va re-auth truoc khi tat App Lock.
3. Them Settings route, timeout Ngay/30 giay/1 phut/5 phut va opaque lock surface.
4. Bat `FLAG_SECURE` khi App Lock active de che Recent Apps va chan screen capture.

**Gate:** timeout policy unit tests, Settings/lock-screen Compose tests, full build/lint va connected device tests pass; khong doi Room schema va khong tuyên bố encryption at rest.

## Gioi han v0.1

- Tep toi da 50 MB; audio 60 phut; scan 50 trang.
- Khong video, transcription, OCR-to-body, drawing, background recording, cloud hoac WYSIWYG rich text.
- Backup kem attachment la Phase v0.2 ngay sau v0.1.
