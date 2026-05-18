package com.clothingstore.clothing_store_be.controller;

import com.clothingstore.clothing_store_be.dto.statistic.DashboardResponse;
import com.clothingstore.clothing_store_be.dto.statistic.OrderStatResponse;
import com.clothingstore.clothing_store_be.dto.statistic.ProductStatResponse;
import com.clothingstore.clothing_store_be.dto.statistic.RevenueStatResponse;
import com.clothingstore.clothing_store_be.service.StatisticService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class StatisticAdminController {

    private final StatisticService statisticService;

    /**
     * GET /admin/dashboard — API xem tổng quan Dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> getDashboardOverview() {
        return ResponseEntity.ok(statisticService.getDashboardOverview());
    }

    /**
     * GET /admin/statistics/revenue — API thống kê doanh thu / lợi nhuận
     */
    @GetMapping("/statistics/revenue")
    public ResponseEntity<RevenueStatResponse> getRevenueStatistics(
            @RequestParam(value = "period", defaultValue = "day") String period,
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        return ResponseEntity.ok(statisticService.getRevenueStatistics(period, fromDate, toDate));
    }

    /**
     * GET /admin/statistics/orders — API thống kê đơn hàng theo trạng thái
     */
    @GetMapping("/statistics/orders")
    public ResponseEntity<OrderStatResponse> getOrderStatistics(
            @RequestParam(value = "period", defaultValue = "day") String period,
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        return ResponseEntity.ok(statisticService.getOrderStatistics(period, fromDate, toDate));
    }

    /**
     * GET /admin/statistics/products — API thống kê sản phẩm
     */
    @GetMapping("/statistics/products")
    public ResponseEntity<ProductStatResponse> getProductStatistics(
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        return ResponseEntity.ok(statisticService.getProductStatistics(fromDate, toDate));
    }

    /**
     * GET /admin/statistics/export — API xuất báo cáo ra file Excel
     */
    @GetMapping("/statistics/export")
    public ResponseEntity<byte[]> exportStatistics(
            @RequestParam(value = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        byte[] excelBytes = statisticService.exportStatisticsToExcel(fromDate, toDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Bao_Cao_Thong_Ke.xlsx\"")
                .contentType(
                        MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);
    }
}
