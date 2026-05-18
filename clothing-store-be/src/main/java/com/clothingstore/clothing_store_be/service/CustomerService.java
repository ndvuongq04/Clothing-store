package com.clothingstore.clothing_store_be.service;

import com.clothingstore.clothing_store_be.dto.customer.CustomerDetailDto;
import com.clothingstore.clothing_store_be.dto.customer.CustomerFilterRequest;
import com.clothingstore.clothing_store_be.dto.response.ResultPaginationDTO;

public interface CustomerService {

    ResultPaginationDTO getCustomers(CustomerFilterRequest req);

    CustomerDetailDto getCustomerDetail(Long userId);
}
