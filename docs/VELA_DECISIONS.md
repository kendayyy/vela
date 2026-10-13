# Vela Architecture Decision Records

Tai lieu nay ghi cac quyet dinh co anh huong dai han. Trang thai gom `Accepted`, `Proposed`, `Superseded`. Khi thay doi quyet dinh, tao ADR moi va lien ket ADR cu thay vi sua mat ly do lich su.

## ADR-001 - Offline-first va local-only mac dinh

- **Status:** Accepted
- **Context:** Vela uu tien toc do, rieng tu va hoat dong khong mang. Account/backend se tang do phuc tap va thay doi product promise.
- **Decision:** MVP khong account, backend, Firebase hay cloud sync. Room la local source of truth. Khong can Internet permission cho core app.
- **Consequences:** Trai nghiem don gian va privacy claim ro rang; nguoi dung tu chiu rui ro mat du lieu neu uninstall/mat may cho den khi backup co mat. Sync sau nay phai la opt-in va duoc thiet ke rieng.

## ADR-002 - Kotlin, Compose va Material 3

- **Status:** Accepted
- **Context:** Day la Android app moi, can UI hien dai va state-driven.
- **Decision:** Dung Kotlin, Jetpack Compose, Material 3, Coroutines/Flow va Navigation Compose.
- **Consequences:** Mot UI stack nhat quan, ho tro dark mode tot; can quan ly recomposition, lifecycle collection va version compatibility.

## ADR-003 - Room la persistence layer

- **Status:** Accepted
- **Context:** Note can query, sort, filter, relation va migration; raw files/DataStore khong phu hop lam database chinh.
- **Decision:** Dung Room. DataStore chi luu preference nho nhu theme, khong luu note.
- **Consequences:** Co compile-time query checks va migration framework; schema thay doi phai duoc version/test. Khong dung destructive migration trong production.

## ADR-004 - UUID dang String cho entity ID

- **Status:** Accepted
- **Context:** Backup/import, duplicate va kha nang sync tuong lai can ID khong phu thuoc mot database local.
- **Options considered:** Auto-increment Long; UUID String; UUID luu blob.
- **Decision:** UUID string cho Note, Folder va Tag.
- **Consequences:** De export/remap va tao ID offline; ton storage/index hon Long. Day la tradeoff chap nhan duoc o quy mo note ca nhan.

## ADR-005 - Kien truc gon UI -> ViewModel -> Repository -> Room

- **Status:** Accepted
- **Context:** Clean Architecture day du de tao nhieu boilerplate cho MVP.
- **Decision:** UI goi ViewModel; ViewModel goi repository. Chi them UseCase khi logic tai su dung hoac phuc tap nhu import/empty trash. UI khong goi DAO.
- **Consequences:** De doc va test, it lop. Can ky luat de ViewModel khong thanh noi chua moi nghiep vu; tach coordinator/use case khi complexity thuc su xuat hien.

## ADR-006 - StateFlow va immutable UI state

- **Status:** Accepted
- **Context:** Compose can state on dinh, de quan sat va test.
- **Decision:** Moi screen co immutable `UiState` trong `StateFlow`; command mot lan duoc mo hinh hoa bang event/result phu hop, khong dat navigation raw trong persistent state neu co the bi replay sai.
- **Consequences:** Luong state ro rang va test duoc; can xu ly lifecycle collection va one-off effect can than.

## ADR-007 - Markdown source, plain text editor trong MVP

- **Status:** Superseded by ADR-019
- **Context:** Plain text la don gian nhat; rich text ton kem; Markdown portable va mo rong tot.
- **Options considered:** Plain text thuan; Markdown; rich text document model.
- **Decision:** Luu `content` la Markdown source. Editor v0.1 la plain text, khong toolbar va khong renderer phuc tap. Preview duoc danh gia o v0.3.
- **Consequences:** Noi dung portable va backward-compatible; nguoi khong biet Markdown van go text binh thuong. Formatting WYSIWYG khong co trong MVP.

