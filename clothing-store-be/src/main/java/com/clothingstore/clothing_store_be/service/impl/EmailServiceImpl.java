package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.entity.Order;
import com.clothingstore.clothing_store_be.entity.OrderItem;
import com.clothingstore.clothing_store_be.entity.Payment;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ByteArrayResource;
import com.clothingstore.clothing_store_be.service.InvoiceService;
import org.springframework.beans.factory.annotation.Value;

import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final ApplicationContext applicationContext;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat CURRENCY_FMT = NumberFormat.getInstance(Locale.of("vi", "VN"));

    // ========================= PUBLIC METHODS =========================

    @Override
    public void sendVerifyEmail(String toEmail, String name, String verifyUrl) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("verifyUrl", verifyUrl);
        sendHtmlEmail(toEmail, "Xác thực tài khoản Clothing Store", "verify-email", vars);
    }

    @Override
    @Async
    public void sendResetPasswordEmail(String toEmail, String name, String resetUrl) {
        Map<String, Object> vars = new HashMap<>();
        vars.put("name", name);
        vars.put("resetUrl", resetUrl);
        sendHtmlEmail(toEmail, "Đặt lại mật khẩu - Clothing Store", "reset-password", vars);
    }

    @Override
    @Async
    public void sendOrderConfirmation(Order order) {
        try {
            String customerName = order.getUser().getFullName();
            String toEmail = order.getUser().getEmail();

            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", customerName);
            vars.put("orderCode", order.getOrderCode());
            vars.put("orderDate", order.getCreatedAt().format(DATE_FMT));
            vars.put("paymentMethod", mapPaymentMethod(order.getPaymentMethod()));
            vars.put("status", mapStatus(order.getStatus()));
            vars.put("total", formatCurrency(order.getTotal()));
            vars.put("shippingAddress", buildAddress(order));
            vars.put("items", buildItemList(order.getItems()));

            byte[] invoicePdf = null;
            try {
                // Đợi một chút để transaction ở OrderServiceImpl kịp commit
                Thread.sleep(500);
                InvoiceService invoiceService = applicationContext.getBean(InvoiceService.class);
                invoicePdf = invoiceService.generateInvoicePdf(order.getId());
            } catch (Exception e) {
                log.error("Failed to generate invoice PDF for order {}: {}", order.getOrderCode(), e.getMessage());
            }

            if (invoicePdf != null) {
                String attachmentName = "HoaDon_" + order.getOrderCode() + ".pdf";
                sendHtmlEmailWithAttachment(toEmail,
                        "Xác nhận đơn hàng #" + order.getOrderCode() + " kèm Hóa đơn điện tử", "order-confirmation",
                        vars, invoicePdf, attachmentName);
            } else {
                sendHtmlEmail(toEmail, "Xác nhận đơn hàng #" + order.getOrderCode(), "order-confirmation", vars);
            }
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    @Override
    @Async
    public void sendCancellationNotice(Order order) {
        try {
            String customerName = order.getUser().getFullName();
            String toEmail = order.getUser().getEmail();

            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", customerName);
            vars.put("orderCode", order.getOrderCode());
            vars.put("orderDate", order.getCreatedAt().format(DATE_FMT));
            vars.put("total", formatCurrency(order.getTotal()));
            vars.put("cancelReason", order.getCancelReason() != null ? order.getCancelReason() : "Không rõ");
            vars.put("cancelledBy", "admin".equals(order.getCancelledBy()) ? "Quản trị viên" : "Khách hàng");
            vars.put("paymentMethod", order.getPaymentMethod());

            sendHtmlEmail(toEmail, "Đơn hàng #" + order.getOrderCode() + " đã bị hủy", "order-cancellation", vars);
        } catch (Exception e) {
            log.error("Failed to send cancellation email for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    @Override
    @Async
    public void sendStatusUpdateNotice(Order order) {
        try {
            String customerName = order.getUser().getFullName();
            String toEmail = order.getUser().getEmail();

            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", customerName);
            vars.put("orderCode", order.getOrderCode());
            vars.put("status", order.getStatus());
            vars.put("statusText", mapStatus(order.getStatus()));
            vars.put("trackingCode", order.getTrackingCode());

            sendHtmlEmail(toEmail, "Cập nhật đơn hàng #" + order.getOrderCode(), "order-status-update", vars);
        } catch (Exception e) {
            log.error("Failed to send status update email for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    @Value("${app.review-url:http://localhost:5173/profile/orders}")
    private String reviewUrl;

    @Override
    @Async
    public void sendReviewInvitation(Order order) {
        try {
            String customerName = order.getUser().getFullName();
            String toEmail = order.getUser().getEmail();

            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", customerName);
            vars.put("orderCode", order.getOrderCode());
            vars.put("reviewUrl", reviewUrl);

            sendHtmlEmail(toEmail, "Cảm ơn bạn đã mua hàng! Hãy để lại đánh giá nhé.", "review-invitation", vars);
        } catch (Exception e) {
            log.error("Failed to send review invitation email for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    @Override
    @Async
    public void sendRefundNotice(Order order) {
        try {
            String customerName = order.getUser().getFullName();
            String toEmail = order.getUser().getEmail();
            Payment payment = order.getPayment();

            Map<String, Object> vars = new HashMap<>();
            vars.put("customerName", customerName);
            vars.put("orderCode", order.getOrderCode());
            vars.put("refundAmount",
                    formatCurrency(payment != null ? payment.getRefundAmount() : java.math.BigDecimal.ZERO));
            vars.put("refundReason", payment != null && payment.getRefundReason() != null ? payment.getRefundReason()
                    : "Hoàn tiền theo yêu cầu");
            vars.put("refundDate", java.time.LocalDateTime.now().format(DATE_FMT));

            sendHtmlEmail(toEmail, "Thông báo hoàn tiền đơn hàng #" + order.getOrderCode(), "refund-notice", vars);
        } catch (Exception e) {
            log.error("Failed to send refund notice email for order {}: {}", order.getOrderCode(), e.getMessage());
        }
    }

    // ========================= PRIVATE HELPERS =========================

    /**
     * Phương thức gửi email HTML chung — tái sử dụng cho tất cả các loại email.
     * 
     * @param to       email người nhận
     * @param subject  tiêu đề email
     * @param template tên file template (không cần .html)
     * @param vars     các biến truyền vào template
     */
    private void sendHtmlEmail(String to, String subject, String template, Map<String, Object> vars) {
        try {
            Context context = new Context();
            context.setVariables(vars);
            String htmlContent = templateEngine.process(template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {} with subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Gửi email thất bại");
        }
    }

    private void sendHtmlEmailWithAttachment(String to, String subject, String template, Map<String, Object> vars,
            byte[] attachmentData, String attachmentFilename) {
        try {
            Context context = new Context();
            context.setVariables(vars);
            String htmlContent = templateEngine.process(template, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            if (attachmentData != null && attachmentFilename != null) {
                helper.addAttachment(attachmentFilename, new ByteArrayResource(attachmentData));
            }

            mailSender.send(message);
            log.info("Email sent successfully to {} with subject: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Gửi email thất bại");
        }
    }

    private String mapPaymentMethod(String method) {
        if (method == null)
            return "Không xác định";
        switch (method.toLowerCase()) {
            case "cod":
                return "Thanh toán khi nhận hàng (COD)";
            case "vnpay":
                return "Thanh toán qua VNPay";
            default:
                return method;
        }
    }

    private String mapStatus(String status) {
        if (status == null)
            return "Không xác định";
        switch (status.toLowerCase()) {
            case "pending":
                return "Chờ xác nhận";
            case "confirmed":
                return "Đã xác nhận";
            case "shipping":
                return "Đang giao hàng";
            case "completed":
                return "Hoàn thành";
            case "cancelled":
                return "Đã hủy";
            case "payment_failed":
                return "Thanh toán thất bại";
            default:
                return status;
        }
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        if (amount == null)
            return "0 ₫";
        return CURRENCY_FMT.format(amount) + " ₫";
    }

    private String buildAddress(Order order) {
        StringBuilder sb = new StringBuilder();
        if (order.getShipStreet() != null)
            sb.append(order.getShipStreet()).append(", ");
        if (order.getShipWard() != null)
            sb.append(order.getShipWard()).append(", ");
        if (order.getShipDistrict() != null)
            sb.append(order.getShipDistrict()).append(", ");
        if (order.getShipProvince() != null)
            sb.append(order.getShipProvince());
        return sb.toString();
    }

    private List<Map<String, Object>> buildItemList(List<OrderItem> items) {
        return items.stream().map(item -> {
            Map<String, Object> map = new HashMap<>();
            map.put("productName", item.getProductName());
            map.put("size", item.getSize() != null ? item.getSize() : "");
            map.put("color", item.getColor() != null ? item.getColor() : "");
            map.put("quantity", item.getQuantity());
            map.put("lineTotal", formatCurrency(item.getLineTotal()));
            return map;
        }).collect(Collectors.toList());
    }
}