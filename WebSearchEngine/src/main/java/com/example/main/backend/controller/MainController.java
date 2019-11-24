package com.example.main.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MainController {

	@RequestMapping("/")
	public String helloWorld(Model model) {

		String message = "Main Controller";
		model.addAttribute("message", message);
		return "index";
	}
	
	@RequestMapping("/welcome")
	public String welcome(Model model) {
		//TODO: Implement front end

		String message = "Main ZAID";
		model.addAttribute("message", message);
		return "welcome";
	}

}
