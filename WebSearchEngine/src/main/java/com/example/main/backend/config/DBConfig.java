package com.example.main.backend.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class DBConfig {
	
	private String url;
	
	private String username;
	
	private String password;
	
	public DBConfig() {
		
		Properties props = new Properties();
		try {
			props.load( new FileInputStream(new ClassPathResource("application.properties").getFile()));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.url = props.getProperty("spring.datasource.url");
		this.username = props.getProperty("spring.datasource.username");
		this.password = props.getProperty("spring.datasource.password");
		System.out.println(this.url);
		System.out.println(this.username);
		System.out.println(this.password);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
