package com.it342.projectmanagementsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = {"com.it342.projectmanagementsystem"})
@SpringBootApplication
public class ProjectmanagementsystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProjectmanagementsystemApplication.class, args);
	}

}
//test