package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.order.*;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface OrderService {

    OrderDetailDto placeOrder(Long userId, PlaceOrderRequest req, HttpServletRequest httpReq);

    Object choosePayment(Long userId, Long orderId, ChoosePaymentRequest req, HttpServletRequest httpReq);

    String handleVnpayIpn(Map<String, String> params);

    OrderDetailDto handleVnpayReturn(Map<String, String> params);

    OrderDetailDto getOrderDetail(Long userId, Long orderId);

    ReturnRequestInfoDto getMyReturnRequestInfo(Long userId, Long orderId);

    OrderDetailDto getOrderDetailAdmin(Long orderId);

    ReturnRequestInfoDto getReturnRequestInfo(Long orderId);

    ResultPaginationDTO getMyOrders(Long userId, OrderFilterRequest req);

    ResultPaginationDTO getAllOrders(OrderFilterRequest req);

    OrderDetailDto cancelByUser(Long userId, Long orderId, CancelOrderRequest req);

    OrderDetailDto requestReturn(Long userId, Long orderId, ReturnRequestDto req, List<MultipartFile> images);

    OrderDetailDto updateOrderStatus(Long orderId, UpdateOrderStatusRequest req);

    ReturnRequestInfoDto confirmReturnRequest(Long orderId, String adminNote);

    RefundResponseDto refundReturnedOrder(Long orderId, String adminNote, MultipartFile billImage,
            HttpServletRequest httpReq);

    ReturnRequestInfoDto rejectReturnRequest(Long orderId, RejectReturnRequestDto req);

    RefundResponseDto refundByError(AdminRefundRequestDto req, String adminUsername, HttpServletRequest httpReq);

    List<PaymentSettingDto> getPaymentSettings();

    PaymentSettingDto togglePaymentMethod(String method);

    List<ReconcileResultDto> reconcile(LocalDate date);
}
