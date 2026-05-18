package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.cart.AddToCartRequest;
import com.clothingstore.clothing_store_be.dto.cart.CartDto;
import com.clothingstore.clothing_store_be.dto.cart.CartItemDto;
import com.clothingstore.clothing_store_be.dto.cart.UpdateCartItemRequest;
import com.clothingstore.clothing_store_be.entity.*;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.*;
import com.clothingstore.clothing_store_be.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final VoucherRepository voucherRepository;
    private final UserRepository userRepository;

    @Override
    public CartDto getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        return toDto(cart);
    }

    @Override
    public CartDto addItem(Long userId, AddToCartRequest req) {
        Cart cart = getOrCreateCart(userId);

        ProductVariant variant = variantRepository.findById(req.getVariantId())
                .orElseThrow(() -> AppException.notFound("Không tìm thấy variant với id: " + req.getVariantId()));

        if (variant.getStockQty() < req.getQuantity()) {
            throw AppException.badRequest("Số lượng tồn kho không đủ (còn " + variant.getStockQty() + ")");
        }

        Optional<CartItem> existing = cartItemRepository
                .findByCartIdAndVariantId(cart.getId(), req.getVariantId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + req.getQuantity();
            if (variant.getStockQty() < newQty) {
                throw AppException.badRequest("Số lượng tồn kho không đủ (còn " + variant.getStockQty() + ")");
            }
            item.setQuantity(newQty);
        } else {
            BigDecimal price = variant.getSalePrice() != null
                    ? variant.getSalePrice()
                    : variant.getProduct().getBasePrice();

            CartItem item = CartItem.builder()
                    .cart(cart)
                    .variant(variant)
                    .quantity(req.getQuantity())
                    .unitPrice(price)
                    .build();
            cart.getItems().add(item);
        }

        recalculate(cart);
        return toDto(cartRepository.save(cart));
    }

    @Override
    public CartDto updateItem(Long userId, Long itemId, UpdateCartItemRequest req) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findItemInCart(cart, itemId);

        if (item.getVariant().getStockQty() < req.getQuantity()) {
            throw AppException.badRequest("Số lượng tồn kho không đủ (còn " + item.getVariant().getStockQty() + ")");
        }

        item.setQuantity(req.getQuantity());
        recalculate(cart);
        return toDto(cartRepository.save(cart));
    }

    @Override
    public CartDto removeItem(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = findItemInCart(cart, itemId);
        cart.getItems().remove(item);
        recalculate(cart);
        return toDto(cartRepository.save(cart));
    }

    @Override
    public CartDto clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.getItems().clear();
        cart.setVoucher(null);
        cart.setSubTotal(BigDecimal.ZERO);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setTotal(BigDecimal.ZERO);
        return toDto(cartRepository.save(cart));
    }

    @Override
    public CartDto applyVoucher(Long userId, String voucherCode) {
        Cart cart = getOrCreateCart(userId);

        Voucher voucher = voucherRepository.findByVoucherCodeAndActiveTrueAndDeletedAtIsNull(voucherCode)
                .orElseThrow(() -> AppException.notFound("Mã voucher không hợp lệ hoặc không hoạt động"));

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
        if (cart.getSubTotal().compareTo(voucher.getMinOrderValue()) < 0) {
            throw AppException.badRequest(
                    "Đơn hàng tối thiểu " + voucher.getMinOrderValue() + " để dùng voucher này");
        }

        cart.setVoucher(voucher);
        recalculate(cart);
        return toDto(cartRepository.save(cart));
    }

    @Override
    public CartDto removeVoucher(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.setVoucher(null);
        recalculate(cart);
        return toDto(cartRepository.save(cart));
    }

    // ─── Helpers ─────────────────────────────────────────

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> AppException.notFound("Người dùng không tồn tại"));
            Cart cart = Cart.builder().user(user).build();
            return cartRepository.save(cart);
        });
    }

    private CartItem findItemInCart(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> AppException.notFound("Không tìm thấy item trong giỏ hàng"));
    }

    private void recalculate(Cart cart) {
        BigDecimal subTotal = cart.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        cart.setSubTotal(subTotal);

        BigDecimal discount = BigDecimal.ZERO;
        if (cart.getVoucher() != null) {
            Voucher v = cart.getVoucher();
            if ("percent".equalsIgnoreCase(v.getType())) {
                discount = subTotal.multiply(v.getDiscountValue())
                        .divide(BigDecimal.valueOf(100));
                // Áp trần giảm nếu có
                if (v.getMaxDiscountCap() != null && discount.compareTo(v.getMaxDiscountCap()) > 0) {
                    discount = v.getMaxDiscountCap();
                }
            } else {
                discount = v.getDiscountValue();
            }
            if (discount.compareTo(subTotal) > 0) {
                discount = subTotal;
            }
        }

        cart.setDiscountAmount(discount);
        cart.setTotal(subTotal.subtract(discount));
    }

    private CartDto toDto(Cart cart) {
        List<CartItemDto> itemDtos = cart.getItems().stream()
                .map(this::toItemDto)
                .collect(Collectors.toList());

        return CartDto.builder()
                .id(cart.getId())
                .voucherCode(cart.getVoucher() != null ? cart.getVoucher().getVoucherCode() : null)
                .subTotal(cart.getSubTotal())
                .discountAmount(cart.getDiscountAmount())
                .total(cart.getTotal())
                .items(itemDtos)
                .build();
    }

    private CartItemDto toItemDto(CartItem item) {
        ProductVariant v = item.getVariant();
        Product p = v.getProduct();
        return CartItemDto.builder()
                .id(item.getId())
                .variantId(v.getId())
                .productId(p.getId())
                .productName(p.getName())
                .thumbnailUrl(p.getThumbnailUrl())
                .color(v.getColor())
                .size(v.getSize())
                .sku(v.getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .build();
    }
}
