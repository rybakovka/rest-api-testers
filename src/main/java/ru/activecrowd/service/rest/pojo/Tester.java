package ru.activecrowd.service.rest.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public final class Tester {
    private Integer id;
    private String email;
    private String phone;
    private String status;
    private UUID orderId;
    private Timestamp birthdate;
    private String city;
    private String gender;
}
