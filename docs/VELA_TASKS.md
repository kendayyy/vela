# Vela Execution Tasks

Quy tac: chi bat dau task ke tiep khi checkbox `Implementation`, `Tests` va `Gate` cua task hien tai deu xong. Neu gate fail, tao/fix test ngay trong task do.

## Phase 0 - Build foundation

- [x] P0-T1 Scaffold Gradle wrapper va app module trong `code/`; build lan dau.
- [x] P0-T2 Them version catalog va toan bo dependency stable; chay dependency resolution.
- [x] P0-T3 Them manifest, Hilt Application, MainActivity va Compose root; unit smoke test.
- [x] P0-T4 Them theme, navigation shell, lint/test config; `assembleDebug test lint`.
- [x] Phase 0 gate pass.

## Phase 1 - Data foundation

- [x] P1-T1 Note/Block/Attachment/FTS entities + Room compile gate.
- [x] P1-T2 DAO CRUD/order/detail transactions + DAO tests.  
- [x] P1-T3 FTS update/search + title/text/OCR tests.
- [x] P1-T4 Repository, UUID/Clock + unit tests.
- [x] P1-T5 Attachment staging/atomic move/quota/orphan cleanup + filesystem tests.
- [x] Phase 1 gate pass.

## Phase 2 - Vela shell

- [x] P2-T1 Color/type/spacing tokens va light/dark theme + screenshot check.
- [x] P2-T2 Edge-to-edge, adaptive cutout va Focus Capsule + inset tests.
- [x] P2-T3 Large-title collections/folder rows + Compose tests.
- [x] P2-T4 Note rows, search/Huy va swipe actions + Compose tests.
- [x] P2-T5 Vela bottom action sheet + back/dismiss/accessibility tests.
- [x] Phase 2 gate pass.

## Phase 3 - Block editor

- [x] P3-T1 Ordered text blocks/title/timestamp + state tests.
- [x] P3-T2 Formatting bar thao tac Markdown + selection tests.
- [x] P3-T3 Insert split/delete merge block rules + unit tests.
- [x] P3-T4 Autosave 500ms/flush/serialization + virtual-time tests.
- [x] P3-T5 Media block surfaces va IME layout + Compose tests.
- [x] Phase 3 gate pass.

## Phase 4 - Image, camera, file

- [x] P4-T1 Photo Picker import/cancel/multiple selection + tests.
- [x] P4-T2 Image normalize/orientation/EXIF privacy + fixture tests.
- [x] P4-T3 SAF file import/validation/size limit + tests.
- [x] P4-T4 CameraX capture/permission/rotation + device test.
- [x] P4-T5 Open/share/remove/cleanup attachment + lifecycle tests.
- [x] Phase 4 gate pass.

## Phase 5 - Audio

- [x] P5-T1 Recorder state machine va microphone permission + unit tests.
- [x] P5-T2 M4A recording/pause/resume/temp recovery + device tests.
- [x] P5-T3 Waveform extraction va Focus Capsule timer + tests.
- [x] P5-T4 Media3 playback/seek/speed/release + tests.
- [x] Phase 5 gate pass.

## Phase 6 - Scanner/OCR

- [x] P6-T1 CameraX preview va OpenCV analyzer/backpressure + performance test.
- [x] P6-T2 Edge/stability detection va auto/manual capture + fixture tests.
- [x] P6-T3 Corner editor/perspective/rotate/filters + golden-image tests.
- [x] P6-T4 Multi-page reorder/delete/PDF + 50-page tests.
- [x] P6-T5 Bundled OCR/progress/failure + Vietnamese fixture tests.
- [x] P6-T6 FTS OCR search va scan block preview + integration tests.
- [x] Phase 6 gate pass.

## Phase 7 - Hardening

- [x] P7-T1 Trash/undo/permanent media cleanup + integration tests.
- [x] P7-T2 TalkBack/font scale/keyboard/predictive Back audit.
- [x] P7-T3 Process death/offline/low-storage/manual device matrix.
- [x] P7-T4 Full `assembleDebug test lint connectedCheck` va sua moi loi.
- [x] P7-T5 Review docs, privacy claim, permissions va release checklist.
- [x] Phase 7 gate pass; v0.1 ready.

## Phase 9 - Checklist trong ghi chú

- [x] P9-T0 Baseline/contract: parser Markdown strict, reducer action va unit tests.
- [x] P9-T1 Checklist Markdown logic: toggle `[ ]`/`[x]`, cursor/selection va Unicode tests.
- [x] P9-T2 Editor interaction: checkbox 48dp, semantic state va callback Compose test.
- [x] P9-T3 Todo progress projection: `NoteSummary` derived data, batch mapping, no migration.
- [x] P9-T4 Dashboard card: progress text, semantic announcement va Compose test.
- [x] P9-T5 Accessibility/regression: autosave/Back, light/dark/font scale va full gate.
- [x] Phase 9 gate pass.

## Phase 9.1 - Markdown checklist interaction refinement

- [x] P9.1-T1 Bỏ checkbox overlay; tap trực tiếp marker Markdown để toggle.
- [x] P9.1-T2 Thêm hit-test marker, TalkBack custom actions và regression tests.
- [x] Phase 9.1 gate pass: unit, lint va 53 instrumentation tests xanh.

## Phase 10 - App Lock

- [x] P10-T0 Dependency audit: AndroidX Biometric stable va lifecycle-process; DataStore contract, Hilt binding va timeout policy tests.
- [x] P10-T1 Process lifecycle coordinator: cold start lock, background timeout va authentication request state; khong doi Room schema.
- [x] P10-T2 Lock surface: opaque Vela lock screen va Android biometric/device credential prompt.
- [x] P10-T3 Settings: enable/disable, timeout Ngay/30 giay/1 phut/5 phut, device-lock availability va accessible entry point.
- [x] P10-T4 Screen privacy: `FLAG_SECURE` khi App Lock bat de chan screenshot, recording va Recent Apps preview.
- [x] P10-T5 Regression: unit va 57 instrumentation tests xanh tren API 35 emulator.
- [x] Phase 10 gate pass: assemble, unit, lint va connected instrumentation xanh.

## Phase 9.2 - Markdown-only checklist guardrail

- [x] P9.2-T1 Khẳng định editor chỉ dùng `BasicTextField`; marker `- [ ]`/`- [x]` là vùng tap duy nhất.
- [x] P9.2-T2 Thêm regression test không có `Role.Checkbox` hay control tick render riêng.
- [x] P9.2-T3 Build APK va chạy full gate.
- [x] Phase 9.2 gate pass: unit, lint va 57 instrumentation tests xanh.
