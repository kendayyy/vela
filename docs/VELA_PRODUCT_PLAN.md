# Vela Product Plan

## 1. Tong quan

Vela la ung dung ghi chu Android offline-first, local-only va privacy-first. San pham toi uu cho mot vong lap ngan: mo ung dung, viet, va thoat ma khong phai quan ly tai khoan hay cho dong bo.

**Product promise:** `Open. Write. Done.`

### Doi tuong su dung

- Nguoi can ghi nhanh y tuong, danh sach, nhat ky ngan hoac noi dung Markdown.
- Nguoi uu tien toc do, su rieng tu va kha nang su dung khi khong co mang.
- Nguoi khong muon tao tai khoan hoac phu thuoc vao dich vu cloud.

### Nguyen tac san pham

1. Offline-first: moi luong chinh phai hoat dong khi khong co Internet.
2. Local-only mac dinh: du lieu nam tren thiet bi va khong tu dong roi khoi thiet bi. Local-only khong dong nghia du lieu da duoc ma hoa.
3. Nhanh va toi gian: tu luc mo app den luc nhap noi dung phai it thao tac.
4. An toan du lieu: autosave, soft delete va migration duoc uu tien hon tinh nang trang tri.
5. MVP gon: chi them abstraction va tinh nang khi chung giai quyet nhu cau hien tai.
6. Co duong mo rong: schema va boundary cho phep them ma hoa va sync sau nay ma khong dua chung vao MVP.

## 2. Pham vi phien ban

### v0.1 - Writing and local media foundation

Muc tieu la chung minh trai nghiem cot loi tu dau den cuoi:

- Khoi tao app Kotlin, Jetpack Compose, Material 3, Hilt, Room va DataStore.
- Hien thi danh sach ghi chu tren Home, ghim truoc va sap xep theo lan cap nhat gan nhat.
- Tao ghi chu moi va mo Editor ngay lap tuc.
- Sua tieu de va noi dung theo cac text block co the chua Markdown syntax.
- Chen media block tai vi tri con tro: anh tu camera/thu vien, scan tai lieu, audio va tep.
- Scanner local nhieu trang: nhan bien, crop, perspective correction, filter, PDF va OCR de tim kiem.
- Ghi am foreground: pause/resume, waveform, seek va playback speed.
- Tim kiem title, text block va OCR text bang Room FTS.
- Autosave debounce `500ms` va flush khi roi Editor.
- Xoa mem ghi chu; Home khong hien thi ghi chu da xoa.
- Dark mode theo he thong, kem tuy chon theme luu trong DataStore neu con du dia milestone.
- Trang thai rong, loading va loi toi thieu.

### Cat khoi v0.1

Nhung tinh nang sau co gia tri, nhung khong nam tren critical path cua trai nghiem viet:

- Favorite, archive, duplicate va undo delete.
- Trash UI, restore va permanent delete.
- Folder, tag va quan he Note-Tag.
- Rich text WYSIWYG, transcription, video va drawing.
- Backup/import day du attachment va Markdown export.
- Tuy chon mau ghi chu.
- Biometric/PIN, database encryption va encrypted backup.
- Widget, reminder, backlink, cloud sync, desktop va web.

### v0.2 - Retrieval and lifecycle
- Favorite, archive, duplicate.
- Trash: danh sach, restore, permanent delete va empty trash.
- Favorites va Archive la cac destination rieng hoac filtered screen dung chung.
- Backup/import canonical gom day du attachment; Markdown export la tuy chon chia se.

### v0.3 - Organization and portability

- Folder, tag va quan he nhieu-nhieu Note-Tag.
- Markdown preview nhe neu nhu cau duoc xac nhan.
- Export tung note hoac nhieu note thanh Markdown nang cao.
- Toi uu relevance/tokenizer FTS neu benchmark cho thay can thiet.

### v0.4 - Protection and platform integration

- Biometric/PIN app lock.
- Encrypted backup va danh gia SQLCipher/database encryption.
- Widget va cac integration Android huu ich.

### Tuong lai, khong cam ket

- Video attachment, reminder, backlink/wiki link.
- Opt-in sync, desktop va web.
- Collaboration va sharing nang cao.

## 3. Non-goals cua MVP

- Khong backend, Firebase, account, authentication hoac cloud sync.
- Khong Internet permission cho cac luong cot loi.
- Khong rich text engine hoac Markdown renderer phuc tap.
- Khong background cleanup bat buoc cho Trash.
- Khong Clean Architecture nhieu lop neu UseCase khong mang lai logic dung chung.
- Khong toi uu cho hang tram nghin note truoc khi co do luong thuc te.

## 4. Trai nghiem nguoi dung

### Home

