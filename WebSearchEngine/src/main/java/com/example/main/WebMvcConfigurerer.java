package com.example.main;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.example.main.backend.config.PerClientRateLimitInterceptor;


@SpringBootApplication
@Configuration
public class WebMvcConfigurerer implements WebMvcConfigurer {
		
	 @Override
	  public void addInterceptors(InterceptorRegistry registry) {
		 System.out.println("###addInterceptors###");
		 registry.addInterceptor(new PerClientRateLimitInterceptor());
	 }
}