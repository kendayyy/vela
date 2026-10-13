# Vela UI Specification

Tài liệu này là nguồn sự thật cho giao diện Vela sau Phase 8. Ảnh tham chiếu chỉ định hướng về độ thoáng và thứ tự thông tin; Vela vẫn là ứng dụng Android riêng, không sao chép iOS từng pixel.

## Nhận diện

- Primary light: cobalt `#315EFB`; primary dark: `#8EA8FF`.
- Background light `#F7F8FC`, surface `#FFFFFF`; dark background `#111318`, surface `#1A1D24`.
- Editor light giữ paper `#FFFEF7`; editor dark dùng `#111318`.
- Amber `#F5A623` dành cho scanner/document ổn định; teal `#0F6B68` dành cho success/focus/Focus Capsule.
- Recording và destructive luôn dùng semantic red.
- Inter cho heading; Android sans cho body/control; letter spacing bằng 0.
- Spacing theo nhịp 4/8dp; touch target tối thiểu 48dp; card 8dp; sheet top corners 16dp.

## App shell và navigation

- Edge-to-edge, tôn trọng status/navigation bars, display cutout và IME.
- Start destination là `Tất cả ghi chú`; màn hình này không có back giả.
- Large title gốc là `Ghi chú`. Secondary routes dùng `Đã ghim`, `Thư mục`, `Đã xóa` và Android Back.
- Focus Capsule chỉ hiện khi saving, OCR, scan processing hoặc recording; không vẽ notch/Dynamic Island giả.
- Không hiển thị Pro, account hoặc Settings khi chưa có chức năng thật.

## Dashboard ghi chú

- Search field và filter button là hai control riêng, cao ít nhất 48dp; focus/query hiện nút `Hủy`.
- Collection chips gồm `Tất cả`, `Đã ghim`, `Thư mục`, `Đã xóa`; danh sách chip cuộn ngang khi font lớn.
- Filter sheet có sort `Mới cập nhật`, `Cũ nhất`, `Tên A-Z` và loại `Văn bản/Ảnh/Tài liệu quét/Âm thanh/Tệp`.
- Note card dùng surface + border nhẹ, icon container 48dp, title một dòng, preview một dòng, timestamp và overflow menu.
- Khi note có checklist, card thêm text progress `đã hoàn tất` giữa preview và timestamp. Progress hoàn tất dùng teal semantic cùng text trạng thái, không chỉ dựa vào màu.
- Icon lấy từ block có ý nghĩa đầu tiên theo document order; preview lấy text block không rỗng đầu tiên, nếu không có thì dùng nhãn attachment.
- Swipe archive/delete vẫn hoạt động; overflow menu là phương án tương đương cho TalkBack và người không dùng gesture.
- CTA `Ghi chú mới` cố định phía dưới, nằm trên navigation inset; list phải có padding để không bị che.
- Nội dung dashboard giới hạn 720-760dp trên tablet và căn giữa.

## Editor

- App bar dùng Android Back `Quay lại Ghi chú`; timestamp nằm trên title.
- Nền paper, title không border, ordered block column; text lưu và sửa dưới dạng Markdown source.
- Formatting dock nằm trên IME, có Insert, heading, bold, italic, list, checklist, quote và code.
- Checklist là Markdown source trong text block: `- [ ] ` cho việc chưa xong và `- [x] ` cho việc đã xong. Chỉ marker đầu dòng được nhận diện; bullet thường không tự biến thành checklist.
- Checklist không có checkbox hoặc icon overlay. Chạm trực tiếp vào marker `- [ ]` hoặc `- [x]` ở đầu dòng để đổi trạng thái; chạm nội dung task vẫn đặt cursor và sửa text như bình thường.
- Không được thêm `Checkbox`, `TriStateCheckbox`, icon tick hay semantics `Role.Checkbox` vào editor checklist; marker Markdown là affordance duy nhất.
- TalkBack công bố mỗi checklist như custom action nêu rõ trạng thái và nội dung. Toggle chỉ đổi marker Markdown, sau đó dùng autosave 500ms và Back flush như mọi text edit khác.
- Insert sheet gồm Scan, Camera, Photos, Record audio, Attach file; dismiss/back không làm mất nội dung.
- Media block dùng surface + border, giữ open/share/remove/retry và audio play/seek/speed.
- Editor content giới hạn 760dp trên tablet; landscape và font lớn không làm toolbar che title/body.

## Camera, scanner và audio

- Camera/scanner là full-screen operational surfaces, nền preview tối và control tương phản cao.
- Camera shutter có cobalt outer ring; close/action controls đạt 48dp và tôn trọng system insets.
- Scanner dùng teal khi đang tìm/tracking và amber khi document ổn định; không đổi pipeline CameraX/OpenCV.
- Corner editor giữ 4 handle, reset, rotate, filter và confirm. Page tray dùng selected chips, outlined `Thêm trang` và primary `Lưu PDF`.
- Recorder dùng semantic red, timer, pause/resume/stop; Focus Capsule phản ánh recording state.
- Audio block giữ waveform, position/duration, seek và speed 0.5x-2x.

## Theme, responsive và accessibility

- Mọi màu màn hình lấy từ semantic theme token; không hard-code màu primary theo screen.
- Pastel content icons chuyển sang tinted dark containers trong dark mode.
- Hỗ trợ phone nhỏ, large phone, tablet, portrait, landscape, font scale 2.0 và keyboard visible.
- Text có maxLines/ellipsis phù hợp; fixed controls không thay đổi kích thước khi state đổi.
- Icon-only button cần content description; selected chip phải công bố selected state.
- Color không phải dấu hiệu duy nhất; swipe luôn có menu/button thay thế.
- Required states: loading, empty, no-result, error, missing media, import failure, recording/paused, scan no-edge/manual và OCR progress/failure.

## App Lock

- Cài đặt thật có entry từ dashboard, không dùng màn hình Settings giả.
- App Lock mặc định tắt; khi bật dùng biometric mạnh hoặc PIN/pattern/password Android, không có PIN Vela riêng.
- Timeout gồm Ngay lập tức, 30 giây, 1 phút và 5 phút; mặc định 30 giây.
- Khi khóa, chỉ hiện surface opaque `Vela đang được khóa`; note, Focus Capsule và navigation không được render phía sau.
- Khi App Lock bật, `FLAG_SECURE` chặn screenshot, quay màn hình và preview Recent Apps. Đây là UI gate, không phải mã hóa Room hoặc attachment.

## Visual QA gate

- Kiểm tra light/dark ở phone portrait, compact landscape và tablet.
- Kiểm tra TalkBack order, touch target 48dp, contrast text 4.5:1 và meaningful icon/border 3:1.
- Chạy Compose tests cho callback, semantics, filter, chips, menu, swipe và fixed CTA.
- Screenshot phải xác nhận không overlap, không blank preview và nội dung không bị CTA/IME/system bars che.