- Edge-to-edge voi large collapsing title; Focus Capsule chi hien khi co save/scan/OCR/recording status.
- Man hinh goc la `Thu muc`; `Tat ca`, `Da ghim`, `Da xoa` va folder nguoi dung la grouped rows.
- Note row hien title, preview mo, timestamp phai va hairline divider; swipe trai cho archive/delete.
- Bottom toolbar co create-note icon amber; khong dung Material FAB.
- Empty state ngan gon, khong can onboarding nhieu buoc.

### Editor

- Android back, title, ordered blocks, timestamp va overflow menu.
- Khi tao moi, focus vao title hoac body theo ket qua usability test; ban dau chon title.
- Khong hien hop thoai chon loai note.
- Text block luu Markdown; media block duoc chen tai con tro va hien inline.
- Formatting bar nam tren IME; insert sheet gom Scan, Camera, Photos, Record audio va Attach file.
- Menu mo rong theo roadmap: Pin, Favorite, Move, Archive, Duplicate, Delete.
- Trang thai save nhe (`Saving`, `Saved`, `Could not save`) va khong lam gian doan viec go.

### Search va collection screens

- Search hien ket qua khi nguoi dung go, debounce nhe de tranh query du thua.
- Folder, Favorites, Archive va Trash dung cung note list component va filter rieng.
- Khong dung bottom navigation trong MVP; navigation tu Home drawer/menu hoac Settings/overflow se gon hon voi so destination ban dau.

### Settings va Backup

- Thong diep privacy: ghi chu o lai tren thiet bi. Local-only khong dong nghia da ma hoa; v0.1 khong co app lock hay encryption tuy chinh.
- SAF duoc dung de nguoi dung chon noi export/import; app khong tu y truy cap file ben ngoai.

## 5. Kien truc de xuat

```text
Compose UI
    -> ViewModel (StateFlow + events)
        -> Repository interface
            -> Room DAO / DataStore / local files
```

- ViewModel so huu immutable `UiState`, nhan UI events va quan ly coroutine.
- Repository dong vai tro boundary cho persistence va transaction.
- UseCase chi duoc them khi co logic nghiep vu tai su dung, vi du import backup, empty trash hoac duplicate note phuc tap.
- UI khong truy cap DAO truc tiep.
- Domain model chi tach khoi entity khi mapping thuc su can thiet; v0.1 co the dung model gon tai repository boundary.
- `Flow` cho danh sach va truy van quan sat; suspend function cho command create/update/delete.

## 6. Mo hinh du lieu Room

### NoteEntity

| Field | Type | Quy tac |
| --- | --- | --- |
| `id` | String | UUID, primary key |
| `title` | String | Mac dinh rong |
| `content` | String | Markdown source, mac dinh rong |
| `createdAt` | Long | Epoch millis, khong thay doi |
| `updatedAt` | Long | Epoch millis, cap nhat khi noi dung/metadata thay doi |
| `isPinned` | Boolean | Mac dinh false |
| `isFavorite` | Boolean | Mac dinh false |
| `isArchived` | Boolean | Mac dinh false |
| `isDeleted` | Boolean | Soft delete, mac dinh false |
| `deletedAt` | Long? | Ho tro future auto-purge |
| `folderId` | String? | Foreign key nullable |
| `color` | String? | Reserved, khong can UI trong v0.1 |

Noi dung note khong con duoc luu trong mot cot `content` duy nhat. `NoteEntity` giu metadata; noi dung duoc luu thanh cac block co thu tu.

### NoteBlockEntity

| Field | Type | Quy tac |
| --- | --- | --- |
| `id` | String | UUID, primary key |
| `noteId` | String | FK den Note, cascade khi permanent delete |
| `type` | String | `TEXT`, `IMAGE`, `SCAN`, `AUDIO`, `FILE` |
| `position` | Long | Thu tu on dinh trong note |
| `textContent` | String? | Markdown source cho `TEXT` |
| `attachmentId` | String? | FK nullable cho media block |
| `createdAt` / `updatedAt` | Long | Epoch millis |

### AttachmentEntity

| Field | Type | Quy tac |
| --- | --- | --- |
| `id` | String | UUID, primary key |
| `noteId` | String | Owner note |
| `kind` | String | `IMAGE`, `SCAN_PDF`, `AUDIO`, `FILE` |
| `relativePath` | String | Path trong app-private storage |
| `displayName` / `mimeType` | String | Metadata hien thi/mo tep |
| `sizeBytes` | Long | Kiem soat quota va UI |
| `width` / `height` | Int? | Anh/preview |
| `durationMs` | Long? | Audio |
| `pageCount` | Int? | Scan PDF |
| `ocrText` | String? | Text index, khong chen vao body |
| `checksum` | String | Kiem tra backup/corruption |