## ADR-008 - Autosave debounce 500ms va flush khi thoat

- **Status:** Accepted
- **Context:** Save moi keystroke gay write du thua; chi debounce co the mat ky tu khi nguoi dung thoat ngay.
- **Decision:** Cap nhat UI state ngay, debounce save `500ms`, bo snapshot trung lap, serialize writes va explicit flush khi xu ly Back/navigation. Neu ViewModel bi clear khi van con ban sua chua debounce, `onCleared()` ghi ban do xuong Room truoc khi thoat.
- **Consequences:** Typing muot va write it hon, trong khi Back chu dong giu du lieu. Process death chi giu noi dung da flush vao Room; `onCleared()` khong chay khi tien trinh bi giet. Implementation phai test race/stale write.

## ADR-009 - Soft delete va Trash

- **Status:** Accepted
- **Context:** Xoa nham note la rui ro lon trong app autosave/local-only.
- **Decision:** Delete dat `isDeleted = true` va `deletedAt`; active queries loai note da xoa. v0.1 co Snackbar hoan tac, danh sach Da xoa, restore, permanent delete va empty trash. Xoa vinh vien chi xoa file khi khong con ghi chu nao tham chieu.
- **Consequences:** Du lieu co the phuc hoi; query phai nhat quan ve state. Auto-purge 30 ngay van la future feature, khong co background worker trong v0.1.

## ADR-010 - Search LIKE truoc, FTS theo du lieu do luong

- **Status:** Superseded by ADR-021
- **Context:** FTS manh hon nhung can virtual table, migration va sync semantics. MVP can search don gian.
- **Options considered:** SQL `LIKE`; Room FTS ngay lap tuc; in-memory filtering.
- **Decision:** v0.2 dung escaped `LIKE` tren title/content trong database. Benchmark 100/1.000/10.000+ note; chuyen FTS neu latency p95 anh huong trai nghiem, muc tieu khoang 100-150ms tren thiet bi muc tieu.
- **Consequences:** Ship nhanh va query de hieu; substring search tren dataset lon co the cham. Repository boundary giup thay implementation.

## ADR-011 - Folder/Tag relational schema

- **Status:** Accepted
- **Context:** Folder la mot-nhieu; Tag la nhieu-nhieu. Chen danh sach tag vao text/JSON se lam query va integrity kho.
- **Decision:** `Note.folderId` nullable; `FolderEntity`, `TagEntity` va `NoteTagCrossRef` voi composite primary key. Xoa Folder set null; xoa Note/Tag cascade cross-ref.
- **Consequences:** Query va integrity ro rang; can migration va transaction. Table chi duoc them khi v0.3 implementation bat dau.

## ADR-012 - Canonical backup la ZIP co version va JSON

- **Status:** Accepted
- **Context:** Backup can giu metadata, relation va attachment future; Markdown khong du.
- **Decision:** `vela-backup.zip` gom `manifest.json`, `notes.json`, `folders.json`, `tags.json`, va reserved `attachments/`. Manifest co `formatVersion`. Markdown export la tinh nang chia se rieng.
- **Consequences:** Co the round-trip va evolve format; importer phai validate version, size, path va corruption. Can fixture compatibility lau dai.

## ADR-013 - Import duplicate khong ghi de local data

- **Status:** Accepted
- **Context:** Import backup vao database dang co co the trung UUID; silently overwrite co nguy co mat du lieu.
- **Options considered:** Overwrite; skip; tao ID moi; hoi tung note.
- **Decision:** Mac dinh tao ID moi cho record trung va remap relations, sau do bao cao ket qua. Import chay atomic transaction.
- **Consequences:** Bao toan du lieu local nhung co the tao ban sao. UI/report can minh bach va importer phai giu map ID cu-moi.

## ADR-014 - Khong encryption/auth trong MVP

