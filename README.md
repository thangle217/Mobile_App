# 📱 ĐỒ ÁN MÔN HỌC: NHẬP MÔN ỨNG DỤNG DI ĐỘNG
## ĐỀ TÀI: ỨNG DỤNG QUẢN LÝ NHÀ TRỌ (RENTAL MANAGER)

---

### 🏛️ THÔNG TIN CHUNG
* **Trường**: Đại học Công nghệ Thông tin - ĐHQG TP.HCM (UIT)
* **Khoa**: Kỹ thuật Phần mềm
* **Môn học**: Nhập môn ứng dụng di động - SE114
* **Đề tài**: Quản lý nhà trọ (Rental Manager)
* **Học kỳ**: Học kỳ 2 — Năm học 2025 - 2026

---

### 👥 DANH SÁCH THÀNH VIÊN NHÓM

| STT | Họ và tên | MSSV | Vai trò |
| :--- | :--- | :--- | :--- |
| 1 | Lê Trần Quang Thắng | 24521605 | Nhóm trưởng |
| 2 | Phan Viết Thiện | 24521666 | Thành viên |
| 3 | Hồ Đình Phước Thịnh | 24521679 | Thành viên |
| 4 | Nguyễn Thành Phát | 24521310 | Thành viên |

---

### 🌟 GIỚI THIỆU ỨNG DỤNG (PROJECT OVERVIEW)
**Rental Manager** là ứng dụng di động quản lý nhà trọ và căn hộ dịch vụ chuyên nghiệp, hỗ trợ giải quyết các khó khăn trong việc quản lý vận hành phòng trọ của cả **Chủ trọ** và **Người thuê**. 

Ứng dụng được thiết kế theo mô hình **Local-first (Ưu tiên ngoại tuyến)**, giúp dữ liệu được xử lý nhanh chóng và lưu trữ an toàn ngay trên thiết bị. Điều này tạo điều kiện chấm điểm cực kỳ thuận lợi cho giảng viên vì ứng dụng có thể **chạy độc lập ngay lập tức** mà không đòi hỏi bất kỳ cấu hình kết nối mạng hay cài đặt Firebase phức tạp nào từ phía người kiểm thử.

#### 💡 Điểm nổi bật kỹ thuật của dự án:
1. **Kiến trúc Local-first mạnh mẽ**: Sử dụng tầng dữ liệu cục bộ (`LocalAppStore`) được tối ưu hóa bằng JSON và lưu trữ có cấu trúc qua SharedPreferences, giúp trải nghiệm mượt mà, không phụ thuộc internet.
2. **Giao diện Jetpack Compose hiện đại**: Ứng dụng xây dựng 100% bằng Jetpack Compose với phong cách Material 3, có hiệu ứng chuyển cảnh mượt mà (`AnimatedContent`), tối ưu hóa cử chỉ vuốt chạm và hỗ trợ giao diện tối (Dark Mode) linh hoạt.
3. **Mô hình Clean Architecture & Repository Pattern**: Phân chia các lớp dữ liệu (`data`), nghiệp vụ (`domain`), giao diện (`ui`) rõ ràng, giúp mã nguồn có độ tùy biến cao, dễ đọc và bảo trì.

---

### ✨ CÁC TÍNH NĂNG CHÍNH ĐÃ THỰC HIỆN

Ứng dụng phân quyền rõ ràng thành 3 vai trò tương tác:

#### 1. Vai trò: Chủ trọ (Landlord)
* **Bảng điều khiển (Dashboard)**: Thống kê nhanh số phòng trống, số hóa đơn chưa thanh toán, tổng doanh thu thực tế và các công việc cần xử lý.
* **Quản lý nhà trọ & phòng trọ**: Thêm mới/chỉnh sửa/xóa các tòa nhà trọ, danh sách phòng trọ, loại phòng trọ, cấu hình đơn giá điện nước.
* **Quản lý khách thuê**: Quản lý hồ sơ thông tin cá nhân khách thuê, xem hình ảnh chứng minh/CCCD mặt trước và mặt sau.
* **Quản lý hợp đồng**: Lập hợp đồng mới, xem danh sách yêu cầu thuê, duyệt hợp đồng điện tử và thực hiện thanh lý hợp đồng.
* **Quản lý chỉ số Điện & Nước**: Ghi nhận chỉ số điện/nước hàng tháng của từng phòng trọ một cách trực quan.
* **Quản lý hóa đơn & thanh toán**: Tự động tính toán tiền phòng, tiền điện, nước và dịch vụ đi kèm để xuất hóa đơn tháng, cập nhật trạng thái thanh toán thủ công hoặc quét QR.
* **Tiếp nhận & xử lý sự cố**: Tiếp nhận báo cáo sự cố cơ sở vật chất từ khách thuê, cập nhật tiến độ sửa chữa (Đang chờ, Đang sửa, Đã hoàn thành).
* **Gửi thông báo**: Gửi tin báo chung, nội quy hoặc nhắc nhợ hóa đơn tự động tới từng khách thuê.

