package com.furb.erp_custos.service;

import com.furb.erp_custos.dto.PeriodDto;
import com.furb.erp_custos.dto.ProductionAppointmentsDto;
import com.furb.erp_custos.repositories.ProductionAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** Busca os apontamentos de produção de um período (dataInicio e dataFim inclusivas). */
@Service
@RequiredArgsConstructor
public class ProductionAppointmentService {

    private final ProductionAppointmentRepository appointmentRepository;

    @Transactional(readOnly = true)
    public List<ProductionAppointmentsDto> findByPeriod(PeriodDto period) {
        validate(period);

        LocalDateTime start = period.dataInicio().atStartOfDay();
        LocalDateTime end = period.dataFim().plusDays(1).atStartOfDay();

        List<ProductionAppointmentsDto> appointments = appointmentRepository.findInPeriod(start, end).stream()
                .map(a -> new ProductionAppointmentsDto(
                        a.getId(), a.getCode(), a.getTotalQuantity(), a.getTotalHours(), a.getDateTime()))
                .toList();

        if (appointments.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhum apontamento de produção entre " + period.dataInicio() + " e " + period.dataFim());
        }
        return appointments;
    }

    private void validate(PeriodDto period) {
        if (period == null || period.dataInicio() == null || period.dataFim() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe dataInicio e dataFim (yyyy-MM-dd)");
        }
        if (period.dataFim().isBefore(period.dataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dataFim não pode ser anterior a dataInicio");
        }
    }
}