Tep duoc ghi vao staging, sau do atomic move den `filesDir/attachments/{noteId}/{attachmentId}` truoc khi metadata duoc commit. Soft delete giu tep; permanent delete moi xoa. Startup maintenance don staging cu va orphan file.

Indexes du kien: `updatedAt`, `isPinned`, `isFavorite`, `isArchived`, `isDeleted`, `folderId`; co the dung composite index phu hop query sau khi do luong.

### FolderEntity

- `id: String` UUID, primary key.
- `name: String`.
- `createdAt: Long`, `updatedAt: Long`.
- Xoa folder nen dat `folderId = null` cho note thay vi xoa note.

### TagEntity

- `id: String` UUID, primary key.
- `name: String`, unique theo quy tac normalize da chon.
- `createdAt: Long`.

### NoteTagCrossRef

- Composite primary key: (`noteId`, `tagId`).
- Foreign key den Note va Tag.
- Xoa Note/Tag thi cascade cross-ref, khong cascade sang entity con lai.
- Index rieng tren `tagId` de query nguoc.

### Database va migration

- Bat dau schema version `1`; khong dung destructive migration trong production.
- Moi thay doi schema co migration va migration test.
- Export Room schema vao version control khi Android project duoc scaffold.
- Timestamp duoc tao tai repository bang mot `Clock` co the test, khong nam trong Composable.

## 7. DAO va repository contract

### NoteDao du kien

- Observe active notes, sap xep pinned truoc roi `updatedAt DESC`.
- Observe mot note theo ID.
- Insert/upsert note.
- Update content va metadata.
- Soft delete, restore va permanent delete.
- Observe favorites, archived va trash.
- Search active notes bang escaped `LIKE` tren title/content.
- Duplicate trong transaction neu sau nay co them relation.

### Repository du kien

- `observeNotes()`, `observeNote(id)`, `createNote()`, `saveNote()`.
- `setPinned()`, `setFavorite()`, `archive()`, `softDelete()`, `restore()`.
- `deletePermanently()` va `emptyTrash()` o v0.1, chi khi nguoi dung chu dong; khong auto-purge.
- `duplicateNote()` o v0.2.
- `searchNotes(query)` o v0.1 bang Room FTS.
- `searchNotes(query)` o v0.2.
- Repository chuan hoa timestamp, transaction va mapping loi persistence.

## 8. State management

### HomeUiState

- `isLoading`, `pinnedNotes`, `notes`, `errorMessage`.
- Empty state duoc suy ra tu danh sach sau loading.

### EditorUiState

- `noteId`, `title`, `content`, metadata va `saveStatus`.
- `isNew`, `isLoading`, `errorMessage`.
- UI event cap nhat state ngay lap tuc; save chay trong ViewModel.

### Autosave contract

1. Tao note moi ngay khi vao Editor de co ID on dinh.
2. Moi thay doi title/content cap nhat state ngay.
3. Ket hop cac thay doi va debounce `500ms` truoc khi ghi Room.
4. Dung `distinctUntilChanged` de bo save trung lap.
5. Khi Back hoac ViewModel sap ket thuc, flush snapshot chua luu bang suspend save trong event dieu huong.
6. Navigation chi hoan tat sau khi flush thanh cong hoac UI da thong bao loi va giu du lieu trong state.
7. Save duoc serialize de ban ghi cu khong ghi de ban moi.

`onCleared()` khong phai diem save dam bao vi khong the cho coroutine hoan tat; luong thoat chu dong phai goi flush.

## 9. Search strategy

### MVP: Room FTS

- FTS index gom title, Markdown text block va OCR text cua scan.
- Search index duoc rebuild transactionally khi title, text block hoac OCR thay doi.
- Chi tra note khong deleted; collection filter duoc ap dung sau truy van ID ket qua.
- Benchmark 100, 1.000 va 10.000+ note, bao gom OCR payload dai; muc tieu p95 100-150ms tren thiet bi muc tieu.

## 10. Backup, import va export

### Canonical backup

