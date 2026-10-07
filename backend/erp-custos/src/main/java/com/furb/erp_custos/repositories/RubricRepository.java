package com.furb.erp_custos.repositories;

import com.furb.erp_custos.entities.RubricEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RubricRepository extends JpaRepository<RubricEntity, UUID> {

    Optional<RubricEntity> findByCode(String code);

    /** Rubricas que têm conta vinculada, já com a conta carregada. */
    @Query("select r from RubricEntity r join fetch r.account order by r.code")
    List<RubricEntity> findAllWithAccount();
}
