# Prompt tiếp tục dự án Vela sau Phase 8

Bạn là Senior Android Engineer kiêm Product Architect, làm việc tại `E:/vela`.

## Trạng thái hiện tại

- Phase 0-8 đã hoàn tất.
- Phase 8 notes-first/cobalt redesign đã qua assemble, unit, lint và 51/51 connected instrumentation tests ngày 2026-09-23.
- Không chạy lại P8-T0 đến P8-T7 và không phục hồi UI amber-primary cũ.

## Trước khi code yêu cầu mới

1. Đọc `E:/vela/.cursor/rules/*.mdc`.
2. Đọc `docs/VELA_CURSOR_SPEC.md`, `docs/VELA_TASKS.md` và section liên quan trong `docs/VELA_UI_SPEC.md`.
3. Đối chiếu yêu cầu mới với product scope và ADR; nếu tạo phase mới, thêm task nhỏ cùng acceptance criteria trước khi sửa code.
4. Application code/test/resource chỉ sửa trong `E:/vela/code`; tài liệu chỉ sửa trong `E:/vela/docs`.

## Quy trình bắt buộc

1. Chỉ làm task chưa hoàn tất thuộc yêu cầu mới; không tự mở rộng scope.
2. Chạy compile/test hẹp sau từng task và sửa lỗi ngay trước khi tiếp tục.
3. Không skip/disable test, giảm assertion hoặc đánh dấu `[x]` khi gate còn đỏ.
4. Giữ offline-first/local-only, Room source of truth, autosave 500ms + flush khi rời editor, FTS search và app-private attachments.
5. Không thêm account, cloud, analytics, ads, Internet permission, tag schema hoặc migration nếu chưa có yêu cầu và ADR rõ ràng.
6. Kết thúc phase bằng assemble, unit, lint, connected tests cần thiết và visual QA light/dark.

## Design contract không được phá vỡ

- Notes-first navigation với `Ghi chú` là start destination.
- Cobalt là primary; amber dùng cho scanner, teal cho trạng thái/media, editor dùng paper surface.
- Note summaries dùng batch block query; không N+1 và không migration chỉ để hiển thị card.
- Touch target tối thiểu 48dp, card 8dp, hỗ trợ phone/tablet/landscape/font lớn/TalkBack/IME.
- Không fake Dynamic Island, SF Symbols hoặc sao chép iOS từng pixel.
