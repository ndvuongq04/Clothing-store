package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.statistic.DashboardResponse;
import com.clothingstore.clothing_store_be.dto.statistic.OrderChartData;
import com.clothingstore.clothing_store_be.dto.statistic.OrderStatResponse;
import com.clothingstore.clothing_store_be.dto.statistic.ProductSaleData;
import com.clothingstore.clothing_store_be.dto.statistic.ProductStatResponse;
import com.clothingstore.clothing_store_be.dto.statistic.ProductStockData;
import com.clothingstore.clothing_store_be.dto.statistic.RevenueStatResponse;
import com.clothingstore.clothing_store_be.entity.Invoice;
import com.clothingstore.clothing_store_be.entity.Order;
import com.clothingstore.clothing_store_be.enums.InvoiceStatus;
import com.clothingstore.clothing_store_be.repository.InvoiceRepository;
import com.clothingstore.clothing_store_be.repository.OrderItemRepository;
import com.clothingstore.clothing_store_be.repository.OrderRepository;
import com.clothingstore.clothing_store_be.repository.ProductRepository;
import com.clothingstore.clothing_store_be.repository.ProductVariantRepository;
import com.clothingstore.clothing_store_be.repository.UserRepository;
import com.clothingstore.clothing_store_be.service.StatisticService;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayOutputStream;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatisticServiceImpl implements StatisticService {

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public DashboardResponse getDashboardOverview() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonthDate = today.withDayOfMonth(1);

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        LocalDateTime startOfMonth = startOfMonthDate.atStartOfDay();
        LocalDateTime endOfMonth = today.withDayOfMonth(today.lengthOfMonth()).atTime(LocalTime.MAX);

        // 1. Doanh thu hôm nay
        BigDecimal revenueToday = invoiceRepository.sumTotalAmountByStatusAndIssuedDate(InvoiceStatus.PAID, today);

        // 2. Doanh thu tháng này
        BigDecimal revenueThisMonth = invoiceRepository.sumTotalAmountByStatusAndIssuedDateBetween(InvoiceStatus.PAID,
                startOfMonthDate, today.withDayOfMonth(today.lengthOfMonth()));

        // 3. Số đơn hàng mới hôm nay
        long newOrdersToday = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        // 4. Số khách hàng mới tháng này
        long newCustomersThisMonth = userRepository.countByRoleAndCreatedAtBetween("user", startOfMonth, endOfMonth);

        // 5. Sản phẩm sắp hết hàng (< 5)
        long lowStockProducts = productVariantRepository.countByStockQtyLessThan(5);

        return DashboardResponse.builder()
                .revenueToday(revenueToday)
                .revenueThisMonth(revenueThisMonth)
                .newOrdersToday(newOrdersToday)
                .newCustomersThisMonth(newCustomersThisMonth)
                .lowStockProducts(lowStockProducts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueStatResponse getRevenueStatistics(String period, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null)
            fromDate = LocalDate.now().minusMonths(1);
        if (toDate == null)
            toDate = LocalDate.now();

        List<Invoice> invoices = invoiceRepository.findByStatusAndIssuedDateBetween(InvoiceStatus.PAID, fromDate,
                toDate);

        Map<String, BigDecimal> revenueMap = new LinkedHashMap<>();
        Map<String, BigDecimal> profitMap = new LinkedHashMap<>();

        // Khởi tạo các nhãn (labels)
        LocalDate current = fromDate;
        DateTimeFormatter formatter;
        switch (period != null ? period.toLowerCase() : "day") {
            case "month":
                formatter = DateTimeFormatter.ofPattern("MM/yyyy");
                while (!current.isAfter(toDate)) {
                    revenueMap.put(current.format(formatter), BigDecimal.ZERO);
                    profitMap.put(current.format(formatter), BigDecimal.ZERO);
                    current = current.plusMonths(1).withDayOfMonth(1);
                }
                break;
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                while (!current.isAfter(toDate)) {
                    revenueMap.put(current.format(formatter), BigDecimal.ZERO);
                    profitMap.put(current.format(formatter), BigDecimal.ZERO);
                    current = current.plusYears(1).withDayOfYear(1);
                }
                break;
            case "week":
            case "day":
            default:
                formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                while (!current.isAfter(toDate)) {
                    revenueMap.put(current.format(formatter), BigDecimal.ZERO);
                    profitMap.put(current.format(formatter), BigDecimal.ZERO);
                    current = current.plusDays(1);
                }
                break;
        }

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (Invoice inv : invoices) {
            String label;
            switch (period != null ? period.toLowerCase() : "day") {
                case "month":
                    label = inv.getIssuedDate().format(DateTimeFormatter.ofPattern("MM/yyyy"));
                    break;
                case "year":
                    label = inv.getIssuedDate().format(DateTimeFormatter.ofPattern("yyyy"));
                    break;
                case "week":
                case "day":
                default:
                    label = inv.getIssuedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    break;
            }

            BigDecimal revenue = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;

            // Tính giá vốn (cost)
            BigDecimal cost = BigDecimal.ZERO;
            if (inv.getOrder() != null && inv.getOrder().getItems() != null) {
                for (var item : inv.getOrder().getItems()) {
                    BigDecimal itemImportPrice = item.getImportPrice() != null ? item.getImportPrice()
                            : BigDecimal.ZERO;
                    cost = cost.add(itemImportPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
                }
            }

            BigDecimal profit = revenue.subtract(cost);

            if (revenueMap.containsKey(label)) {
                revenueMap.put(label, revenueMap.get(label).add(revenue));
                profitMap.put(label, profitMap.get(label).add(profit));
            }

            totalRevenue = totalRevenue.add(revenue);
            totalProfit = totalProfit.add(profit);
        }

        List<String> labels = new ArrayList<>(revenueMap.keySet());
        List<BigDecimal> revenues = new ArrayList<>(revenueMap.values());
        List<BigDecimal> profits = new ArrayList<>(profitMap.values());

        return RevenueStatResponse.builder()
                .labels(labels)
                .revenue(revenues)
                .profit(profits)
                .totalRevenue(totalRevenue)
                .totalProfit(totalProfit)
                .growthRate(0.0) // Todo: calculate actual growth rate
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderStatResponse getOrderStatistics(String period, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null)
            fromDate = LocalDate.now().minusMonths(1);
        if (toDate == null)
            toDate = LocalDate.now();

        List<Order> orders = orderRepository.findByCreatedAtBetween(fromDate.atStartOfDay(),
                toDate.atTime(LocalTime.MAX));

        long total = 0;
        long completed = 0;
        long cancelled = 0;
        long delivering = 0;

        Map<String, OrderChartData> chartMap = new LinkedHashMap<>();

        // Khởi tạo labels
        LocalDate current = fromDate;
        DateTimeFormatter formatter;
        switch (period != null ? period.toLowerCase() : "day") {
            case "month":
                formatter = DateTimeFormatter.ofPattern("MM/yyyy");
                while (!current.isAfter(toDate)) {
                    chartMap.put(current.format(formatter), new OrderChartData(current.format(formatter), 0, 0));
                    current = current.plusMonths(1).withDayOfMonth(1);
                }
                break;
            case "year":
                formatter = DateTimeFormatter.ofPattern("yyyy");
                while (!current.isAfter(toDate)) {
                    chartMap.put(current.format(formatter), new OrderChartData(current.format(formatter), 0, 0));
                    current = current.plusYears(1).withDayOfYear(1);
                }
                break;
            case "week":
            case "day":
            default:
                formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                while (!current.isAfter(toDate)) {
                    chartMap.put(current.format(formatter), new OrderChartData(current.format(formatter), 0, 0));
                    current = current.plusDays(1);
                }
                break;
        }

        for (Order order : orders) {
            total++;
            String status = order.getStatus() != null ? order.getStatus().toLowerCase() : "";

            if (status.equals("completed"))
                completed++;
            else if (status.equals("cancelled"))
                cancelled++;
            else if (status.equals("shipping"))
                delivering++;

            String label;
            switch (period != null ? period.toLowerCase() : "day") {
                case "month":
                    label = order.getCreatedAt().format(DateTimeFormatter.ofPattern("MM/yyyy"));
                    break;
                case "year":
                    label = order.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy"));
                    break;
                case "week":
                case "day":
                default:
                    label = order.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    break;
            }

            if (chartMap.containsKey(label)) {
                OrderChartData data = chartMap.get(label);
                if (status.equals("completed")) {
                    data.setCompleted(data.getCompleted() + 1);
                } else if (status.equals("cancelled")) {
                    data.setCancelled(data.getCancelled() + 1);
                }
            }
        }

        double completionRate = total > 0 ? (double) completed / total * 100 : 0.0;
        completionRate = Math.round(completionRate * 10.0) / 10.0; // round to 1 decimal

        return OrderStatResponse.builder()
                .total(total)
                .completed(completed)
                .cancelled(cancelled)
                .delivering(delivering)
                .completionRate(completionRate)
                .chartData(new ArrayList<>(chartMap.values()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductStatResponse getProductStatistics(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null)
            fromDate = LocalDate.now().minusMonths(1);
        if (toDate == null)
            toDate = LocalDate.now();

        PageRequest topPage = PageRequest.of(0, 10);
        Page<Object[]> topSellingPage = orderItemRepository.findTopSellingProducts(InvoiceStatus.PAID, fromDate, toDate,
                topPage);

        List<ProductSaleData> topSelling = topSellingPage.getContent().stream().map(obj -> {
            return ProductSaleData.builder()
                    .productId((Long) obj[0])
                    .name((String) obj[1])
                    .thumbnailUrl((String) obj[2])
                    .categoryName((String) obj[3])
                    .quantitySold(obj[4] != null ? ((Number) obj[4]).longValue() : 0)
                    .revenue(obj[5] != null ? (BigDecimal) obj[5] : BigDecimal.ZERO)
                    .build();
        }).collect(Collectors.toList());

        PageRequest slowPage = PageRequest.of(0, 10);
        Page<Object[]> slowMovingPage = productRepository
                .findSlowMovingProducts(slowPage);

        List<ProductStockData> slowMoving = slowMovingPage.getContent().stream().map(obj -> {
            return ProductStockData.builder()
                    .productId((Long) obj[0])
                    .name((String) obj[1])
                    .thumbnailUrl((String) obj[2])
                    .categoryName((String) obj[3])
                    .stockQty(obj[4] != null ? ((Number) obj[4]).longValue() : 0)
                    .build();
        }).collect(Collectors.toList());

        return ProductStatResponse.builder()
                .topSelling(topSelling)
                .slowMoving(slowMoving)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportStatisticsToExcel(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null) fromDate = LocalDate.now().minusMonths(1);
        if (toDate == null) toDate = LocalDate.now();

        // Lấy dữ liệu
        RevenueStatResponse revenueStat = getRevenueStatistics("day", fromDate, toDate);
        OrderStatResponse orderStat = getOrderStatistics("day", fromDate, toDate);
        ProductStatResponse productStat = getProductStatistics(fromDate, toDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            
            // Style chung
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);

            CellStyle rowStyle = workbook.createCellStyle();
            rowStyle.setBorderBottom(BorderStyle.THIN);
            rowStyle.setBorderTop(BorderStyle.THIN);
            rowStyle.setBorderRight(BorderStyle.THIN);
            rowStyle.setBorderLeft(BorderStyle.THIN);

            // ================= Sheet 1: Tổng quan & Doanh thu =================
            Sheet sheet1 = workbook.createSheet("Tong Quan & Doanh Thu");
            
            // Header: Tổng quan
            Row titleRow = sheet1.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("BÁO CÁO HOẠT ĐỘNG KINH DOANH TỪ " + fromDate + " ĐẾN " + toDate);
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);

            Row kpiHeaderRow = sheet1.createRow(2);
            String[] kpiHeaders = {"Tổng doanh thu", "Tổng lợi nhuận", "Tổng số đơn", "Số đơn thành công", "Số đơn hủy", "Tỷ lệ chốt đơn"};
            for (int i = 0; i < kpiHeaders.length; i++) {
                Cell cell = kpiHeaderRow.createCell(i);
                cell.setCellValue(kpiHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            Row kpiDataRow = sheet1.createRow(3);
            kpiDataRow.createCell(0).setCellValue(revenueStat.getTotalRevenue() != null ? revenueStat.getTotalRevenue().doubleValue() : 0);
            kpiDataRow.createCell(1).setCellValue(revenueStat.getTotalProfit() != null ? revenueStat.getTotalProfit().doubleValue() : 0);
            kpiDataRow.createCell(2).setCellValue(orderStat.getTotal());
            kpiDataRow.createCell(3).setCellValue(orderStat.getCompleted());
            kpiDataRow.createCell(4).setCellValue(orderStat.getCancelled());
            kpiDataRow.createCell(5).setCellValue(orderStat.getCompletionRate() + "%");
            for (int i = 0; i < 6; i++) kpiDataRow.getCell(i).setCellStyle(rowStyle);

            // Header: Chi tiết doanh thu
            Row revHeaderRow = sheet1.createRow(6);
            String[] revHeaders = {"Ngày", "Doanh thu", "Lợi nhuận"};
            for (int i = 0; i < revHeaders.length; i++) {
                Cell cell = revHeaderRow.createCell(i);
                cell.setCellValue(revHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 7;
            for (int i = 0; i < revenueStat.getLabels().size(); i++) {
                Row row = sheet1.createRow(rowIdx++);
                row.createCell(0).setCellValue(revenueStat.getLabels().get(i));
                row.createCell(1).setCellValue(revenueStat.getRevenue().get(i) != null ? revenueStat.getRevenue().get(i).doubleValue() : 0);
                row.createCell(2).setCellValue(revenueStat.getProfit().get(i) != null ? revenueStat.getProfit().get(i).doubleValue() : 0);
                for (int j = 0; j < 3; j++) row.getCell(j).setCellStyle(rowStyle);
            }

            for (int i = 0; i < 6; i++) sheet1.autoSizeColumn(i);

            // ================= Sheet 2: Sản phẩm bán chạy =================
            Sheet sheet2 = workbook.createSheet("San Pham Ban Chay");
            Row topHeaderRow = sheet2.createRow(0);
            String[] topHeaders = {"ID", "Tên Sản Phẩm", "Danh Mục", "Đã Bán", "Doanh Thu"};
            for (int i = 0; i < topHeaders.length; i++) {
                Cell cell = topHeaderRow.createCell(i);
                cell.setCellValue(topHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            rowIdx = 1;
            for (ProductSaleData p : productStat.getTopSelling()) {
                Row row = sheet2.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getProductId());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getCategoryName());
                row.createCell(3).setCellValue(p.getQuantitySold());
                row.createCell(4).setCellValue(p.getRevenue() != null ? p.getRevenue().doubleValue() : 0);
                for (int j = 0; j < 5; j++) row.getCell(j).setCellStyle(rowStyle);
            }

            for (int i = 0; i < 5; i++) sheet2.autoSizeColumn(i);

            // ================= Sheet 3: Sản phẩm tồn kho lâu =================
            Sheet sheet3 = workbook.createSheet("San Pham Ton Kho");
            Row slowHeaderRow = sheet3.createRow(0);
            String[] slowHeaders = {"ID", "Tên Sản Phẩm", "Danh Mục", "Số Lượng Tồn"};
            for (int i = 0; i < slowHeaders.length; i++) {
                Cell cell = slowHeaderRow.createCell(i);
                cell.setCellValue(slowHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            rowIdx = 1;
            for (ProductStockData p : productStat.getSlowMoving()) {
                Row row = sheet3.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getProductId());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getCategoryName());
                row.createCell(3).setCellValue(p.getStockQty());
                for (int j = 0; j < 4; j++) row.getCell(j).setCellStyle(rowStyle);
            }

            for (int i = 0; i < 4; i++) sheet3.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error exporting statistics to Excel", e);
        }
    }
}
