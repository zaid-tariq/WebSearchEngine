package com.example.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.main.backend.config.PerClientRateLimitInterceptor;


@SpringBootApplication
public class WebSearchEngineApplication extends SpringBootServletInitializer implements WebMvcConfigurer{

	public static void main(String[] args) {
		System.out.println("#### main ####");
		SpringApplication.run(WebSearchEngineApplication.class, args);
	}
	
	 @Override
	  public void addInterceptors(InterceptorRegistry registry) {
		 System.out.println("#### addInterceptors ####");
		 registry.addInterceptor(new PerClientRateLimitInterceptor()).addPathPatterns("*");
	 }
}
