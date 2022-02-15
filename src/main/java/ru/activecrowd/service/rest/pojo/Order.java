package ru.activecrowd.service.rest.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Order {
    private String status;
    private UUID key;
    private BigDecimal price;
    private OrderProperties orderProperties;
}
