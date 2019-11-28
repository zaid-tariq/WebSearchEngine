package com.example.main.backend.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.core.io.ClassPathResource;

public class Utils {
	
	public static File createTempFileFromInputStream(String a_resourceName) throws IOException {
		
		File tempFile = File.createTempFile(a_resourceName, null);
		tempFile.deleteOnExit();
		FileOutputStream out = new FileOutputStream(tempFile);
		IOUtils.copy( new ClassPathResource(a_resourceName).getInputStream(), out);
		return tempFile;
	}

}
