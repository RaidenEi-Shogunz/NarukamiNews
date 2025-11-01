# ⚡ NarukamiNews – Ứng dụng đọc báo RSS hợp pháp, cá nhân hóa và hoạt động offline

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.x-blue?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-%237851F3?logo=jetpackcompose&logoColor=white)
![MVVM](https://img.shields.io/badge/Architecture-MVVM-informational)
![Hilt DI](https://img.shields.io/badge/DI-Hilt-success)
![Room](https://img.shields.io/badge/DB-Room-orange)
![Retrofit](https://img.shields.io/badge/Network-Retrofit%20%7C%20OkHttp-yellow)
![Firebase](https://img.shields.io/badge/Firebase-Auth%20%7C%20Firestore-ffca28?logo=firebase&logoColor=black)
![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)

NarukamiNews ⚡ là ứng dụng đọc tin tức dựa trên **RSS hợp pháp**, phát triển bằng **Kotlin + Jetpack Compose**, tối ưu **nhanh – mượt – offline-first** và **không sao chép nội dung gốc**. Ứng dụng chỉ hiển thị metadata (title, description, thumbnail, link gốc) và luôn **ghi rõ nguồn** theo nguyên tắc **Legal by Design**.

---

## 🔗 Mục lục
- [Tính năng chính](#-tính-năng-chính)
- [Kiến trúc & Công nghệ](#-kiến-trúc--công-nghệ)
- [Cấu trúc thư mục](#-cấu-trúc-thư-mục)
- [Yêu cầu hệ thống](#-yêu-cầu-hệ-thống)
- [Cách chạy dự án](#-cách-chạy-dự-án)
- [Ảnh minh họa](#-ảnh-minh-họa)
- [Nguyên tắc pháp lý RSS](#-nguyên-tắc-pháp-lý-rss-legal-by-design)
- [Hiệu năng & bảo mật](#-hiệu-năng--bảo-mật)
- [Lộ trình phát triển](#-lộ-trình-phát-triển)
- [Tài liệu tham khảo (APA)](#-tài-liệu-tham-khảo-apa)
- [Tác giả](#-tác-giả)
- [Giấy phép](#-giấy-phép)

---

## ✨ Tính năng chính
- 📰 **Đọc tin hợp pháp** từ các nguồn RSS công khai (VNExpress, Tuổi Trẻ, Thanh Niên, Zing News, Dân Trí, Vietnamnet…).
- 🔎 **Tìm kiếm** theo tiêu đề/mô tả; **lọc** theo nguồn.
- 📥 **Offline-first**: cache Room, đọc lại khi không có mạng.
- ⭐ **Bookmark** bài viết để xem lại nhanh.
- 🎨 **Tùy chỉnh chủ đề**: accent color, dark/light, font scale.
- 👤 **Đăng nhập Firebase Auth** (email/password).
- 🔧 **Admin Dashboard** (trong app): quản lý nguồn RSS, highlights, người dùng (role-based).
- 🌐 **Đọc hợp pháp** bằng WebView / Chrome Custom Tabs (chuyển tới link gốc, không copy nội dung).
- 🛠️ **CI/CD** bằng GitHub Actions (build, test, lint, artifact).

---

## 🧩 Kiến trúc & Công nghệ
- **Kiến trúc**: MVVM + Repository + Hilt (DI).
- **UI**: Jetpack Compose (Material 3, Animation, Scaffold, Snackbar).
- **Data**: Room (cache), Retrofit + OkHttp (RSS), Coroutines + Flow/StateFlow.
- **Firebase**: Authentication, Cloud Firestore (users, rss_sources, highlights).
- **Bảo mật**: Network Security Config (HTTPS), Firestore Security Rules, R8/Proguard, secrets tách khỏi repo.
- **Hiệu năng**: Baseline Profiles, LazyColumn tối ưu, Coil AsyncImage.

---

## 🗂 Cấu trúc thư mục
NarukamiNews/
├─ app/
│ ├─ data/
│ │ ├─ local/ # Room: Entity, Dao, Database, Migrations
│ │ ├─ remote/ # Retrofit service, RSS parser
│ │ └─ repository/ # Repository hợp nhất local + remote
│ ├─ ui/
│ │ ├─ screens/ # Home, Reader, Search, Bookmarks, Settings, Profile
│ │ └─ admin/ # AdminSources, AdminHighlights, AdminUsers
│ ├─ viewmodel/ # ViewModels (StateFlow)
│ ├─ di/ # Hilt modules
│ └─ utils/ # Helpers, mappers, constants
├─ gradle/ # Wrapper
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ .gitignore
└─ README.md

---

## 💻 Yêu cầu hệ thống
- **Android Studio** Ladybug (2025.1) hoặc mới hơn
- **JDK 17**
- **minSdk 24**, **targetSdk 34**
- Kết nối mạng lần đầu để đồng bộ RSS; sau đó có thể đọc offline

---

## 🚀 Cách chạy dự án
1. **Clone repo**
   ```bash
   git clone https://github.com/RaidenEi-Shogunz/NarukamiNews.git
   cd NarukamiNews
2. Mở bằng Android Studio → chờ Gradle sync.

3. Firebase: tải file google-services.json của dự án Firebase (KHÔNG commit lên Git) và đặt vào app/.

4. Run trên thiết bị Android 7.0+.

🔐 Secrets/API keys và google-services.json không nằm trong repo để đảm bảo bảo mật.

Nguyên tắc pháp lý RSS (Legal by Design)

Chỉ hiển thị metadata: title, description, thumbnail, link gốc.

Không lưu/chép nội dung toàn văn; người dùng được chuyển hướng tới trang báo gốc để đọc.

Luôn ghi nguồn rõ ràng.

Không chèn quảng cáo vào dữ liệu RSS.

Tuân thủ Luật ATTT mạng 2015 và Nghị định 13/2023/NĐ-CP về bảo vệ dữ liệu cá nhân.

⚡ Hiệu năng & bảo mật

Baseline Profiles: giảm thời gian cold start.

R8/shrinkResources: giảm dung lượng AAB.

Coil + LazyColumn: hạn chế jank%.

Network Security Config: chỉ cho phép HTTPS.

Firestore Rules (role-based):

admin: quản lý rss_sources, highlights, users

user: đọc công khai, ghi dữ liệu cá nhân của họ.

🗺 Lộ trình phát triển

v3 – Personalization / API Base: backend riêng (Ktor/Spring), gợi ý theo hành vi đọc.

v4 – Refactor & Optimization: multi-module (domain/data/ui), Paging 3, WorkManager prefetch, App Startup.

v5 – AI & Expansion: tóm tắt/gợi ý bằng AI, mở rộng đa nền tảng (WearOS/Web/Desktop), DevOps nâng cao.

TÀI LIỆU THAM KHẢO
Android Developers. (2025). Jetpack Compose Documentation – Modern toolkit for building native UI.
Retrieved from https://developer.android.com/jetpack/compose
Google Firebase. (2025). Cloud Firestore Documentation – NoSQL Database for Mobile, Web & Server.
Retrieved from https://firebase.google.com/docs/firestore
Square, Inc. (2025). Retrofit – Type-safe HTTP client for Android and Java.
Retrieved from https://square.github.io/retrofit/
Android Developers. (2025). Room Persistence Library – Save data in a local database using SQLite.
Retrieved from https://developer.android.com/training/data-storage/room
RSS Advisory Board. (2023). RSS 2.0 Specification – Standard format for web content syndication.
Retrieved from https://www.rssboard.org/rss-specification
## 👨‍💻 Nhóm thực hiện
| Thành viên | MSSV | Nhiệm vụ | Tỷ lệ |
|-------------|------|----------|--------|
| **Lê Minh Đạt** | 23210501010 | PPT, Word | 20% |
| **Nguyễn Nhật Duy** | 23210501008 | Word | 15% |
| **Chung Khánh Duy** | 23210501006 | Word | 15% |
| **Diệp Hoàng Thái** | 23210501019 | Code chính, Firebase Integration, Word | 50% |

**GVHD:** Thầy Trần Thanh Nhã  
**Trường Đại học Bình Dương – Phân hiệu Cà Mau**

---

## 🚀 Hướng dẫn chạy
1. Clone dự án:
   ```bash
   git clone https://github.com/RaidenEi-Shogunz/NarukamiNews.git
Mở trong Android Studio (Ladybug hoặc mới hơn).

Thêm file google-services.json của Firebase vào thư mục app/.

Build & Run trên Android 7.0+ (API 24 trở lên).

📘 Bản quyền
Phát hành theo giấy phép MIT License — bạn có thể sử dụng, chỉnh sửa và phân phối mã nguồn, miễn là giữ nguyên ghi chú bản quyền.
