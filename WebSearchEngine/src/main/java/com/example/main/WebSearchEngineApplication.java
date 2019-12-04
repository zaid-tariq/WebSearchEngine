package com.example.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.main.backend.config.PerClientRateLimitInterceptor;


@SpringBootApplication
@Configuration
public class WebSearchEngineApplication extends SpringBootServletInitializer implements WebMvcConfigurer{

	public static void main(String[] args) {
		SpringApplication.run(WebSearchEngineApplication.class, args);
	}
	
	 @Override
	  public void addInterceptors(InterceptorRegistry registry) {
		 registry.addInterceptor(new PerClientRateLimitInterceptor());
	 }
}
