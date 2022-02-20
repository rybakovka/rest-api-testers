package ru.activecrowd.service.rest.controllers;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import ru.activecrowd.service.rest.pojo.*;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/orders")
@RequiredArgsConstructor
public final class OrdersRestController {

    @NonNull
    private final JdbcOperations jdbcOperations;

    @PostMapping(consumes = {MediaType.APPLICATION_JSON_VALUE}) //create order by Customer
    public ResponseEntity<Order> createOrder(@RequestBody OrderProperties orderProps) {
        if(orderProps.getAmount() == 0) return ResponseEntity.notFound().build();
        String ageminQuery = orderProps.getAgemin() == null? "" : " AND ((SELECT now()) - birthdate) >= interval '" +
                orderProps.getAgemin() +" year' ";
        String agemaxQuery = orderProps.getAgemax() == null? "" : " AND ((SELECT now()) - birthdate) <= interval '" +
                orderProps.getAgemax() +" year' ";
        String cityQuery = orderProps.getCity() == null? "" : " AND city = '" + orderProps.getCity().toUpperCase() + "'";
        String genderQuery = orderProps.getGender() == null? "" : " AND gender = '" + orderProps.getGender().toUpperCase() + "'";
        Order orderResult = new Order();
        orderResult.setOrderProperties(orderProps);
        Integer count = jdbcOperations.queryForObject(
                "SELECT count(id) FROM testers WHERE status = 'FREE'"+
                        ageminQuery + agemaxQuery + cityQuery + genderQuery + ";"
                , (resultSet, i) ->
                        Integer.valueOf(resultSet.getInt("count" )));
        if (count<orderProps.getAmount()) return ResponseEntity.notFound().eTag("Testers not found").build();

        orderResult.setStatus("CREATED");
        orderResult.setPrice(BigDecimal.valueOf(orderProps.getAmount() * 38));
        orderResult.setKey(UUID.randomUUID());
        jdbcOperations.update("INSERT INTO orders " +
                        "(status, key, price, amount, agemin, agemax, city, gender) " +
                        "VALUES( ?, ?, ?, ?, ?, ?, ?, ?);",
                orderResult.getStatus()
                ,orderResult.getKey()
                ,orderResult.getPrice()
                ,orderProps.getAmount()
                ,orderProps.getAgemin()
                ,orderProps.getAgemax()
                ,orderProps.getCity()
                ,orderProps.getGender());
        orderResult.setOrderProperties(orderProps);

        //update testers to busy
        int orderID = jdbcOperations.queryForObject(
                "SELECT id FROM orders WHERE key = ? LIMIT 1;", (resultSet, i) ->
                        Integer.valueOf(resultSet.getInt("id" )), orderResult.getKey());
        jdbcOperations.update("UPDATE testers SET status='BUSY', orderid = "+orderID+
                " WHERE id IN (SELECT id FROM testers WHERE status = 'FREE'"
                + ageminQuery + agemaxQuery + cityQuery + genderQuery + " LIMIT " + orderProps.getAmount() + ");");
        return ResponseEntity.ok(orderResult);
    }

    //change order by admin
//    @PutMapping(consumes = {MediaType.APPLICATION_JSON_VALUE})
//    public ResponseEntity<Order> changeOrder(@RequestBody Order order, @RequestParam(name = "admin") UUID adminKey) {
//        if (adminKey != UUID.fromString("81d73f81-ba5e-4c49-bff4-12b720f4bed0"))
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
//        jdbcOperations.update("UPDATE orders SET" +
//                        " status=?, price=?, amount=?, agemin=?, agemax=?, city=?, gender=? WHERE key = ?;",
//                 order.getStatus()
//                ,order.getPrice()
//                ,order.getOrderProperties().getAmount()
//                ,order.getOrderProperties().getAgemin()
//                ,order.getOrderProperties().getAgemax()
//                ,order.getOrderProperties().getCity()
//                ,order.getOrderProperties().getGender()
//                ,order.getKey());
//        return ResponseEntity.ok( order);
//    }

    @GetMapping("{orderId}")  //return status of pay
    public ResponseEntity<Order> infoOrder(@PathVariable UUID orderId) {
        try {
            return ResponseEntity.ok(jdbcOperations.queryForObject("SELECT * FROM orders WHERE key = ? LIMIT 1;",
                    (resultSet, i) ->
                            new Order(
                                    resultSet.getString("status"),
                                    UUID.fromString(resultSet.getString("key")),
                                    resultSet.getBigDecimal("price"),
                                    new OrderProperties(
                                            Integer.valueOf(resultSet.getInt("amount")),
                                            Integer.valueOf(resultSet.getInt("agemin")),
                                            Integer.valueOf(resultSet.getInt("agemax")),
                                            resultSet.getString("city"),
                                            resultSet.getString("gender")
                                    )
                            )
                    , orderId));
        } catch (IncorrectResultSizeDataAccessException e) {
            return ResponseEntity.notFound().build();
        }
    }



    @PostMapping("{orderKey}/payments")
    public ResponseEntity<Void> createPayment(@PathVariable UUID orderKey
            ,@RequestBody Payment payment) {
        if (!payment.getAdmin().equals(UUID.fromString("81d73f81-ba5e-4c49-bff4-12b720f4bed0")))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        Integer orderId = this.jdbcOperations.queryForObject(
                "SELECT id FROM orders WHERE key = ? LIMIT 1;"
                , (resultSet, i) ->
                        Integer.valueOf(resultSet.getInt("id" )), orderKey);

        jdbcOperations.update("INSERT INTO payments " +
                        "(date, sum, customer, orderid) " +
                        "VALUES( now(), ?, ?, ?);",
                payment.getProperties().getSum(),
                payment.getProperties().getCustomer(),
                orderId);
        return ResponseEntity.created(URI.create("")).build();
    }

    @GetMapping("{orderKey}/payments")
    public ResponseEntity<List<PaymentProperties>> getOrderPayments(@PathVariable UUID orderKey) {
        Integer orderId = this.jdbcOperations.queryForObject(
                "SELECT id FROM orders WHERE key = ? LIMIT 1;"
                , (resultSet, i) ->
                        Integer.valueOf(resultSet.getInt("id" )), orderKey);

        return ResponseEntity.ok(jdbcOperations.query("SELECT * FROM payments WHERE orderid = ?", (resultSet, i) ->
                new PaymentProperties(
                        resultSet.getBigDecimal("sum"),
                        resultSet.getString("customer")
                ), orderId));
    }

    @GetMapping("{orderKey}/testers")
    public ResponseEntity<List<Tester>> getOrderTesters(@PathVariable UUID orderKey) {
        Integer orderId = jdbcOperations.queryForObject(
                "SELECT id FROM orders WHERE key = ? LIMIT 1;"
                , (resultSet, i) ->
                        Integer.valueOf(resultSet.getInt("id" )), orderKey);

        int countPayments = jdbcOperations.queryForObject(
                "SELECT count(*) FROM payments WHERE orderid = ?"
                , (resultSet, i) ->
                        Integer.valueOf(resultSet.getInt("count" )), orderId);
        if (countPayments < 1) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(jdbcOperations.query("SELECT * FROM testers WHERE orderid = ?", (resultSet, i) ->
                new Tester(Integer.valueOf(resultSet.getInt("id")),
                        resultSet.getString("email"),
                        resultSet.getString("phone"),
                        resultSet.getString("status"),
                        orderKey,
                        resultSet.getTimestamp("birthdate"),
                        resultSet.getString("city"),
                        resultSet.getString("gender")
                ), orderId));
    }
}
