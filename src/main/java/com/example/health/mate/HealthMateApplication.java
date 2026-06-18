package com.example.health.mate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HealthMateApplication {

	public static void main(String[] args) {
		SpringApplication.run(HealthMateApplication.class, args);
	}

}
