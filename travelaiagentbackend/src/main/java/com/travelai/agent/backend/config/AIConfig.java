package com.travelai.agent.backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.travelai.agent.backend.tools.FlightTools;
import com.travelai.agent.backend.tools.HotelTools;
import com.travelai.agent.backend.tools.WeatherTools;

import lombok.RequiredArgsConstructor;

@Configuration 
@RequiredArgsConstructor 
public class AIConfig {

    private final FlightTools flightTools;
    private final HotelTools hotelTools;
    private final WeatherTools weatherTools;

     @Bean
     public ChatMemory chatMemory() {
                return MessageWindowChatMemory
                                .builder()
                                .maxMessages(10)
                                .build();
     }
    
    @Bean 
    public ChatClient chatClient(ChatClient.Builder builder){
        return builder
               .defaultSystem("""       
                                             
                     You are a helpful travel planning and online store customer support assistant. Use the available tools to help users find flights, hotels, and weather information. When users provide a budget, recommend suitable options within their budget and briefly explain why each option is a good fit.
                                                                                   """)
               .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory()).build())
               .defaultTools(
                 flightTools,hotelTools,weatherTools
               )
               .build();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
