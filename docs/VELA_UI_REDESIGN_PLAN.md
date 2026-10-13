# Vela Phase 8 UI Redesign Plan

## Mục tiêu

Đưa Vela sang trải nghiệm notes-first, cobalt-primary, sạch và thoáng theo ảnh tham chiếu nhưng giữ Android navigation, Focus Capsule và toàn bộ nghiệp vụ offline hiện có.

## Critical path

1. P8-T0 sửa baseline cho đến khi compile + unit xanh.
2. P8-T1 khóa design tokens trước khi sửa component.
3. P8-T2 tạo note summaries trước dashboard để card không tự truy vấn data.
4. P8-T3 đổi navigation + dashboard + FTS search.
5. P8-T4 và P8-T5 đồng bộ editor/media mà không đổi nghiệp vụ.
6. P8-T6 hoàn thiện responsive/accessibility.
7. P8-T7 full gate, connected tests, screenshot và docs.

## Contract đã chọn

- `AllNotes` là start destination; `Folders`, `PinnedNotes`, `DeletedNotes` là secondary collection routes.
- Không thêm tag schema. Chips dùng collection hiện có.
- `NoteSummary` chứa note metadata, preview và primary content type.
- Repository tạo summaries từ batch note/block queries; không N+1 và không migration Room.
- Search active notes tiếp tục dùng FTS; deleted notes lọc local vì soft delete đã loại khỏi index.
- Primary content type là block có ý nghĩa đầu tiên theo position; preview là text block không rỗng đầu tiên.
- Camera/scanner/audio giữ nguyên state machine và processing pipeline.

## Acceptance criteria

- Dashboard có large title `Ghi chú`, search, filter, collection chips, content cards và fixed CTA.
- Card có icon content type, preview, timestamp, swipe và overflow action tương đương.
- Editor/media/scanner/recorder dùng theme mới nhưng không mất chức năng v0.1.
- Light/dark, font lớn, landscape, tablet, TalkBack và IME không overlap.
- `assembleDebug`, toàn bộ unit, lint và connected instrumentation pass trước khi đóng Phase 8.

## Rủi ro và kiểm soát

- Navigation regression: route tests/manual Back smoke test và `launchSingleTop`/`popUpTo` collection navigation.
- FTS/search race: debounce trong ViewModel, list summary tests và repository integration tests.
- UI data cost: batch block query + grouping trong repository, không query từng row.
- Camera/scanner regression: chỉ thay presentation; giữ CameraX/OpenCV callbacks và device tests.
- Dark/large font overflow: semantic tokens, horizontal chips, max content width và Compose forced-size/font tests.

## Completion checkpoint - 2026-09-23

- P8-T0 đến P8-T7 đã hoàn tất tuần tự.
- Final gate pass: `:app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest`.
- Instrumentation: 51/51 tests pass trên AVD `Luvin_API_35`.
- Visual QA pass cho dashboard và editor ở light/dark; ảnh lưu tại `code/build/reports/visual-qa/`.
- Shared Gradle transform cache trên máy từng lỗi; final gate được xác minh bằng `GRADLE_USER_HOME` cô lập, không có thay đổi source để né test.