```text
vela-backup.zip
|-- manifest.json
|-- notes.json
|-- folders.json
|-- tags.json
`-- attachments/     # reserved for future versions
```

- `manifest.json`: `format`, `formatVersion`, `createdAt`, `appVersion`, record counts va checksum neu can.
- JSON dung UTF-8, field names on dinh, ID UUID va timestamp epoch millis.
- Import validate manifest/version truoc, giai nen vao bo nho/temporary storage co gioi han va dung Room transaction.
- Version moi phai doc duoc cac version backup con duoc ho tro; khong silently import format khong biet.
- Duplicate ID: mac dinh tao ID moi va bao cao so ban ghi duplicate, de khong ghi de du lieu local.
- Corrupted/partial backup: huy transaction va hien loi co the hanh dong, khong de database o trang thai nua import.
- Markdown export la dinh dang chia se, khong phai backup canonical vi lam mat metadata va relations.

## 11. Privacy va security

- Khong account, analytics, ads hoac network dependency trong MVP.
- Khong log title/content va khong dua note text vao crash report.
- Backup chi tao khi nguoi dung chu dong yeu cau.
- UI noi ro du lieu nam tren thiet bi va uninstall co the xoa du lieu neu chua backup.
- Kien truc dat boundary de them biometric/PIN, Android Keystore, encrypted backup va database encryption sau nay.
- Khong tuyen bo du lieu da ma hoa o at-rest trong MVP neu chi dua vao bao ve mac dinh cua Android.

## 12. Error handling

- Room read error: giu UI on dinh, hien retry va log thong tin khong nhay cam.
- Save error: hien `Could not save`, giu ban nhap trong state va cho retry.
- Import/export error: phan biet permission, het dung luong, file malformed, version khong ho tro va I/O failure.
- Migration failure: khong fallback destructive; bao loi va giu database de co the phuc hoi.
- Snackbar dung cho action ngan han nhu undo; loi can quyet dinh dung dialog/screen state.

## 13. Performance

- Dung `LazyColumn`, key on dinh bang note ID va immutable UI models.
- Select chi cac cot can thiet cho list neu profile cho thay content lon gay ton kem.
- Khong collect cung Flow nhieu lan; dung `stateIn`/`shareIn` tai ViewModel khi phu hop.
- Debounce autosave va search; tranh tao object/format timestamp trong moi recomposition.
- Query va index phai duoc kiem tra bang benchmark thuc te, dac biet tai 10.000 note.
- Attachment khong luu blob trong Room; Room chi luu metadata va app-private relative path.

## 14. Testing strategy

- Unit: repository timestamp/mapping, ViewModel state, autosave debounce/flush/order, search escaping, trash rules.
- Database: DAO queries, sort/filter, soft delete/restore, relation, transaction va moi migration.
- Compose UI: create/edit note, quay lai Home, search, delete + undo, restore Trash.
- Import/export: round-trip, version cu, duplicate IDs, malformed archive, interrupted import.
- Performance: Home list va search voi 100/1.000/10.000+ fixture notes.
- Manual: process death, rotation, dark mode, ban phim, Back gesture va thiet bi dung luong thap.

## 15. Navigation

```text
Home
|-- Editor(noteId)
|-- Search
|-- Favorites
|-- Archive
|-- Trash
`-- Settings
    `-- Backup & Import

Folder(folderId) -> Editor(noteId)
```

- Dung Navigation Compose voi typed route neu version thu vien duoc chon ho tro on dinh.
- Truyen ID qua route, khong truyen object Note.
- Sau create, Editor nhan ID vua tao; Back quay ve nguon mo note.

## 16. Cau truc project du kien

```text
app/src/main/java/com/vela/notes/
|-- VelaApplication.kt
|-- MainActivity.kt
|-- core/
|   |-- time/
|   `-- ui/
|-- data/
|   |-- local/
|   |   |-- dao/
|   |   |-- entity/
|   |   |-- relation/
|   |   |-- migration/
|   |   `-- VelaDatabase.kt
|   |-- preferences/
|   |-- attachment/
|   |-- scanner/
|   |-- audio/
|   |-- backup/
|   `-- repository/
|-- domain/
|   |-- model/
|   `-- repository/
|-- di/
|-- navigation/
`-- ui/
    |-- home/
    |-- editor/
    |-- scanner/
    |-- search/
    |-- collection/
    |-- settings/
    |-- backup/
    `-- theme/
```

`domain/usecase/` chi duoc tao khi co UseCase thuc su. Test mirror package production trong `src/test` va `src/androidTest`.

## 17. Critical path va dieu kien thanh cong

Critical path v0.1:

1. Project foundation va dependency injection.
2. Room schema, DAO, repository va migration policy.
3. Home observe danh sach.
4. Create note va navigation den Editor.
5. Editor state va autosave an toan.
6. Attachment storage va inline block editor.
7. Photo Picker, camera, file picker va audio.
8. Custom scanner, PDF, bundled OCR va FTS.
9. Delete mem toi thieu.
10. Test cot loi va polish theme/error/empty states.

v0.1 thanh cong khi nguoi dung co the mo app, tao note, go lien tuc, roi Editor ngay sau khi go, mo lai app va thay dung noi dung; tat ca khong can account hay ket noi mang.
