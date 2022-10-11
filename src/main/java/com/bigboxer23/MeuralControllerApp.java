package com.bigboxer23;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 *
 */
@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(info =
@Info(title = "Meural remote control", version = "1", description = "Provides ability to push content to a Meural display from an external URL",
		contact = @Contact(name = "bigboxer23@gmail.com", url="https://github.com/bigboxer23/meural-control"))
)
public class MeuralControllerApp
{
	public static void main(String[] args)
	{
		SpringApplication.run(MeuralControllerApp.class, args);
	}
}
