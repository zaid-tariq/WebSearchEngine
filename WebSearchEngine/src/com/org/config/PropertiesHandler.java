package com.org.config;

import java.io.IOException;
import java.util.Properties;

public class PropertiesHandler {
	
	public PropertiesHandler() throws IOException {
		Properties props = new Properties();
//		props.load( ClassLoader.getSystemResourceAsStream("config.properties"));
		getClass().getClassLoader().getResourceAsStream("config.properties");
	}

}
