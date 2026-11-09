package com.travelai.agent.backend.travelaiagentbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TravelaiagentbackendApplication {

	public static void main(String[] args) {

		System.out.println(
        "OPENAI KEY PRESENT: " +
        (System.getenv("OPENAI_API_KEY") != null)
        );

		System.out.println(
        "SERPAI KEY PRESENT: " +
        (System.getenv("SERPAPI_API_KEY") != null)
        );

		SpringApplication.run(TravelaiagentbackendApplication.class, args);
	}

}
