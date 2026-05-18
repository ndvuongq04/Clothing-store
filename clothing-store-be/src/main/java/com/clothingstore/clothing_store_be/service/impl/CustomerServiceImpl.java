package com.clothingstore.clothing_store_be.service.impl;

import com.clothingstore.clothing_store_be.dto.customer.CustomerDetailDto;
import com.clothingstore.clothing_store_be.dto.customer.CustomerFilterRequest;
import com.clothingstore.clothing_store_be.dto.customer.CustomerSummaryDto;
import com.clothingstore.clothing_store_be.dto.order.OrderSummaryDto;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;
import com.clothingstore.clothing_store_be.entity.Order;
import com.clothingstore.clothing_store_be.entity.User;
import com.clothingstore.clothing_store_be.exception.AppException;
import com.clothingstore.clothing_store_be.repository.OrderRepository;
import com.clothingstore.clothing_store_be.repository.UserRepository;
import com.clothingstore.clothing_store_be.service.CustomerService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerServiceImpl implements CustomerService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    public ResultPaginationDTO getCustomers(CustomerFilterRequest req) {
        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("role"), "user"));

            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                String like = "%" + req.getKeyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("fullName"), like),
                        cb.like(root.get("email"), like)
                ));
            }
            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        PageRequest pageable = PageRequest.of(req.getPage(), req.getPageSize(),
                Sort.by(Sort.Direction.DESC, "userId"));
        Page<User> page = userRepository.findAll(spec, pageable);

        ResultPaginationDTO.Meta meta = new ResultPaginationDTO.Meta(
                page.getNumber(), page.getSize(),
                page.getTotalPages(), page.getTotalElements());

        List<CustomerSummaryDto> content = page.getContent().stream()
                .map(u -> CustomerSummaryDto.builder()
                        .userId(u.getUserId())
                        .fullName(u.getFullName())
                        .email(u.getEmail())
                        .phoneNumber(u.getPhoneNumber())
                        .gender(u.getGender() != null ? u.getGender().name() : null)
                        .status(u.isStatus())
                        .emailVerified(u.isEmailVerified())
                        .totalOrders(orderRepository.countByUserUserId(u.getUserId()))
                        .build())
                .toList();

        return new ResultPaginationDTO(meta, content);
    }

    @Override
    public CustomerDetailDto getCustomerDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Không tìm thấy khách hàng"));

        if (!"user".equals(user.getRole())) {
            throw AppException.notFound("Không tìm thấy khách hàng");
        }

        long totalOrders = orderRepository.countByUserUserId(userId);
        long completedOrders = orderRepository.countByUserUserIdAndStatus(userId, "completed");
        long cancelledOrders = orderRepository.countByUserUserIdAndStatus(userId, "cancelled");

        List<Order> recentRaw = orderRepository
                .findByUserUserId(userId, PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent();

        List<OrderSummaryDto> recentOrders = recentRaw.stream()
                .map(o -> OrderSummaryDto.builder()
                        .orderId(o.getId())
                        .orderCode(o.getOrderCode())
                        .status(o.getStatus())
                        .paymentMethod(o.getPaymentMethod())
                        .paymentStatus(o.getPaymentStatus())
                        .total(o.getTotal())
                        .itemCount(o.getItems() == null ? 0 : o.getItems().size())
                        .createdAt(o.getCreatedAt())
                        .build())
                .toList();

        return CustomerDetailDto.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .dateOfBirth(user.getDateOfBirth())
                .status(user.isStatus())
                .emailVerified(user.isEmailVerified())
                .totalOrders(totalOrders)
                .totalSpent(orderRepository.sumTotalByUserId(userId))
                .completedOrders(completedOrders)
                .cancelledOrders(cancelledOrders)
                .recentOrders(recentOrders)
                .build();
    }
}