- **Status:** Accepted
- **Context:** Biometric, PIN, Keystore va encrypted DB/backup co nhieu edge case phuc hoi va key lifecycle.
- **Decision:** Khong ship app lock hay encryption tuy chinh trong v0.1-v0.3. Khong tuyen bo Vela da ma hoa du lieu. Giu persistence/backup boundary de them sau threat model.
- **Consequences:** MVP don gian hon nhung khong phu hop voi threat model can bao ve khi thiet bi da mo khoa. Privacy message phai chinh xac: local-only khong dong nghia end-to-end encryption.

## ADR-015 - Navigation khong dung bottom bar trong MVP

- **Status:** Accepted
- **Context:** v0.1 chi co Home va Editor; bottom navigation se tao destination trong va tang chrome.
- **Decision:** Dung single activity + Navigation Compose. Home la start destination; Editor/Search/collections/Settings la route. Them drawer/menu hoac entry point phu hop khi v0.2 co nhieu collection.
- **Consequences:** v0.1 tap trung vao noi dung. Navigation pattern co the duoc danh gia lai khi so destination thuong dung tang.

## ADR-016 - Backup va file access qua Storage Access Framework

- **Status:** Accepted
- **Context:** Android scoped storage va privacy yeu cau nguoi dung chon file/location.
- **Decision:** Export/import dung SAF; app khong yeu cau broad storage permission.
- **Consequences:** Hop voi Android va quyen nguoi dung; can xu ly provider I/O, permission va interrupted operation.

## ADR-017 - Khong auto-purge Trash trong MVP

- **Status:** Accepted
- **Context:** Auto-delete 30 ngay can policy, clock, background scheduling va UX thong bao.
- **Decision:** v0.1 chi co permanent delete va empty trash khi nguoi dung chu dong. Giu `deletedAt` de co the them auto-purge sau. Khong dung WorkManager.
- **Consequences:** Trash co the tang dung luong cho den khi nguoi dung don; tranh background behavior khong bat ngo trong MVP.

## ADR-018 - No application code truoc khi planning duoc review

- **Status:** Accepted
- **Context:** Repo khoi dau rong va yeu cau dat product/architecture boundary truoc scaffold.
- **Decision:** Bon file trong `docs/` la artifact dau tien. Android scaffold chi bat dau sau khi scope v0.1, baseline SDK/toolchain va ADR duoc review.
- **Consequences:** Giam rework va scope drift; buoc implementation dau tien la Phase 0 trong task checklist.

## Open decisions truoc khi code

Nhung muc nay can chot o Phase 0, khong can chan viec planning:

- Min SDK, target SDK, JDK, AGP, Kotlin va dependency versions cu the.
- Title hay body nhan focus dau tien sau usability test; mac dinh hien tai la title.
- Theme setting co ship v0.1 hay chi follow system de giu scope gon.
- Quy tac note rong: giu note rong hay tu xoa khi roi Editor chua nhap gi; de xuat tu xoa neu ca title/content deu rong va note chua tung co noi dung.
- Device baseline dung de dat performance threshold.

## ADR-019 - Tai lieu block: Markdown text va inline media

- **Status:** Accepted
- **Decision:** Note gom `NoteBlockEntity` co thu tu. Text block luu Markdown source; media block tham chieu `AttachmentEntity`. Chen media tai con tro tach text block, xoa media co the noi hai text block ke nhau.
- **Consequences:** Ho tro anh/scan/audio/file nam dung vi tri ma khong can rich-text engine; autosave phai transactionally luu block order.

## ADR-020 - Attachment nam trong app-private storage

- **Status:** Accepted
- **Decision:** Anh/tep chon qua Photo Picker/SAF duoc copy vao app-private storage bang staging + atomic move. Room chi luu metadata/path. Anh duoc sua orientation va loai GPS EXIF.
- **Consequences:** Offline va ben vung sau reboot/provider thay doi; app phai quan ly quota, orphan cleanup va backup attachment.

## ADR-021 - FTS trong v0.1 cho text va OCR

