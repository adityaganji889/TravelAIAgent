
package com.travelai.agent.backend.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class WeatherTools {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String API_URL;

    public WeatherTools(
            RestTemplate restTemplate,
            @Value("${app.serpapi.api.key}") String apiKey,
            @Value("${app.serpapi.api.url}") String API_URL) {

        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.API_URL = API_URL;
    }

    // Current Weather
    @Tool(
        name = "getCurrentWeather",
        description = """
            Get current weather information for a city.

            Parameters:
            - city: City name (e.g. Mumbai, Delhi, Bangalore).

            Returns temperature, weather conditions,
            humidity, and wind information when available.
            """
    )
    public String getCurrentWeather(String city) {

        try {

            String url = UriComponentsBuilder
                    .fromUriString(API_URL)
                    .queryParam("engine", "google")
                    .queryParam("q", "current weather in " + city)
                    .queryParam("api_key", apiKey)
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .build()
                    .encode()
                    .toUriString();

            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {
                return "No response received from weather API.";
            }

            if (response.has("error")) {
                return "Weather API error: "
                        + response.path("error").asText();
            }

            JsonNode weather = response.path("weather");

            if (weather.isMissingNode() || weather.isNull()) {
                JsonNode answerBox = response.path("answer_box");

                if (!answerBox.isMissingNode()
                        && !answerBox.isNull()) {
                    return answerBox.toPrettyString();
                }

                JsonNode knowledgeGraph =
                        response.path("knowledge_graph");

                if (!knowledgeGraph.isMissingNode()
                        && !knowledgeGraph.isNull()) {
                    return knowledgeGraph.toPrettyString();
                }

                return "Weather information not available for "
                        + city;
            }

            return weather.toPrettyString();

        } catch (Exception e) {

            return "Unable to get current weather: "
                    + e.getMessage();
        }
    }

    // Weather Forecast
    @Tool(
        name = "getWeatherForecast",
        description = """
            Get weather forecast information for a city.

            Parameters:
            - city: City name (e.g. Mumbai, Delhi).
            - date: Forecast date (e.g. 15-Nov-2026).

            Returns expected temperature, weather conditions,
            and forecast information when available.
            """
    )
    public String getWeatherForecast(
            String city,
            String date) {

        try {

            String url = UriComponentsBuilder
                    .fromUriString(API_URL)
                    .queryParam("engine", "google")
                    .queryParam(
                            "q",
                            "weather forecast in " + city
                                    + " on " + date)
                    .queryParam("api_key", apiKey)
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .build()
                    .encode()
                    .toUriString();

            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {
                return "No response received from weather API.";
            }

            if (response.has("error")) {
                return "Weather API error: "
                        + response.path("error").asText();
            }

            JsonNode answerBox = response.path("answer_box");

            if (!answerBox.isMissingNode()
                    && !answerBox.isNull()) {
                return answerBox.toPrettyString();
            }

            JsonNode knowledgeGraph =
                    response.path("knowledge_graph");

            if (!knowledgeGraph.isMissingNode()
                    && !knowledgeGraph.isNull()) {
                return knowledgeGraph.toPrettyString();
            }

            JsonNode organicResults =
                    response.path("organic_results");

            if (organicResults.isArray()
                    && !organicResults.isEmpty()) {
                return organicResults.toPrettyString();
            }

            return "Weather forecast not available for "
                    + city + " on " + date;

        } catch (Exception e) {

            return "Unable to get weather forecast: "
                    + e.getMessage();
        }
    }
}
