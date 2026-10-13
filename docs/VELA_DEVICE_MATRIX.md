# Vela v0.1 manual device matrix

Ma tran ngan cho may ao `Luvin_API_35` (API 35). Ung dung khong co quyen mang; moi buoc duoi day chay ngoai tuyen.

| Tinh huong | Thao tac | Ket qua mong doi |
| --- | --- | --- |
| Offline happy path | Tao ghi chu, sua, doi 1 giay, thoat, mo lai | Tieu de va noi dung da luu con nguyen |
| Xoay man hinh | Dang go trong editor, xoay ngang roi doc | Chu dang go khong mat, thanh dinh dang khong de tieu de |
| Process death | Sau khi chu da luu, giet tien trinh app va mo lai | Room mo lai dung noi dung da flush |
| Thoat som | Sua roi bam Back truoc 500ms | Ban sua van duoc ghi khi editor dong |
| Tu choi may anh / mic | Tu choi quyen khi chup hoac ghi am | Editor van mo, co cau bao quyen |
| May anh khong san sang | Dong man chup hoac man quet | Camera va phien phan tich duoc giai phong |
| URI loi | Huy bo chon tep, hoac chon URI khong doc duoc | Khong tao tep rac; hien loi dinh kem |
| Het dung luong | Import khi bo nho day | Hien "Khong du dung luong de luu tep."; staging bi xoa |
| Thung rac | Xoa mem, hoan tac, xoa vinh vien, don thung rac | File chi bi xoa khi khong con ghi chu nao tham chieu |

Kiem tra tu dong tuong ung: `ProcessRecreationRepositoryTest`, `EditorResilienceTest`, `AppPrivateAttachmentStoreTest.storageFailureRemovesTheStagedFile`, `CameraCaptureScreenTest.leavingTheScreenReleasesTheCamera`, `ScannerPreviewScreenTest.previewBindsAndCloseReleasesSession`.
