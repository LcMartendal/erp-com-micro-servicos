package com.furb.erp_custos.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "rubric")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RubricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code;

    /** Conta cujo total é rateado pelas horas-máquina para gerar a taxa desta rubrica. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private AccountEntity account;

    /** Taxa da rubrica (ex.: R$ por hora-máquina). Nula até o cálculo rodar. */
    @Column(name = "rate_value", precision = 19, scale = 4)
    private BigDecimal value;
}
