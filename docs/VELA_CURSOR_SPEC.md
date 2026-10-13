# Vela Cursor Spec

Tài liệu này là context rút gọn cho AI agent. Đọc file này trước; chỉ mở tài liệu gốc hoặc source liên quan trực tiếp đến task đang làm. `VELA_TASKS.md` vẫn là nguồn sự thật về tiến độ.

## 1. Product contract

- Vela là Android note app offline-first, local-only, privacy-first.
- Product promise: `Open. Write. Done.`
- Không account, backend, Firebase, cloud sync, analytics, ads hoặc `INTERNET` permission.
- Local-only không đồng nghĩa encrypted; v0.1 không app lock hay custom encryption.
- Giới hạn v0.1: file 50 MB, audio 60 phút, scan 50 trang.
- Không video, transcription, OCR-to-body, drawing, background recording hoặc WYSIWYG.

## 2. Workspace và toolchain

- Workspace: `E:/vela`.
- Application source/test/Gradle/resource: `E:/vela/code`.
- Planning/spec/checklist: `E:/vela/docs`.
- Cursor rules: `E:/vela/.cursor/rules`.
- Không sửa generated files trong `code/**/build/`.
- Package/application ID: `com.vela.notes`.
- minSdk 24, compile/targetSdk 36, JDK 17.
- AGP 9.3.3, Gradle 9.7.1, Kotlin 2.4.20, Compose BOM 2026.06.01.
- Room 2.8.5, Hilt 2.60.1, Navigation 2.9.8, CameraX 1.6.2, Media3 1.11.1, ML Kit bundled OCR 16.0.1, OpenCV 4.12.0.
- Version catalog `code/gradle/libs.versions.toml` là nguồn version duy nhất.

## 3. Source-of-truth order

1. `docs/VELA_TASKS.md`: task hiện tại và checkbox hoàn tất.
2. `docs/VELA_DECISIONS.md`: ADR/invariant kiến trúc.
3. `docs/VELA_UI_SPEC.md`: hành vi và nhận diện UI.
4. `docs/VELA_IMPLEMENTATION_PLAN.md`: phase/gate.
5. `docs/VELA_PRODUCT_PLAN.md`: product scope/roadmap đầy đủ.

Nếu spec rút gọn này khác tài liệu gốc, tài liệu gốc theo thứ tự trên thắng. Không đọc lại toàn bộ tài liệu gốc nếu task chỉ liên quan một phần; dùng heading/search để lấy đoạn cần thiết.

## 4. Architecture

```text
Compose UI -> ViewModel (StateFlow/events) -> Repository -> Room/local files
```

- Single activity + Navigation Compose; route truyền ID, không truyền object.
- UI không gọi DAO hoặc storage trực tiếp.
- ViewModel sở hữu immutable UI state và coroutine lifecycle.
- Hilt bindings nằm trong `code/app/src/main/java/com/vela/notes/di/`.
- `VelaClock` và `IdGenerator` phải được inject cho logic cần test.
- Chỉ thêm UseCase/coordinator khi logic thực sự được tái sử dụng hoặc phức tạp.
- I/O, bitmap, OpenCV, codec và PDF chạy ngoài main thread.
- Camera/recorder/codec/player/callback/temp file phải release/cleanup trên mọi exit path.

## 5. Data model và persistence

- Room schema version 1; schema export ở `code/app/schemas/`.
- Không destructive migration; schema change cần migration + test.
- `NoteEntity`: metadata, UUID String, timestamps, pin/favorite/archive/delete/folder/color.
- `NoteBlockEntity`: ordered block `TEXT/IMAGE/SCAN/AUDIO/FILE`, position bước ổn định, text Markdown hoặc attachment ID.
- `AttachmentEntity`: app-private relative path, MIME/name/size, dimensions, duration, waveform, page count, OCR, checksum.
- `NoteSearchEntity`: Room FTS cho title + Markdown blocks + OCR text.
- Save note detail và FTS index transactionally.
- Soft delete giữ attachment; permanent delete mới dọn file không còn tham chiếu.

## 6. Attachment và privacy invariants

- Bytes nằm tại app-private `filesDir/attachments/{noteId}/...`; Room không lưu blob.
- Import dùng staging, size guard, checksum, atomic move và rollback batch khi lỗi.
- Path/ID phải validate, không cho traversal.
- Photo dùng Photo Picker; file dùng SAF; không broad storage permission.
- FileProvider chỉ cấp read URI; `android:allowBackup="false"`.
- Ảnh được normalize orientation, resize tối đa 4096 và re-encode để bỏ EXIF/GPS.
- Startup maintenance dọn staging/orphan an toàn.

## 7. Editor và autosave

- Note gồm title + ordered text/media blocks; text lưu Markdown source.
- Formatting bar chèn Markdown syntax, không rich-text renderer.
- Insert actions: Scan, Camera, Photos, Record audio, Attach file.
- Autosave cập nhật UI ngay, debounce 500ms, bỏ snapshot trùng, serialize writes.
- Back phải explicit flush trước navigation; `onCleared()` không phải save guarantee.
- Media menu phải giữ Open, Share, Remove; xóa file chỉ sau metadata save.

## 8. UI identity

