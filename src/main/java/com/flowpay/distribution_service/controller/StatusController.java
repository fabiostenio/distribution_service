package com.flowpay.distribution_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/status")
public class StatusController {

    @GetMapping
    public String check() {
        return "Backend FlowPay está online e operando!";
    }
}