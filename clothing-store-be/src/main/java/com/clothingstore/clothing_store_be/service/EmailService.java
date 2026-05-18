package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.entity.Order;

public interface EmailService {
    void sendVerifyEmail(String toEmail, String name, String verifyUrl);

    void sendResetPasswordEmail(String toEmail, String name, String resetUrl);

    void sendOrderConfirmation(Order order);

    void sendCancellationNotice(Order order);

    void sendStatusUpdateNotice(Order order);

    void sendReviewInvitation(Order order);

    void sendRefundNotice(Order order);
}