- **Status:** Accepted
- **Decision:** Room FTS index title, Markdown text va OCR text ngay tu v0.1; khong bat dau bang `LIKE` vi OCR payload co the lon.
- **Consequences:** Search nhanh hon nhung can transaction/rebuild test va migration discipline.

## ADR-022 - Scanner local do Vela kiem soat

- **Status:** Accepted
- **Decision:** CameraX + OpenCV cho preview, edge detection, auto/manual crop, perspective correction, filters va multi-page PDF. Khong dung document-scanner flow tai dong cua Google Play Services.
- **Consequences:** Hoat dong offline va UI nhat quan, doi lai scope test camera/image processing lon hon.

## ADR-023 - OCR bundled, chi dung cho search

- **Status:** Accepted
- **Decision:** Dung ML Kit Text Recognition Latin model bundled. OCR chay sau khi xac nhan trang, luu text vao attachment metadata/FTS; khong chen vao body.
- **Consequences:** App tang kich thuoc nhung OCR co san offline va khong lam bien doi noi dung nguoi dung.

## ADR-024 - Audio foreground va Media3 playback

- **Status:** Accepted
- **Decision:** MediaRecorder ghi AAC/M4A khi app foreground; Media3 phat, seek va doi toc do. Waveform la peak data tinh local. Background transition dung va giu ban ghi tam an toan.
- **Consequences:** Can runtime microphone permission va test tren thiet bi that; khong co transcription/background recording trong v0.1.

## ADR-025 - Vela Focus Capsule thay Dynamic Island gia

- **Status:** Accepted
- **Decision:** App edge-to-edge va ton trong display cutout Android. Capsule chuc nang nam duoi status bar hien save/scan/OCR/recording status; khong ve hardware pill gia.
- **Consequences:** Giu nhip UI da tham khao nhung thich ung nhieu thiet bi va tao nhan dien Vela rieng.

## ADR-026 - Notes-first start destination

- **Status:** Accepted
- **Decision:** `AllNotes` là start destination với title `Ghi chú`. Pinned, Folders và Deleted được mở từ collection chips; secondary routes dùng Android Back. Không thêm bottom navigation hoặc tag system.
- **Consequences:** Luồng tạo/tìm ghi chú ngắn hơn và gần ảnh tham chiếu; Collections screen vẫn tồn tại nhưng không còn là màn hình đầu tiên.

## ADR-027 - Cobalt primary và semantic media colors

- **Status:** Accepted
- **Decision:** Cobalt `#315EFB` light / `#8EA8FF` dark là primary. Amber dành cho scanner/document; teal dành cho success/focus/Capsule; paper `#FFFEF7` dành cho editor light.
- **Consequences:** Vela có nhận diện riêng và CTA rõ hơn; screen code phải dùng semantic theme token, không coi amber là primary toàn app.

## ADR-028 - Note summary projection không migration

- **Status:** Accepted
- **Decision:** Dashboard dùng domain `NoteSummary` gồm note, preview và primary content type. Repository kết hợp batch note/block queries; không thêm cột hay bảng chỉ để phục vụ UI.
- **Consequences:** Card phản ánh nội dung và tránh N+1; list observation đọc block metadata liên quan nhưng Room schema vẫn version 1.

## ADR-029 - App Lock dùng Android biometric va device credential

- **Status:** Accepted
- **Decision:** App Lock là opt-in trong Cài đặt, dùng AndroidX Biometric với `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`. Preference và timeout lưu bằng DataStore; coordinator theo dõi process foreground/background và dùng elapsed realtime. Không tạo PIN Vela, không thêm SQLCipher hay mã hóa backup.
- **Consequences:** Không tự quản lý password/recovery; thiết bị cần screen lock hợp lệ. Tắt App Lock yêu cầu xác thực lại. Khi bật, `FLAG_SECURE` che Recent Apps và chặn screenshot/recording; privacy copy phải nói rõ đây không phải encryption at rest.
