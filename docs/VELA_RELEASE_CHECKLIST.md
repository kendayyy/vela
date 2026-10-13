# Vela v0.1 release checklist

Phien ban debug/release hien tai: `versionName` `0.1.0`, `versionCode` `1`, `applicationId` `com.vela.notes`.

## Privacy

- Local-only: ghi chu va tep nam trong app-private storage. Khong account, cloud, analytics, ads hay Firebase.
- Local-only khong dong nghia du lieu da duoc ma hoa. App Lock la UI gate opt-in, khong phai SQLCipher hay encrypted backup.
- Khi App Lock bat, `FLAG_SECURE` chan screenshot, quay man hinh va preview Recent Apps; mo khoa bang biometric hoac device credential Android.
- `android:allowBackup="false"`. `backup_rules.xml` va `data_extraction_rules.xml` loai root, file, database, sharedpref va external khoi cloud backup va device transfer.

## Manifest

- Quyen giu lai: `CAMERA`, `RECORD_AUDIO`. Camera khong bat buoc (`android.hardware.camera.any` `required=false`).
- `INTERNET` va `ACCESS_NETWORK_STATE` bi go bang `tools:node="remove"`.
- Khong co `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE` hay `MANAGE_EXTERNAL_STORAGE`.
- `android:enableOnBackInvokedCallback="true"`.

## FileProvider

- Authority `${applicationId}.files`, `exported=false`, `grantUriPermissions=true`.
- `file_paths.xml` chi mo `files-path` `attachments/`.
- Intent mo/chia se chi gan `FLAG_GRANT_READ_URI_PERMISSION`.

## Gioi han v0.1

- Tep 50 MB, ghi am 60 phut, scan 50 trang.
- Khong video, transcription, drawing, background recording, WYSIWYG hay backup cloud.
- Thung rac: xoa mem, hoan tac, khoi phuc, xoa vinh vien va don thung rac. Khong auto-purge.

## Truoc khi danh dau san sang

- `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest` pass.
- Merged manifest khong con `INTERNET` hay broad storage permission.
- Dependency report khong them analytics, ads, OkHttp hay Retrofit. ML Kit keo `transport-backend-cct` va `firebase-encoders`/`firebase-components` de model bundled khoi dong; app khong khai bao Firebase Analytics va manifest da go `INTERNET`.
