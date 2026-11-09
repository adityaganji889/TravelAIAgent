package com.travelai.agent.backend.travelaiagentbackend.config;

import java.util.concurrent.Executor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.travelai.agent.backend.travelaiagentbackend.tools.FlightTools;
import com.travelai.agent.backend.travelaiagentbackend.tools.HotelTools;
import com.travelai.agent.backend.travelaiagentbackend.tools.WeatherTools;

@Configuration
public class AIConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory
                .builder()
                .maxMessages(10)
                .build();
    }

    @Bean
     public ChatClient chatClient(
            ChatClient.Builder builder,
            ChatMemory chatMemory,
            FlightTools flightTools,
            HotelTools hotelTools,
            WeatherTools weatherTools) {
        return builder
                .defaultSystem("""
                        You are an intelligent Travel AI Agent.

                        Your responsibilities are:
                        1. Search flights using the available flight tools.
                        2. Search hotels using the available hotel tools.
                        3. Retrieve weather information using the available weather tools.
                        4. Help users plan their trips based on their preferences and budget.

                        IMPORTANT RULES:

                        - When a user requests flight information, use the flight search tool.
                        - When a user requests hotel information, use the hotel search tool.
                        - When a user requests weather information, use the weather tool.
                        - When a user requests a complete travel plan, use all relevant tools.
                        - Never fabricate flight prices, hotel prices, availability, or weather information.
                        - If a tool fails, clearly explain which information could not be retrieved.
                        - Do not tell users to manually visit Google Flights, Booking.com, or other websites
                          as a substitute for executing available tools.
                        - Ask the user for missing essential travel information when necessary.
                        - Present retrieved information in a clear, structured format.
                        """)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(
                        flightTools, hotelTools, weatherTools)
                .build();
    }

    @Bean(name = "chatExecutor")
    public Executor chatExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(20);
        executor.setThreadNamePrefix("chat-");

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        executor.initialize();

        return executor;
    }

}
