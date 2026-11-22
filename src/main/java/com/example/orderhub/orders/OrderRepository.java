package com.example.orderhub.orders;

import java.util.List;

import org.springframework.data.repository.ListCrudRepository;

public interface OrderRepository extends ListCrudRepository<PurchaseOrder, Long> {

	List<PurchaseOrder> findByCustomerReferenceOrderByCreatedAtDesc(String customerReference);

	List<PurchaseOrder> findTop50ByOrderByCreatedAtDesc();

}
