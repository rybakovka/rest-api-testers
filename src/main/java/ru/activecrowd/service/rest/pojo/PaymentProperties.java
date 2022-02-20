package ru.activecrowd.service.rest.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class PaymentProperties {
    private BigDecimal sum;
    private String customer;
}
