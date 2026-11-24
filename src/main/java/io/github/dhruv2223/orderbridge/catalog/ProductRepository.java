package io.github.dhruv2223.orderbridge.catalog;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;

public interface ProductRepository
		extends ListCrudRepository<Product, Long>, ListPagingAndSortingRepository<Product, Long> {

	Optional<Product> findBySku(String sku);

	Page<Product> findByCategory(String category, Pageable pageable);

	boolean existsBySku(String sku);

}
