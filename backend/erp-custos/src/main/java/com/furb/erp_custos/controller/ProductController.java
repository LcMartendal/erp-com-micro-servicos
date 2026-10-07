package com.furb.erp_custos.controller;

import com.furb.erp_custos.dto.CalculationRequestedDto;
import com.furb.erp_custos.dto.PeriodDto;
import com.furb.erp_custos.dto.ProductDto;
import com.furb.erp_custos.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "produto")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /** Dispara o cálculo dos custos (exige as taxas já calculadas). Corpo: {"dataInicio":"...","dataFim":"..."} */
    @PostMapping
    public ResponseEntity<CalculationRequestedDto> calculateProductCosts(@RequestBody PeriodDto period) {
        return ResponseEntity.accepted().body(productService.requestCalculation(period));
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getProducts() {
        return ResponseEntity.ok(productService.findAll());
    }
}
