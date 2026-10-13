# Vela Google Play assets

Generated and verified on 2026-09-23.

## App icon

- Play Console icon: `icon/vela-play-icon-512.png` (`512 x 512`, RGB PNG).
- Device verification: `icon/vela-icon-device-check.png` shows the adaptive icon rendered by the Android launcher.
- The launcher includes legacy, round and Android 8+ adaptive variants with a white background and safe-zone foreground.

## Phone screenshots

Upload in this order from `phone/`:

1. `01-dashboard.png` - Dashboard with three real note summaries.
2. `02-editor.png` - Cream editor with title, body and formatting dock.
3. `03-attachments.png` - Attachment sheet for scan, camera, gallery, audio and files.
4. `04-search.png` - Debounced note search with a matching result.
5. `05-dashboard-dark.png` - Dashboard in dark mode.
6. `06-editor-dark.png` - Editor in dark mode.

Every screenshot is a 24-bit RGB PNG at 1080 x 1920 (9:16), with no alpha channel. The images show the real app UI and contain no device frame, ranking, price or promotional claim.

Suggested Vietnamese alt text, each under 140 characters:

1. `Danh sách ghi chú Vela với ô tìm kiếm, bộ sưu tập và ba ghi chú mẫu.`
2. `Trình soạn thảo Vela với nền giấy, nội dung ghi chú và thanh định dạng.`
3. `Bảng thêm nội dung gồm quét tài liệu, chụp ảnh, chọn ảnh, ghi âm và tệp.`
4. `Tìm kiếm ghi chú Vela theo từ khóa và hiển thị kết quả phù hợp.`
5. `Danh sách ghi chú Vela trong giao diện tối.`
6. `Trình soạn thảo ghi chú Vela trong giao diện tối.`

## Preview video

`video/vela-play-preview.mp4`

- H.264, yuv420p
- 1080 x 1920 portrait
- 30 fps
- 25.77 seconds
- No black bars, touch indicators or permission dialogs
- Real flow: dashboard, editor, attachment sheet and search

Google Play accepts a YouTube URL rather than a direct MP4 upload. Upload this file to YouTube, disable monetisation/ads, use a clean thumbnail, then paste the video URL without timecode or playlist parameters into Play Console.

`video/vela-play-preview-contact-sheet-detailed.png` is QA-only and must not be uploaded as a store screenshot.

## Verification

Executed from `E:/vela/code` with an isolated Gradle user home:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Results:

- Assemble: pass
- Unit tests: pass
- Lint: pass
- Connected instrumentation on `Luvin_API_35` / API 35: 51 of 51 pass

The first combined connected invocation started before the emulator was online and reported `No connected devices`; it was rerun after boot and passed all 51 tests.
