package com.mckevin.agentcrm.domain.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Contract(String number, ProductLine product, String label, ContractStatus status, BigDecimal annualPremium,
                       LocalDate startDate, LocalDate renewalDate) {

    public boolean isActive() {
        return status == ContractStatus.ACTIVE;
    }

    public long daysUntilRenewal(LocalDate today) {
        return ChronoUnit.DAYS.between(today, renewalDate);
    }
}
