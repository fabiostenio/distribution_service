package com.flowpay.distribution_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class DistributionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DistributionServiceApplication.class, args);
	}

}
