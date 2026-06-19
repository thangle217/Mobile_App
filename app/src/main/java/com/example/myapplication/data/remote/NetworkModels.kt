package com.example.myapplication.data.remote

data class LoginRequest(
    val tenDangNhap: String,
    val matKhau: String,
    val vaiTro: String
)

data class DangKyRequest(
    val tenDangNhap: String,
    val matKhau: String,
    val xacNhanMatKhau: String,
    val email: String,
    val hoTen: String,
    val soDienThoai: String,
    val cccd: String? = null,
    val anhCccdMatTruoc: String? = null,
    val anhCccdMatSau: String? = null,
    val vaiTro: String
)

data class NhaTroDto(
    val maNhaTro: String? = null,
    val tenNhaTro: String,
    val diaChi: String? = null,
    val soPhong: String? = null,
    val trangThai: String? = null
)

data class PhongDto(
    val maPhong: String? = null,
    val maNhaTro: String,
    val tenPhong: String,
    val giaPhong: Double,
    val trangThai: String? = null,
    val ghiChu: String? = null
)

data class HoaDonCreateDto(
    val maPhong: String,
    val kyHoaDon: String,
    val chiPhiPhatSinh: Double? = 0.0,
    val lyDoPhatSinh: String? = null
)

data class BaoCaoSuCoDto(
    val tieuDe: String,
    val moTa: String,
    val mucDoKhancap: String,
    val trangThai: String? = "Mới"
)

data class PhanHoiSuCoDto(
    val phanHoi: String,
    val trangThaiMoi: String
)

data class ThanhToanSubmitDto(
    val maHoaDon: String,
    val maGiaoDich: String,
    val anhBienLai: String,
    val ghiChu: String? = null
)

data class ThongBaoCreateDto(
    val tieuDe: String,
    val noiDung: String,
    val loaiNguoiNhan: String,
    val maPhong: String? = null
)
