package ru.activecrowd.service.rest.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/testers")
@RequiredArgsConstructor
public final class TestersRestController {

    @NonNull
    private final JdbcOperations jdbcOperations;

    @GetMapping("count") //completed
    public ResponseEntity<Integer> getCountTesters(@RequestParam(name = "agemin", required = false) Integer agemin
                                                  ,@RequestParam(name = "agemax", required = false) Integer agemax
                                                  ,@RequestParam(name = "city", required = false) String city
                                                  ,@RequestParam(name = "gender", required = false) String gender) {
        String ageminQuery = agemin == null? "" : " AND ((SELECT now()) - birthdate) >= interval '"+ agemin +" year' ";
        String agemaxQuery = agemax == null? "" : " AND ((SELECT now()) - birthdate) <= interval '"+ agemax +" year' ";
        String cityQuery = city == null? "" : " AND city = '" + city.toUpperCase() + "'";
        String genderQuery = gender == null? "" : " AND gender = '" + gender.toUpperCase() + "'";
        return ResponseEntity.ok(
                this.jdbcOperations.queryForObject(
                        "SELECT count(id) FROM testers WHERE status = 'FREE'" +
                                ageminQuery + agemaxQuery + cityQuery + genderQuery + ";"
                        , (resultSet, i) ->
                Integer.valueOf(resultSet.getInt("count" ))));
    }
}
