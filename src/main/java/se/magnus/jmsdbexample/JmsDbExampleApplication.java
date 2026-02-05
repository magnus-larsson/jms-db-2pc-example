package se.magnus.jmsdbexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class JmsDbExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(JmsDbExampleApplication.class, args);
	}

}
