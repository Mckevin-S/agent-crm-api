package com.mckevin.agentcrm.domain.customer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record Claim(String number, String contractNumber, String nature, ClaimStatus status, LocalDate openedOn,
                    BigDecimal estimatedAmount) {

    public long ageInDays(LocalDate today) {
        return ChronoUnit.DAYS.between(openedOn, today);
    }
}
