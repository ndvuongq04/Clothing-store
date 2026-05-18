package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.order.*;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.entity.*;
import com.clothingstore.clothing_store_be.enums.InvoiceStatus;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.*;
import com.clothingstore.clothing_store_be.service.EmailService;
import com.clothingstore.clothing_store_be.service.FileStorageService;
import com.clothingstore.clothing_store_be.service.OrderService;
import com.clothingstore.clothing_store_be.util.VnpayUtil;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private static final String RETURN_REQUEST_IMAGE_SUBFOLDER = "return-requests";
    private static final String REFUND_TRANSFER_PROOF_SUBFOLDER = "refund-transfer-proofs";
    private static final int MAX_RETURN_REQUEST_IMAGES = 5;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentSettingRepository paymentSettingRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final ProductVariantRepository variantRepository;
    private final VoucherRepository voucherRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final FileStorageService fileStorageService;
    private final VnpayUtil vnpayUtil;

    // ─── UC-20: Place order ───────────────────────────────

    @Override
    public OrderDetailDto placeOrder(Long userId, PlaceOrderRequest req, HttpServletRequest httpReq) {
        Cart cart = cartRepository.findByUserUserId(userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy giỏ hàng"));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw AppException.badRequest("Giỏ hàng đang trống");
        }

        // Lọc các item được chọn
        Set<Long> selectedIds = new HashSet<>(req.getCartItemIds());
        List<CartItem> selectedItems = cart.getItems().stream()
                .filter(ci -> selectedIds.contains(ci.getId()))
                .toList();

        if (selectedItems.isEmpty()) {
            throw AppException.badRequest("Không tìm thấy sản phẩm đã chọn trong giỏ hàng");
        }

        Address address = addressRepository
                .findByIdAndUserUserIdAndDeletedAtIsNull(req.getAddressId(), userId)
                .orElseThrow(() -> AppException.notFound("Địa chỉ không tồn tại hoặc không thuộc về bạn"));

        // Kiểm tra tồn kho cho các item được chọn
        for (CartItem ci : selectedItems) {
            ProductVariant variant = variantRepository.findById(ci.getVariant().getId())
                    .orElseThrow(() -> AppException.notFound("Sản phẩm không còn tồn tại"));
            if (variant.getStockQty() < ci.getQuantity()) {
                throw AppException.badRequest("Sản phẩm '" + variant.getProduct().getName()
                        + "' chỉ còn " + variant.getStockQty() + " trong kho");
            }
        }

        // Tính subTotal từ các item được chọn
        BigDecimal subTotal = selectedItems.stream()
                .map(ci -> ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Validate và apply voucher từ request
        Voucher voucher = null;
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (req.getVoucherCode() != null && !req.getVoucherCode().isBlank()) {
            voucher = voucherRepository
                    .findByVoucherCodeAndActiveTrueAndDeletedAtIsNull(req.getVoucherCode())
                    .orElseThrow(() -> AppException.badRequest("Mã voucher không hợp lệ hoặc không hoạt động"));

            LocalDate today = LocalDate.now();
            if (today.isBefore(voucher.getStartDate())) {
                throw AppException.badRequest("Voucher chưa đến ngày hiệu lực");
            }
            if (today.isAfter(voucher.getExpiryDate())) {
                throw AppException.badRequest("Mã voucher đã hết hạn");
            }
            if (voucher.getUsedCount() >= voucher.getMaxUsage()) {
                throw AppException.badRequest("Mã voucher đã hết lượt sử dụng");
            }
            if (subTotal.compareTo(voucher.getMinOrderValue()) < 0) {
                throw AppException.badRequest(
                        "Đơn hàng tối thiểu " + voucher.getMinOrderValue() + " để dùng voucher này");
            }

            if ("percent".equalsIgnoreCase(voucher.getType())) {
                discountAmount = subTotal.multiply(voucher.getDiscountValue())
                        .divide(BigDecimal.valueOf(100));
                if (voucher.getMaxDiscountCap() != null
                        && discountAmount.compareTo(voucher.getMaxDiscountCap()) > 0) {
                    discountAmount = voucher.getMaxDiscountCap();
                }
            } else {
                discountAmount = voucher.getDiscountValue();
            }
            if (discountAmount.compareTo(subTotal) > 0)
                discountAmount = subTotal;

            voucher.setUsedCount(voucher.getUsedCount() + 1);
            voucherRepository.save(voucher);
        }

        BigDecimal total = subTotal.subtract(discountAmount);

        // Build order với snapshot địa chỉ
        Order order = Order.builder()
                .user(cart.getUser())
                .address(address)
                .shipFullName(address.getFullName())
                .shipPhone(address.getPhone())
                .shipProvince(address.getProvince())
                .shipDistrict(address.getDistrict())
                .shipWard(address.getWard())
                .shipStreet(address.getStreet())
                .voucher(voucher)
                .subTotal(subTotal)
                .discountAmount(discountAmount)
                .total(total)
                .note(req.getNote())
                .items(new ArrayList<>())
                .build();

        // Build order items từ các item được chọn (snapshot)
        for (CartItem ci : selectedItems) {
            ProductVariant v = ci.getVariant();
            Product p = v.getProduct();
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .variant(v)
                    .productName(p.getName())
                    .color(v.getColor())
                    .size(v.getSize())
                    .thumbnailUrl(p.getThumbnailUrl())
                    .quantity(ci.getQuantity())
                    .unitPrice(ci.getUnitPrice())
                    .lineTotal(ci.getUnitPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .importPrice(v.getImportPrice() != null ? v.getImportPrice() : BigDecimal.ZERO)
                    .build();
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);

        // Trừ tồn kho cho các item được đặt
        for (CartItem ci : selectedItems) {
            ProductVariant variant = variantRepository.findById(ci.getVariant().getId()).get();
            variant.setStockQty(variant.getStockQty() - ci.getQuantity());
            variantRepository.save(variant);
        }

        // Xoá chỉ các item đã đặt khỏi giỏ hàng, cập nhật lại tổng tiền giỏ
        cart.getItems().removeIf(ci -> selectedIds.contains(ci.getId()));
        recalculateCart(cart);
        cartRepository.save(cart);

        // Tạo Invoice với status PENDING
        createInvoice(saved);

        // Xử lý payment ngay khi đặt hàng
        String method = req.getPaymentMethod();
        PaymentSetting setting = paymentSettingRepository.findById(method)
                .orElseThrow(() -> AppException.badRequest("Phương thức thanh toán không hợp lệ"));
        if (!setting.isEnabled()) {
            throw AppException.badRequest("Phương thức '" + method + "' hiện không khả dụng");
        }

        saved.setPaymentMethod(method);

        Payment payment = Payment.builder()
                .order(saved)
                .method(method)
                .amount(saved.getTotal())
                .build();

        String paymentUrl = null;
        if ("vnpay".equals(method)) {
            payment.setVnpayTxnRef(saved.getOrderCode());
            paymentRepository.save(payment);
            saved.setPayment(payment);
            orderRepository.save(saved);
            String ip = vnpayUtil.getClientIp(httpReq);
            paymentUrl = vnpayUtil.buildPaymentUrl(saved.getOrderCode(), saved.getTotal(), ip);
        } else {
            paymentRepository.save(payment);
            saved.setPayment(payment);
            orderRepository.save(saved);
            emailService.sendOrderConfirmation(saved);
        }

        OrderDetailDto result = toOrderDetailDto(saved);
        result.setPaymentUrl(paymentUrl);
        return result;
    }

    // ─── UC-21: Choose payment ────────────────────────────

    @Override
    public Object choosePayment(Long userId, Long orderId, ChoosePaymentRequest req,
            HttpServletRequest httpReq) {
        Order order = orderRepository.findByIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        if (!"pending".equals(order.getStatus()) && !"payment_failed".equals(order.getStatus())) {
            throw AppException.badRequest("Đơn hàng không ở trạng thái có thể chọn thanh toán");
        }

        String method = req.getPaymentMethod();
        PaymentSetting setting = paymentSettingRepository.findById(method)
                .orElseThrow(() -> AppException.badRequest("Phương thức thanh toán không hợp lệ"));
        if (!setting.isEnabled()) {
            throw AppException.badRequest("Phương thức '" + method + "' hiện không khả dụng");
        }

        order.setPaymentMethod(method);

        // Xóa payment cũ nếu có (retry)
        if (order.getPayment() != null) {
            paymentRepository.delete(order.getPayment());
            order.setPayment(null);
        }

        Payment payment = Payment.builder()
                .order(order)
                .method(method)
                .amount(order.getTotal())
                .build();

        if ("vnpay".equals(method)) {
            payment.setVnpayTxnRef(order.getOrderCode());
        }

        paymentRepository.save(payment);
        order.setStatus("pending");
        orderRepository.save(order);

        if ("cod".equals(method)) {
            emailService.sendOrderConfirmation(order);
            return toOrderDetailDto(order);
        }

        String ip = vnpayUtil.getClientIp(httpReq);
        String paymentUrl = vnpayUtil.buildPaymentUrl(order.getOrderCode(), order.getTotal(), ip);
        return Map.of("paymentUrl", paymentUrl);
    }

    // ─── VNPay IPN ────────────────────────────────────────

    @Override
    public String handleVnpayIpn(Map<String, String> params) {
        if (!vnpayUtil.verifySignature(params))
            return "97";

        String txnRef = params.get("vnp_TxnRef");
        Payment payment = paymentRepository.findByVnpayTxnRef(txnRef).orElse(null);
        if (payment == null)
            return "01";

        if (!"pending".equals(payment.getStatus()))
            return "02";

        Order order = payment.getOrder();
        String responseCode = params.get("vnp_ResponseCode");

        if ("00".equals(responseCode)) {
            payment.setStatus("success");
            payment.setPaidAt(LocalDateTime.now());
            payment.setVnpayTransactionNo(params.get("vnp_TransactionNo"));
            payment.setVnpayResponseCode(responseCode);
            payment.setVnpayPayDate(params.get("vnp_PayDate"));
            order.setPaymentStatus("paid");
            order.setStatus("confirmed");
            // VNPay thanh toán thành công → Invoice PAID
            updateInvoiceStatus(order, InvoiceStatus.PAID);
            emailService.sendOrderConfirmation(order);
        } else {
            payment.setStatus("failed");
            order.setStatus("payment_failed");
            order.setPaymentStatus("unpaid");
            restoreStock(order);
        }

        paymentRepository.save(payment);
        orderRepository.save(order);
        return "00";
    }

    // ─── VNPay return ─────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public OrderDetailDto handleVnpayReturn(Map<String, String> params) {
        if (!vnpayUtil.verifySignature(params)) {
            throw AppException.badRequest("Chữ ký VNPay không hợp lệ");
        }
        String orderCode = params.get("vnp_TxnRef");
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));
        return toOrderDetailDto(order);
    }

    // ─── UC-22: Order detail ──────────────────────────────

    // ─── Return request (user) ────────────────────────────

    @Override
    public OrderDetailDto requestReturn(Long userId, Long orderId, ReturnRequestDto req, List<MultipartFile> images) {
        Order order = orderRepository.findByIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        if (!"completed".equals(order.getStatus())) {
            throw AppException.badRequest("Chỉ có thể yêu cầu hoàn hàng khi đơn hàng đã giao thành công");
        }
        if (!"paid".equals(order.getPaymentStatus())) {
            throw AppException.badRequest("Đơn hàng chưa thanh toán, không thể yêu cầu hoàn tiền");
        }
        if (!supportsRefund(order.getPaymentMethod())) {
            throw AppException.badRequest("Phương thức thanh toán không hỗ trợ hoàn tiền");
        }

        // Kiểm tra trong vòng 7 ngày
        LocalDateTime limitDate = order.getCreatedAt().plusDays(7);
        if (LocalDateTime.now().isAfter(limitDate)) {
            throw AppException.badRequest("Đã quá 7 ngày kể từ khi đặt hàng, không thể yêu cầu hoàn hàng");
        }

        if (req.getReason() == null || req.getReason().isBlank()) {
            throw AppException.badRequest("Lý do hoàn hàng không được để trống");
        }
        if (requiresRefundBankInfo(order) && isBlank(req.getRefundBankInfo())) {
            throw AppException.badRequest("Đơn COD yêu cầu thông tin ngân hàng nhận hoàn tiền");
        }
        if (images == null || images.isEmpty()) {
            throw AppException.badRequest("Vui lòng gửi ít nhất 1 ảnh khi yêu cầu hoàn hàng");
        }
        if (images.size() > MAX_RETURN_REQUEST_IMAGES) {
            throw AppException.badRequest("Tối đa " + MAX_RETURN_REQUEST_IMAGES + " ảnh cho mỗi yêu cầu hoàn hàng");
        }

        Payment payment = order.getPayment();
        payment.setStatus("refund_requested");
        payment.setRefundReason(req.getReason());
        payment.setRefundBankInfo(normalizeRefundBankInfo(req.getRefundBankInfo()));
        payment.setRefundRequestDate(LocalDateTime.now());
        payment.getRefundImages().clear();

        List<String> uploadedUrls = fileStorageService.uploadFiles(RETURN_REQUEST_IMAGE_SUBFOLDER, images);
        for (String url : uploadedUrls) {
            payment.getRefundImages().add(PaymentRefundImage.builder()
                    .payment(payment)
                    .imageUrl(url)
                    .build());
        }
        paymentRepository.save(payment);

        order.setPaymentStatus("refund_requested");
        order.setStatus("return_requested");
        orderRepository.save(order);

        return toOrderDetailDto(order);
    }

    // ─── Confirm return & refund (admin) ────────────────────

    @Override
    public ReturnRequestInfoDto confirmReturnRequest(Long orderId, String adminNote) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        if (!"return_requested".equals(order.getStatus())) {
            throw AppException.badRequest("Đơn hàng chưa ở trạng thái yêu cầu hoàn hàng");
        }
        if (!"refund_requested".equals(order.getPaymentStatus())) {
            throw AppException.badRequest("Đơn hàng chưa có yêu cầu hoàn hàng");
        }

        Payment payment = order.getPayment();
        if (payment == null || !supportsRefund(payment.getMethod())) {
            throw AppException.badRequest("Phương thức thanh toán không hỗ trợ hoàn tiền");
        }

        paymentRepository.save(payment);

        order.setStatus("return_approved");
        orderRepository.save(order);

        return buildReturnRequestInfo(order);
    }

    // ─── Refund by error (admin) ────────────────────────────

    @Override
    public RefundResponseDto refundReturnedOrder(Long orderId, String adminNote, MultipartFile billImage,
            HttpServletRequest httpReq) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        if (!"return_approved".equals(order.getStatus())) {
            throw AppException.badRequest("Đơn hàng chưa được chấp nhận hoàn hàng");
        }
        if (!"refund_requested".equals(order.getPaymentStatus())) {
            throw AppException.badRequest("Đơn hàng không ở trạng thái chờ hoàn tiền");
        }

        Payment payment = order.getPayment();
        if (payment == null || !supportsRefund(payment.getMethod())) {
            throw AppException.badRequest("Phương thức thanh toán không hỗ trợ hoàn tiền");
        }
        BigDecimal amount = order.getTotal();

        String adminEmail = getCurrentUserEmail();

        payment.setStatus("refunded");
        payment.setRefundAmount(amount);
        payment.setRefundApprovedAt(LocalDateTime.now());
        payment.setRefundApprovedBy(adminEmail);

        if ("cod".equals(payment.getMethod())) {
            if (billImage == null || billImage.isEmpty()) {
                throw AppException.badRequest("Đơn COD yêu cầu ảnh bill chuyển khoản khi hoàn tiền");
            }

            String proofUrl = fileStorageService.uploadFiles(REFUND_TRANSFER_PROOF_SUBFOLDER, List.of(billImage))
                    .stream()
                    .findFirst()
                    .orElseThrow(() -> AppException.badRequest("Không thể tải lên ảnh bill chuyển khoản"));

            payment.setRefundTransferProofUrl(proofUrl);
            payment.setRefundVnpayResponseCode(null);
            payment.setRefundVnpayTransactionNo(null);
        } else if ("vnpay".equals(payment.getMethod())) {
            String txnRef = payment.getVnpayTxnRef();
            String transactionNo = payment.getVnpayTransactionNo();
            String payDate = payment.getVnpayPayDate();

            if (transactionNo == null || transactionNo.isBlank()) {
                throw AppException.badRequest("Thiếu mã giao dịch VNPay, không thể hoàn tiền");
            }

            String ip = vnpayUtil.getClientIp(httpReq);
            String orderInfo = "Hoan tien don hang " + order.getOrderCode();

            @SuppressWarnings("unchecked")
            Map<String, Object> refundRes = vnpayUtil.refund(
                    txnRef, transactionNo, amount, payDate,
                    adminEmail, ip, orderInfo, "02");

            String responseCode = String.valueOf(refundRes.getOrDefault("vnp_ResponseCode", ""));
            if (!"00".equals(responseCode)) {
                String message = String.valueOf(refundRes.getOrDefault("vnp_Message", "Unknown error"));
                throw AppException.badRequest("VNPay hoàn tiền thất bại: " + responseCode + " - " + message);
            }

            payment.setRefundVnpayResponseCode(responseCode);
            payment.setRefundVnpayTransactionNo(String.valueOf(refundRes.getOrDefault("vnp_TransactionNo", "")));
            payment.setRefundTransferProofUrl(null);
        } else {
            payment.setRefundVnpayResponseCode(null);
            payment.setRefundVnpayTransactionNo(null);
            payment.setRefundTransferProofUrl(null);
        }
        paymentRepository.save(payment);

        order.setStatus("returned");
        order.setPaymentStatus("refunded");
        orderRepository.save(order);

        restoreStock(order);
        updateInvoiceStatus(order, InvoiceStatus.REFUNDED);
        emailService.sendRefundNotice(order);

        return buildRefundResponse(order, payment);
    }

    @Override
    public ReturnRequestInfoDto rejectReturnRequest(Long orderId, RejectReturnRequestDto req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        if (!"return_requested".equals(order.getStatus())) {
            throw AppException.badRequest("Đơn hàng chưa ở trạng thái yêu cầu hoàn hàng");
        }
        if (!"refund_requested".equals(order.getPaymentStatus())) {
            throw AppException.badRequest("Đơn hàng chưa có yêu cầu hoàn hàng");
        }

        Payment payment = order.getPayment();
        if (payment == null || !supportsRefund(payment.getMethod())) {
            throw AppException.badRequest("Phương thức thanh toán không hỗ trợ hoàn tiền");
        }

        payment.setStatus("success");
        payment.setRefundRejectedAt(LocalDateTime.now());
        payment.setRefundRejectedBy(getCurrentUserEmail());
        payment.setRefundRejectReason(req.getReason());
        payment.setRefundApprovedAt(null);
        payment.setRefundApprovedBy(null);
        payment.setRefundAmount(null);
        payment.setRefundVnpayResponseCode(null);
        payment.setRefundVnpayTransactionNo(null);
        paymentRepository.save(payment);

        order.setStatus("recjected_refund");
        order.setPaymentStatus("paid");
        orderRepository.save(order);

        return buildReturnRequestInfo(order);
    }

    @Override
    public RefundResponseDto refundByError(AdminRefundRequestDto req, String adminUsername,
            HttpServletRequest httpReq) {
        Order order = orderRepository.findByOrderCode(req.getOrderCode())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        Payment payment = order.getPayment();
        if (payment == null || !"vnpay".equals(payment.getMethod())) {
            throw AppException.badRequest("Phương thức thanh toán không hỗ trợ hoàn tiền");
        }
        if (!"paid".equals(order.getPaymentStatus()) && !"refund_requested".equals(order.getPaymentStatus())) {
            throw AppException.badRequest("Đơn hàng chưa thanh toán hoặc đã được hoàn tiền");
        }

        BigDecimal amount = req.getAmount();
        if (amount.compareTo(order.getTotal()) > 0) {
            throw AppException.badRequest("Số tiền hoàn không được lớn hơn tổng đơn hàng");
        }

        String txnRef = payment.getVnpayTxnRef();
        String transactionNo = payment.getVnpayTransactionNo();
        String payDate = payment.getVnpayPayDate();

        if (transactionNo == null || transactionNo.isBlank()) {
            throw AppException.badRequest("Thiếu mã giao dịch VNPay, không thể hoàn tiền");
        }

        String ip = vnpayUtil.getClientIp(httpReq);
        String orderInfo = "Hoan tien loi don hang " + order.getOrderCode();

        @SuppressWarnings("unchecked")
        Map<String, Object> refundRes = vnpayUtil.refund(
                txnRef, transactionNo, amount, payDate,
                adminUsername, ip, orderInfo, "02");

        String responseCode = String.valueOf(refundRes.getOrDefault("vnp_ResponseCode", ""));
        if (!"00".equals(responseCode)) {
            String message = String.valueOf(refundRes.getOrDefault("vnp_Message", "Unknown error"));
            throw AppException.badRequest("VNPay hoàn tiền thất bại: " + responseCode + " - " + message);
        }

        payment.setStatus("refunded");
        payment.setRefundAmount(amount);
        payment.setRefundReason(req.getReason());
        payment.setRefundApprovedAt(LocalDateTime.now());
        payment.setRefundApprovedBy(adminUsername);
        payment.setRefundVnpayResponseCode(responseCode);
        payment.setRefundVnpayTransactionNo(String.valueOf(refundRes.getOrDefault("vnp_TransactionNo", "")));
        paymentRepository.save(payment);

        order.setStatus("refunded_error");
        order.setPaymentStatus("refunded");
        orderRepository.save(order);

        restoreStock(order);
        updateInvoiceStatus(order, InvoiceStatus.REFUNDED);
        emailService.sendRefundNotice(order);

        return buildRefundResponse(order, payment);
    }

    private RefundResponseDto buildRefundResponse(Order order, Payment payment) {
        return RefundResponseDto.builder()
                .orderCode(order.getOrderCode())
                .paymentStatus(order.getPaymentStatus())
                .orderStatus(order.getStatus())
                .refundAmount(payment.getRefundAmount())
                .refundReason(payment.getRefundReason())
                .refundBankInfo(payment.getRefundBankInfo())
                .refundRequestDate(payment.getRefundRequestDate())
                .refundApprovedAt(payment.getRefundApprovedAt())
                .refundApprovedBy(payment.getRefundApprovedBy())
                .refundVnpayResponseCode(payment.getRefundVnpayResponseCode())
                .refundVnpayTransactionNo(payment.getRefundVnpayTransactionNo())
                .refundTransferProofUrl(payment.getRefundTransferProofUrl())
                .build();
    }

    private String getCurrentUserEmail() {
        // Lấy email từ SecurityContext
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getClaimAsString("email");
        }
        return "admin";
    }

    private ReturnRequestInfoDto buildReturnRequestInfo(Order order) {
        Payment payment = order.getPayment();
        if (payment == null || payment.getRefundRequestDate() == null) {
            throw AppException.badRequest("ÄÆ¡n hÃ ng chÆ°a cÃ³ yÃªu cáº§u hoÃ n hÃ ng");
        }

        return ReturnRequestInfoDto.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .orderStatus(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderTotal(order.getTotal())
                .customerName(order.getUser() != null ? order.getUser().getFullName() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .customerPhone(order.getShipPhone())
                .refundReason(payment.getRefundReason())
                .refundBankInfo(payment.getRefundBankInfo())
                .refundRequestDate(payment.getRefundRequestDate())
                .refundAmount(payment.getRefundAmount())
                .refundApprovedAt(payment.getRefundApprovedAt())
                .refundApprovedBy(payment.getRefundApprovedBy())
                .refundRejectedAt(payment.getRefundRejectedAt())
                .refundRejectedBy(payment.getRefundRejectedBy())
                .refundRejectReason(payment.getRefundRejectReason())
                .refundVnpayResponseCode(payment.getRefundVnpayResponseCode())
                .refundVnpayTransactionNo(payment.getRefundVnpayTransactionNo())
                .refundTransferProofUrl(payment.getRefundTransferProofUrl())
                .imageUrls(payment.getRefundImages() == null ? List.of()
                        : payment.getRefundImages().stream().map(PaymentRefundImage::getImageUrl).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetail(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));
        return toOrderDetailDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestInfoDto getMyReturnRequestInfo(Long userId, Long orderId) {
        Order order = orderRepository.findByIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> AppException.notFound("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n hÃ ng"));
        return buildReturnRequestInfo(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetailAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));
        return toOrderDetailDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public ReturnRequestInfoDto getReturnRequestInfo(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        Payment payment = order.getPayment();
        if (payment == null || payment.getRefundRequestDate() == null) {
            throw AppException.badRequest("Đơn hàng chưa có yêu cầu hoàn hàng");
        }

        return ReturnRequestInfoDto.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .orderStatus(order.getStatus())
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .orderTotal(order.getTotal())
                .customerName(order.getUser() != null ? order.getUser().getFullName() : null)
                .customerEmail(order.getUser() != null ? order.getUser().getEmail() : null)
                .customerPhone(order.getShipPhone())
                .refundReason(payment.getRefundReason())
                .refundBankInfo(payment.getRefundBankInfo())
                .refundRequestDate(payment.getRefundRequestDate())
                .refundAmount(payment.getRefundAmount())
                .refundApprovedAt(payment.getRefundApprovedAt())
                .refundApprovedBy(payment.getRefundApprovedBy())
                .refundRejectedAt(payment.getRefundRejectedAt())
                .refundRejectedBy(payment.getRefundRejectedBy())
                .refundRejectReason(payment.getRefundRejectReason())
                .refundVnpayResponseCode(payment.getRefundVnpayResponseCode())
                .refundVnpayTransactionNo(payment.getRefundVnpayTransactionNo())
                .refundTransferProofUrl(payment.getRefundTransferProofUrl())
                .imageUrls(payment.getRefundImages() == null ? List.of()
                        : payment.getRefundImages().stream().map(PaymentRefundImage::getImageUrl).toList())
                .build();
    }

    // ─── UC-24: Order history ─────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getMyOrders(Long userId, OrderFilterRequest req) {
        Specification<Order> spec = buildSpec(req, userId);
        PageRequest pageable = PageRequest.of(req.getPage(), req.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return new ResultPaginationDTO(
                new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(),
                        page.getTotalPages(), page.getTotalElements()),
                page.map(this::toOrderSummaryDto).getContent());
    }

    // ─── UC-23: Cancel by user ────────────────────────────

    @Override
    public OrderDetailDto cancelByUser(Long userId, Long orderId, CancelOrderRequest req) {
        Order order = orderRepository.findByIdAndUserUserId(orderId, userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        if (!"pending".equals(order.getStatus())) {
            throw AppException.badRequest(
                    "Chỉ có thể hủy đơn hàng ở trạng thái chờ xác nhận. " +
                            "Vui lòng liên hệ hỗ trợ để được trợ giúp.");
        }

        order.setStatus("cancelled");
        order.setCancelReason(req.getReason());
        order.setCancelledBy("user");
        order.setCancelledAt(LocalDateTime.now());
        restoreStock(order);

        if ("paid".equals(order.getPaymentStatus())) {
            order.setPaymentStatus("refund_requested");
        }

        // User hủy đơn → Invoice CANCELLED
        updateInvoiceStatus(order, InvoiceStatus.CANCELLED);

        emailService.sendCancellationNotice(order);
        return toOrderDetailDto(orderRepository.save(order));
    }

    // ─── UC-33: Admin update status ───────────────────────

    @Override
    public OrderDetailDto updateOrderStatus(Long orderId, UpdateOrderStatusRequest req) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy đơn hàng"));

        Map<String, List<String>> allowed = Map.of(
                "pending", List.of("confirmed", "cancelled"),
                "confirmed", List.of("shipping", "cancelled"),
                "shipping", List.of("completed"));

        List<String> validNext = allowed.getOrDefault(order.getStatus(), List.of());
        if (!validNext.contains(req.getStatus())) {
            throw AppException.badRequest("Không thể chuyển trạng thái từ '"
                    + order.getStatus() + "' sang '" + req.getStatus() + "'");
        }

        if ("shipping".equals(req.getStatus())) {
            if (req.getTrackingCode() == null || req.getTrackingCode().isBlank()) {
                throw AppException.badRequest("Mã vận đơn là bắt buộc khi chuyển sang trạng thái 'shipping'");
            }
            order.setTrackingCode(req.getTrackingCode());
        }

        if ("cancelled".equals(req.getStatus())) {
            if (req.getReason() == null || req.getReason().isBlank()) {
                throw AppException.badRequest("Lý do hủy là bắt buộc");
            }
            order.setCancelReason(req.getReason());
            order.setCancelledBy("admin");
            order.setCancelledAt(LocalDateTime.now());
            restoreStock(order);
            if ("paid".equals(order.getPaymentStatus())) {
                order.setPaymentStatus("refund_requested");
            }
            // Admin hủy đơn → Invoice CANCELLED
            updateInvoiceStatus(order, InvoiceStatus.CANCELLED);
        }

        // COD: giao hàng thành công → Invoice PAID
        if ("completed".equals(req.getStatus()) && "cod".equals(order.getPaymentMethod())) {
            order.setPaymentStatus("paid");
            updateInvoiceStatus(order, InvoiceStatus.PAID);
        }

        order.setStatus(req.getStatus());
        Order savedOrder = orderRepository.save(order);

        // Initialize lazy user fields before leaving the transaction; email is sent
        // after commit.
        if (savedOrder.getUser() != null) {
            savedOrder.getUser().getFullName();
            savedOrder.getUser().getEmail();
        }

        runAfterCommit(() -> {
            emailService.sendStatusUpdateNotice(savedOrder);
            if ("completed".equals(savedOrder.getStatus())) {
                emailService.sendReviewInvitation(savedOrder);
            }
        });

        return toOrderDetailDto(savedOrder);
    }

    // ─── UC-33: Admin list ────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ResultPaginationDTO getAllOrders(OrderFilterRequest req) {
        Specification<Order> spec = buildSpec(req, null);
        PageRequest pageable = PageRequest.of(req.getPage(), req.getSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> page = orderRepository.findAll(spec, pageable);
        return new ResultPaginationDTO(
                new ResultPaginationDTO.Meta(page.getNumber(), page.getSize(),
                        page.getTotalPages(), page.getTotalElements()),
                page.map(this::toOrderSummaryDto).getContent());
    }

    // ─── UC-34: Payment settings ──────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PaymentSettingDto> getPaymentSettings() {
        return paymentSettingRepository.findAll().stream().map(this::toPaymentSettingDto).toList();
    }

    @Override
    public PaymentSettingDto togglePaymentMethod(String method) {
        PaymentSetting setting = paymentSettingRepository.findById(method)
                .orElseThrow(() -> AppException.notFound("Phương thức '" + method + "' không tồn tại"));
        setting.setEnabled(!setting.isEnabled());
        return toPaymentSettingDto(paymentSettingRepository.save(setting));
    }

    // ─── UC-35: Reconciliation ────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ReconcileResultDto> reconcile(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);
        return paymentRepository.findByMethodAndCreatedAtBetween("vnpay", start, end)
                .stream()
                .map(p -> ReconcileResultDto.builder()
                        .orderCode(p.getOrder().getOrderCode())
                        .localPaymentStatus(p.getStatus())
                        .vnpayTransactionNo(p.getVnpayTransactionNo())
                        .matched(p.getVnpayTransactionNo() != null)
                        .build())
                .toList();
    }

    // ─── Private helpers ──────────────────────────────────

    private boolean supportsRefund(String paymentMethod) {
        return "vnpay".equals(paymentMethod) || "cod".equals(paymentMethod);
    }

    private boolean requiresRefundBankInfo(Order order) {
        return order != null && "cod".equals(order.getPaymentMethod());
    }

    private String normalizeRefundBankInfo(String refundBankInfo) {
        return isBlank(refundBankInfo) ? null : refundBankInfo.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void createInvoice(Order order) {
        String dateStr = java.time.LocalDate.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = invoiceRepository.count() + 1;
        String invoiceCode = String.format("INV-%s-%04d", dateStr, count);

        Invoice invoice = Invoice.builder()
                .invoiceCode(invoiceCode)
                .order(order)
                .status(InvoiceStatus.PENDING)
                .issuedDate(java.time.LocalDate.now())
                .subtotalAmount(order.getSubTotal())
                .discountAmount(order.getDiscountAmount())
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(order.getTotal())
                .build();
        invoiceRepository.save(invoice);
    }

    private void updateInvoiceStatus(Order order, InvoiceStatus status) {
        invoiceRepository.findByOrderId(order.getId())
                .ifPresent(invoice -> {
                    invoice.setStatus(status);
                    invoiceRepository.save(invoice);
                });
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = variantRepository.findById(item.getVariant().getId())
                    .orElse(null);
            if (variant != null) {
                variant.setStockQty(variant.getStockQty() + item.getQuantity());
                variantRepository.save(variant);
            }
        }
    }

    private Specification<Order> buildSpec(OrderFilterRequest req, Long forceUserId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            Long uid = forceUserId != null ? forceUserId : req.getUserId();
            if (uid != null) {
                predicates.add(cb.equal(root.get("user").get("userId"), uid));
            }
            if (req.getStatus() != null && !req.getStatus().isBlank()) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                predicates.add(cb.like(root.get("orderCode"), "%" + req.getKeyword() + "%"));
            }
            if (req.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        req.getFromDate().atStartOfDay()));
            }
            if (req.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        req.getToDate().atTime(23, 59, 59)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private OrderDetailDto toOrderDetailDto(Order o) {
        String addressLine = String.join(", ",
                nvl(o.getShipStreet()), nvl(o.getShipWard()),
                nvl(o.getShipDistrict()), nvl(o.getShipProvince()));

        List<OrderItemDto> itemDtos = o.getItems() == null ? List.of()
                : o.getItems().stream().map(i -> OrderItemDto.builder()
                        .orderItemId(i.getId())
                        .productId(i.getVariant().getProduct().getId())
                        .variantId(i.getVariant().getId())
                        .productName(i.getProductName())
                        .color(i.getColor())
                        .size(i.getSize())
                        .thumbnailUrl(i.getThumbnailUrl())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build()).toList();

        PaymentDto paymentDto = null;
        if (o.getPayment() != null) {
            Payment p = o.getPayment();
            paymentDto = PaymentDto.builder()
                    .paymentId(p.getId())
                    .method(p.getMethod())
                    .status(p.getStatus())
                    .amount(p.getAmount())
                    .vnpayTransactionNo(p.getVnpayTransactionNo())
                    .paidAt(p.getPaidAt())
                    .build();
        }

        return OrderDetailDto.builder()
                .orderId(o.getId())
                .orderCode(o.getOrderCode())
                .status(o.getStatus())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .subTotal(o.getSubTotal())
                .discountAmount(o.getDiscountAmount())
                .total(o.getTotal())
                .voucherCode(o.getVoucher() != null ? o.getVoucher().getVoucherCode() : null)
                .note(o.getNote())
                .trackingCode(o.getTrackingCode())
                .cancelReason(o.getCancelReason())
                .recipientName(o.getShipFullName())
                .recipientPhone(o.getShipPhone())
                .addressLine(addressLine)
                .items(itemDtos)
                .payment(paymentDto)
                .createdAt(o.getCreatedAt())
                .updatedAt(o.getUpdatedAt())
                .build();
    }

    private OrderSummaryDto toOrderSummaryDto(Order o) {
        return OrderSummaryDto.builder()
                .orderId(o.getId())
                .orderCode(o.getOrderCode())
                .status(o.getStatus())
                .paymentMethod(o.getPaymentMethod())
                .paymentStatus(o.getPaymentStatus())
                .total(o.getTotal())
                .itemCount(o.getItems() == null ? 0 : o.getItems().size())
                .customerName(o.getUser() != null ? o.getUser().getFullName() : null)
                .customerEmail(o.getUser() != null ? o.getUser().getEmail() : null)
                .createdAt(o.getCreatedAt())
                .build();
    }

    private PaymentSettingDto toPaymentSettingDto(PaymentSetting s) {
        return PaymentSettingDto.builder()
                .method(s.getMethod())
                .isEnabled(s.isEnabled())
                .build();
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private void recalculateCart(Cart cart) {
        BigDecimal subTotal = cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        cart.setSubTotal(subTotal);

        BigDecimal discount = BigDecimal.ZERO;
        Voucher v = cart.getVoucher();
        if (v != null) {
            if (subTotal.compareTo(v.getMinOrderValue()) >= 0) {
                if ("percent".equalsIgnoreCase(v.getType())) {
                    discount = subTotal.multiply(v.getDiscountValue()).divide(BigDecimal.valueOf(100));
                    if (v.getMaxDiscountCap() != null && discount.compareTo(v.getMaxDiscountCap()) > 0)
                        discount = v.getMaxDiscountCap();
                } else {
                    discount = v.getDiscountValue();
                }
                if (discount.compareTo(subTotal) > 0)
                    discount = subTotal;
            } else {
                cart.setVoucher(null);
            }
        }
        cart.setDiscountAmount(discount);
        cart.setTotal(subTotal.subtract(discount));
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
