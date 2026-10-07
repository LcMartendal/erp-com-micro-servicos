package com.furb.erp_custos.controller;

import com.furb.erp_custos.dto.CalculationRequestedDto;
import com.furb.erp_custos.dto.PeriodDto;
import com.furb.erp_custos.dto.RubricDto;
import com.furb.erp_custos.service.RubricService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "rubrica")
@RequiredArgsConstructor
public class RubricController {

    private final RubricService rubricService;

    /** Dispara o cálculo das taxas. Corpo: {"dataInicio":"2026-08-01","dataFim":"2026-08-31"} */
    @PostMapping
    public ResponseEntity<CalculationRequestedDto> calculateRubricRates(@RequestBody PeriodDto period) {
        return ResponseEntity.accepted().body(rubricService.requestCalculation(period));
    }

    @GetMapping
    public ResponseEntity<List<RubricDto>> getRubrics() {
        return ResponseEntity.ok(rubricService.findAll());
    }
}
