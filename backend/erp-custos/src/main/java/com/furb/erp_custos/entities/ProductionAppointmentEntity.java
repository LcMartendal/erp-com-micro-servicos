package com.furb.erp_custos.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "production_appointment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductionAppointmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Código do produto apontado (casa com Product.code). */
    @Column(nullable = false)
    private String code;

    @Column(name = "total_quantity", precision = 19, scale = 4)
    private BigDecimal totalQuantity;

    @Column(name = "total_hours")
    private Integer totalHours;

    @Column(name = "date_time")
    private LocalDateTime dateTime;
}
