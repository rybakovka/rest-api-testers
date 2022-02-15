package ru.activecrowd.service.rest.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderProperties {
    private Integer amount;
    private Integer agemin;
    private Integer agemax;
    private String city;
    private String gender;
}
