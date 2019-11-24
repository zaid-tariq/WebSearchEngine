package com.example.main.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

public class SpringBootInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(SpringBootInitializer.class);
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SpringBootInitializer.class, args);
	}
}
