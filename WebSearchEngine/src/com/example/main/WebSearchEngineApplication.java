package com.example.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

@SpringBootApplication
public class WebSearchEngineApplication extends SpringBootServletInitializer{

	public static void main(String[] args) {
		SpringApplication.run(WebSearchEngineApplication.class, args);
	}

}