#### 2. Vai trò: Người thuê (Tenant)
* **Gửi yêu cầu thuê phòng**: Xem danh sách phòng trống và gửi yêu cầu đăng ký thuê trực tuyến.
* **Theo dõi hợp đồng cá nhân**: Tra cứu thông tin hợp đồng đang thuê, tiền cọc, ngày bắt đầu/hết hạn và gửi yêu cầu gia hạn hợp đồng.
* **Theo dõi hóa đơn & thanh toán**: Nhận hóa đơn điện tử hàng tháng, xem chi tiết chi phí và thực hiện thanh toán chuyển khoản qua tài khoản ngân hàng được chủ trọ cấu hình.
* **Báo cáo sự cố**: Gửi phản ánh hư hỏng cơ sở vật chất trực tiếp từ điện thoại (hỏng điện, nước, internet) kèm mô tả chi tiết để chủ trọ kịp thời xử lý.
* **Hộp thư thông báo**: Đọc thông báo chung của nhà trọ và các tin nhắn nhắc nợ tiền phòng.
* **Thông tin tài khoản**: Quản lý thông tin cá nhân và cập nhật hình ảnh CCCD để chủ trọ làm hợp đồng.

#### 3. Vai trò: Quản trị viên (Admin)
* **Quản lý danh sách người dùng**: Xem toàn bộ tài khoản chủ trọ và người thuê trong hệ thống.

---

### 🔑 TÀI KHOẢN TRẢI NGHIỆM SẴN CÓ (TEST CREDENTIALS)

> [!IMPORTANT]
> Để thuận tiện cho giảng viên chấm điểm, ứng dụng tích hợp sẵn cơ chế **Tự động Seed dữ liệu mẫu (Demo Data)** ngay lần đầu tiên mở ứng dụng. Giảng viên chỉ cần đăng nhập bằng các tài khoản bên dưới để kiểm thử tất cả chức năng mà không cần tự tạo dữ liệu từ đầu:

| Vai trò | Tên đăng nhập / Email | Mật khẩu | Mô tả dữ liệu test mẫu |
| :--- | :--- | :--- | :--- |
| **Quản trị viên** | `Admin` hoặc `admin@demo.local` | `Admin123` | Có quyền quản trị toàn hệ thống. |
| **Chủ trọ** | `chutro` hoặc `chutro@example.com` | `123456` | Tên: **Nguyễn Minh Quân**. Đã được cấu hình sẵn 5 nhà trọ mẫu, danh sách phòng trọ, khách thuê, hợp đồng và hóa đơn mẫu. |
| **Người thuê** | `nguoithue` hoặc `nguoithue@example.com` | `123456` | Tên: **Người Thuê Demo**. Đang thực hiện hợp đồng tại phòng **P101**, đã có sẵn hóa đơn cần thanh toán và sự cố mẫu đang chờ sửa. |

---

### 🛠️ CÔNG NGHỆ & THƯ VIỆN SỬ DỤNG
* **Kotlin**: Ngôn ngữ lập trình chính cho ứng dụng.
* **Jetpack Compose**: Thư viện thiết kế UI khai báo (Declarative UI) hiện đại.
* **Material Design 3**: Bộ quy chuẩn thiết kế giao diện Google tích hợp sẵn.
* **AndroidX DataStore / SharedPreferences**: Quản lý trạng thái đăng nhập (`SessionStore`) và lưu trữ dữ liệu offline (`LocalAppStore`).
* **OkHttp**: Hỗ trợ kết nối và gửi yêu cầu mạng.

---

### 💻 HƯỚNG DẪN CÀI ĐẶT & CHẠY ỨNG DỤNG

#### 1. Yêu cầu cấu hình hệ thống
* **Hệ điều hành**: Windows 10/11, macOS, hoặc Linux.
* **IDE**: Android Studio (Koala, Ladybug hoặc phiên bản mới hơn).
* **Java SDK**: JDK 11 hoặc mới hơn (Android Studio đã tích hợp sẵn).
* **Android SDK**: Compile SDK 36 (Android 16), Min SDK 24 (Android 7.0).
* **Thiết bị chạy thử**: Thiết bị ảo (Emulator) hoặc điện thoại thật hỗ trợ API Level 24 trở lên.

#### 2. Các bước mở và chạy dự án trong Android Studio

1. **Tải mã nguồn về máy**:
   * Giải nén file nén nguồn hoặc chạy lệnh:
     ```bash
     git clone <URL_REPO_PROJECT>
     ```
2. **Mở dự án trên Android Studio**:
   * Mở Android Studio, click chọn **Open**.
   * Dẫn đường dẫn đến thư mục chứa mã nguồn dự án (thư mục chứa tệp `settings.gradle.kts`).
3. **Đồng bộ hóa Gradle**:
   * Hãy đợi khoảng 1-3 phút để Android Studio tải các dependencies và hoàn tất **Gradle Sync**.
4. **Chạy ứng dụng (Run)**:
   * Kết nối thiết bị Android thật (đã bật chế độ gỡ lỗi USB) hoặc khởi chạy thiết bị ảo (Emulator).
   * Bấm biểu tượng nút **Run (▶️)** trên thanh công cụ phía trên hoặc bấm tổ hợp phím `Shift + F10` (trên Windows/Linux).
   * Chờ quá trình build hoàn tất, file APK sẽ được cài đặt và ứng dụng tự khởi động trên thiết bị.

---
*Đồ án được thực hiện nhằm mục đích học tập môn Nhập môn ứng dụng di động tại UIT - Khoa Kỹ thuật phần mềm.*
