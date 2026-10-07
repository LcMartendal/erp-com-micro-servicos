package com.furb.erp_custos.repositories;

import com.furb.erp_custos.entities.ProductionAppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductionAppointmentRepository extends JpaRepository<ProductionAppointmentEntity, UUID> {

    List<ProductionAppointmentEntity> findByCode(String code);

    List<ProductionAppointmentEntity> findByDateTimeBetween(LocalDateTime start, LocalDateTime end);

    /** Intervalo semiaberto: start inclusive, end exclusivo (evita problemas de precisão no fim do dia). */
    @Query("select a from ProductionAppointmentEntity a where a.dateTime >= :start and a.dateTime < :end order by a.dateTime")
    List<ProductionAppointmentEntity> findInPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
