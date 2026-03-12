package com.juridico.sistema_juridico;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class SistemaJuridicoApplication {

	public static void main(String[] args) {
		SpringApplication.run(SistemaJuridicoApplication.class, args);
	}

}
