package com.furb.erp_custos.repositories;

import com.furb.erp_custos.entities.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, UUID> {

    Optional<ProductEntity> findByCode(String code);

    List<ProductEntity> findByCodeIn(Collection<String> codes);
}