- Android-native, không fake notch/Dynamic Island và không sao chép iOS từng pixel.
- Focus Capsule nằm dưới status bar, chỉ hiện save/OCR/scan/recording state hữu ích.
- Cobalt `#315EFB` light / `#8EA8FF` dark là primary cho CTA, selection và active controls.
- Amber `#F5A623`/`#FFD60A` dành cho scanner/document; teal `#0F6B68`/`#5AC8C5` cho success/focus/Capsule.
- App background light `#F7F8FC`, dark `#111318`; editor light `#FFFEF7`; recording/destructive dùng semantic red.
- Inter cho large title/Vela, Android sans cho body/control; letter spacing 0.
- Touch target tối thiểu 48dp; surface/card 8dp; sheet top corners 16dp.
- Hỗ trợ light/dark, TalkBack, font lớn, landscape, IME/insets, loading/empty/error.
- `AllNotes` là start destination. Dashboard có search + filter, collection chips, content-type cards và fixed `Ghi chú mới` CTA.
- `NoteSummary` được tạo từ batch note/block query; không query từng card và không migration schema cho UI.

## 9. Audio contract

- MediaRecorder AAC/M4A foreground, pause/resume, max 60 phút.
- Temp recording ở `.audio-recording/*.m4a.part`; stop finalize `.m4a`; startup cleanup stale files.
- Waveform decode local bằng MediaExtractor/MediaCodec thành 64 peaks `0..100`.
- Focus Capsule hiển thị recording timer và pause state.
- Media3 playback phải hỗ trợ play/pause, seek, progress, speed 0.5x-2x và release idempotent.
- Player thuộc vòng đời EditorViewModel, không singleton.

## 10. Scanner/OCR contract

- CameraX preview + ImageAnalysis với backpressure `KEEP_ONLY_LATEST`.
- OpenCV local: edge/quadrilateral, stability, perspective, rotate và filters.
- Corner editor có 4 handles, clamp, Reset/Confirm và touch target lớn.
- Multi-page reorder/delete, max 50 trang, export PDF local.
- ML Kit Latin model phải bundled; OCR sau confirm, có progress/cancel/error.
- OCR chỉ lưu metadata/FTS, không tự chèn vào body.
- Scan block có cover, page count, OCR status và open/share/remove.

## 11. Source map

- `data/local/`: Room entities, DAO, FTS, converters, database.
- `data/repository/`: mapping và offline repository.
- `data/attachment/`: app-private store, maintenance, open/share/remove.
- `data/photo/`, `data/file/`, `data/audio/`: import/platform engines.
- `domain/model/`, `domain/repository/`: domain boundary.
- `ui/editor/`: block state/reducer, autosave, editor UI/ViewModel.
- `ui/collections/`, `ui/notes/`: folders/collections/list/search/swipe.
- `ui/camera/`: CameraX photo capture hiện tại.
- `ui/shell/`: edge-to-edge và Focus Capsule.
- `ui/theme/`: Vela tokens; tái sử dụng thay vì hard-code tùy tiện.
- Unit tests mirror package trong `src/test`; device/Compose tests trong `src/androidTest`.

## 12. Current execution checkpoint

- Luôn kiểm tra checkbox mới nhất trong `VELA_TASKS.md` trước khi làm.
- Phase 0-8 đã hoàn tất; notes-first/cobalt redesign đã qua full gate ngày 2026-09-23.
- Không chạy lại Phase 8 hoặc tự tạo Phase mới. Chỉ tiếp tục khi có yêu cầu mới, rồi bổ sung task/checkpoint tương ứng trước khi code.
- Baseline từng hỏng ở `NotesScreen.kt` và Trash contract đã được sửa trong P8-T0; không khôi phục lại code lỗi cũ.
- Khi checklist thay đổi, checklist thắng đoạn checkpoint này.

## 13. Test strategy

- Pure unit: reducers, formatters, planners, validators, geometry, waveform.
- Coroutines test virtual time: autosave, timer, ordering, cancellation.
- Robolectric/in-memory Room: DAO, repository, FTS, filesystem/metadata.
- Compose instrumentation: states, semantics, callbacks, Back/sheet, 48dp targets.
- Device/emulator: CameraX, permissions, FileProvider, MediaRecorder, MediaCodec, Media3, OpenCV integration.
- Fixture/golden: scanner edge, perspective/filter, PDF và Vietnamese OCR.
- Test phải deterministic, cleanup resource/file và không phụ thuộc network/external storage.

## 14. Gate commands

Chạy từ `E:/vela/code` trên Windows:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --stacktrace
.\gradlew.bat :app:connectedDebugAndroidTest --stacktrace
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --stacktrace
```

- Task gate: compile + test hẹp liên quan.
- Phase gate: assemble + toàn bộ unit + lint + required connected tests.
- Gate fail thì sửa trong cùng task; không skip/disable/giảm assertion.
- Chỉ đánh dấu `[x]` sau khi implementation, tests và gate đều xanh.

## 15. Khi nào cần đọc tài liệu dài

- Scope/roadmap/non-goal mơ hồ: đọc section liên quan trong `VELA_PRODUCT_PLAN.md`.
- Thay đổi kiến trúc/schema/privacy: đọc ADR tương ứng trong `VELA_DECISIONS.md`.
- Thiết kế/interaction/accessibility: đọc section tương ứng trong `VELA_UI_SPEC.md`.
- Acceptance criteria/phase gate: đọc phase tương ứng trong `VELA_IMPLEMENTATION_PLAN.md`.
- Tiến độ: chỉ cần `VELA_TASKS.md` và checkpoint trong prompt hiện tại.
