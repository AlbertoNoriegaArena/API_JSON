package com.ejemploAPI;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.PropertySource;


@SpringBootApplication
@PropertySource("classpath:dataSource.properties")
public class EjemploApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(EjemploApiApplication.class, args);
	}

}
