# Clothing Store

Ứng dụng thương mại điện tử bán quần áo gồm frontend React/Vite và backend Spring Boot. Dự án phục vụ khách hàng mua sắm trực tuyến và quản trị viên quản lý sản phẩm, đơn hàng, hóa đơn, voucher, báo cáo.

## Mục lục

- [Giới thiệu](#giới-thiệu)
- [Tính năng](#tính-năng)
- [Yêu cầu hệ thống](#yêu-cầu-hệ-thống)
- [Cài đặt](#cài-đặt)
- [Hướng dẫn sử dụng](#hướng-dẫn-sử-dụng)
- [Cấu hình](#cấu-hình)

## Giới thiệu

Clothing Store là monorepo gồm:

- [clothing-store-be](clothing-store-be): REST API Spring Boot, JWT authentication, MySQL, quản lý sản phẩm, giỏ hàng, đơn hàng, thanh toán, đánh giá và báo cáo.
- [clothing-store-fe](clothing-store-fe): giao diện React/Vite cho khách hàng và trang quản trị.

Backend mặc định chạy tại `http://localhost:8080/api/v1`; frontend mặc định chạy tại `http://localhost:5173` và proxy các request `/api` sang backend.

## Tính năng

- Đăng ký, đăng nhập, refresh token, xác thực email, quên và đặt lại mật khẩu.
- Duyệt, lọc, tìm kiếm sản phẩm; xem chi tiết sản phẩm, màu sắc, kích thước và đánh giá.
- Giỏ hàng, voucher, đặt hàng, theo dõi đơn hàng, hủy đơn và yêu cầu trả hàng.
- Thanh toán COD và VNPay sandbox.
- Trang quản trị sản phẩm, danh mục, biến thể, khách hàng, đơn hàng, hóa đơn, voucher và thống kê doanh thu.
- Upload ảnh sản phẩm, ảnh đánh giá, ảnh hoàn tiền; xuất hóa đơn PDF và báo cáo Excel.

## Yêu cầu hệ thống

- Git
- Java 21
- MySQL 8+
- Node.js 20+ và npm 10+
- Tài khoản SMTP nếu cần gửi email xác thực, quên mật khẩu và thông báo đơn hàng

## Cài đặt

Clone repository:

```bash
git clone https://github.com/ndvuongq04/Clothing-store.git
cd Clothing-store
```

Tạo database MySQL:

```sql
CREATE DATABASE clothing_store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Cấu hình backend:

```powershell
cd clothing-store-be
Copy-Item .env.example .env
```

Mở file `.env` vừa tạo, tham khảo [clothing-store-be/.env.example](clothing-store-be/.env.example), và cập nhật tối thiểu các biến sau:

```env
DB_URL=jdbc:mysql://localhost:3306/clothing_store?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_base64_hs512_secret
MAIL_USERNAME=your_email@example.com
MAIL_PASSWORD=your_app_password
```

Chạy backend:

```powershell
.\gradlew.bat bootRun
```

Nếu dùng macOS/Linux:

```bash
./gradlew bootRun
```

Cài đặt và chạy frontend trong terminal khác:

```powershell
cd clothing-store-fe
npm install
npm run dev
```

Mở ứng dụng tại `http://localhost:5173`.

## Hướng dẫn sử dụng

Tài khoản quản trị mặc định được tạo khi backend khởi động lần đầu:

```text
Email: superadmin@clothingstore.vn
Password: S123456
```

Luôn đổi mật khẩu tài khoản này trước khi đưa dự án lên môi trường thật.

Một số URL hữu ích:

- Frontend: `http://localhost:5173`
- Admin: `http://localhost:5173/admin`
- API base URL: `http://localhost:8080/api/v1`

Ví dụ gọi API đăng nhập:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"superadmin@clothingstore.vn\",\"password\":\"S123456\"}"
```

Ví dụ lấy danh sách sản phẩm public:

```bash
curl "http://localhost:8080/api/v1/products?page=0&pageSize=12"
```

Build frontend:

```powershell
cd clothing-store-fe
npm run build
```

Build backend:

```powershell
cd clothing-store-be
.\gradlew.bat build
```

## Cấu hình

Backend đọc biến môi trường từ file `.env` trong [clothing-store-be](clothing-store-be) khi chạy profile `dev`. Các biến quan trọng:

| Biến | Bắt buộc | Mô tả |
| --- | --- | --- |
| `SPRING_PROFILE` | Không | Profile chạy ứng dụng, mặc định `dev`. |
| `DB_URL` | Có | JDBC URL tới MySQL. |
| `DB_USERNAME` | Có | Tài khoản MySQL. |
| `DB_PASSWORD` | Có | Mật khẩu MySQL. |
| `JWT_SECRET` | Có | Secret Base64 dùng cho HS512 JWT. |
| `JWT_ACCESS_TOKEN_EXPIRATION` | Không | Thời gian sống access token, đơn vị millisecond. |
| `JWT_REFRESH_TOKEN_EXPIRATION` | Không | Thời gian sống refresh token, đơn vị millisecond. |
| `MAIL_HOST` | Không | SMTP host, mặc định `smtp.gmail.com`. |
| `MAIL_PORT` | Không | SMTP port, mặc định `587`. |
| `MAIL_USERNAME` | Có nếu gửi email | Tài khoản SMTP. |
| `MAIL_PASSWORD` | Có nếu gửi email | Mật khẩu/app password SMTP. |
| `FRONTEND_URL` | Không | Origin frontend dùng cho CORS, mặc định `http://localhost:5173`. |
| `VERIFY_URL` | Không | URL xác thực email frontend. |
| `RESET_PASSWORD_URL` | Không | URL đặt lại mật khẩu frontend. |
| `VNPAY_TMN_CODE` | Không | Mã merchant VNPay sandbox/production. |
| `VNPAY_HASH_SECRET` | Không | Secret ký request VNPay. |
| `VNPAY_RETURN_URL` | Không | Callback URL VNPay sau thanh toán. |

Frontend đang dùng proxy trong [clothing-store-fe/vite.config.js](clothing-store-fe/vite.config.js), vì vậy các request `/api` sẽ được chuyển đến `http://localhost:8080`.
