package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.statistic.DashboardResponse;
import com.clothingstore.clothing_store_be.dto.statistic.OrderStatResponse;
import com.clothingstore.clothing_store_be.dto.statistic.RevenueStatResponse;

import com.clothingstore.clothing_store_be.dto.statistic.ProductStatResponse;

import java.time.LocalDate;

public interface StatisticService {
    DashboardResponse getDashboardOverview();

    RevenueStatResponse getRevenueStatistics(String period, LocalDate fromDate, LocalDate toDate);

    OrderStatResponse getOrderStatistics(String period, LocalDate fromDate, LocalDate toDate);

    ProductStatResponse getProductStatistics(LocalDate fromDate, LocalDate toDate);

    byte[] exportStatisticsToExcel(LocalDate fromDate, LocalDate toDate);
}
