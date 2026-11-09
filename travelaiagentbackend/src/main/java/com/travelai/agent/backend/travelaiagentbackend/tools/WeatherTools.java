
package com.travelai.agent.backend.travelaiagentbackend.tools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

// import com.fasterxml.jackson.databind.JsonNode;
import tools.jackson.databind.JsonNode;

@Component
public class WeatherTools {

    private static final Logger logger =
            LoggerFactory.getLogger(WeatherTools.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String API_URL;

    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    private static final DateTimeFormatter API_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE;

    public WeatherTools(
            RestTemplate restTemplate,
            @Value("${app.serpapi.api.key}") String apiKey,
            @Value("${app.serpapi.api.url}") String API_URL) {

        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.API_URL = API_URL;
    }

    // Validate city
    private String validateCity(String city) {

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException(
                    "Please provide a valid city name.");
        }

        return city.trim();
    }

    // Convert DD-MMM-YYYY to YYYY-MM-DD
    private String convertDate(String date) {

        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException(
                    "Forecast date cannot be empty.");
        }

        try {

            return LocalDate.parse(
                    date.trim(), INPUT_FORMAT)
                    .format(API_FORMAT);

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    "Invalid date: " + date
                    + ". Please use DD-MMM-YYYY format, e.g. 15-Nov-2026.");
        }
    }

    // Extract useful weather information from SerpAPI response
    private String extractWeatherData(JsonNode response) {

        // Check direct weather object
        JsonNode weather = response.path("weather");

        if (!weather.isMissingNode()
                && !weather.isNull()
                && !weather.isEmpty()) {

            return weather.toPrettyString();
        }

        // Check answer box
        JsonNode answerBox = response.path("answer_box");

        if (!answerBox.isMissingNode()
                && !answerBox.isNull()
                && !answerBox.isEmpty()) {

            return answerBox.toPrettyString();
        }

        // Check knowledge graph
        JsonNode knowledgeGraph =
                response.path("knowledge_graph");

        if (!knowledgeGraph.isMissingNode()
                && !knowledgeGraph.isNull()
                && !knowledgeGraph.isEmpty()) {

            return knowledgeGraph.toPrettyString();
        }

        // Check organic search results
        JsonNode organicResults =
                response.path("organic_results");

        if (organicResults.isArray()
                && !organicResults.isEmpty()) {

            StringBuilder result = new StringBuilder();

            for (JsonNode item : organicResults) {

                @SuppressWarnings("deprecation")
                String title = item.path("title")
                        .asText("");

                @SuppressWarnings("deprecation")
                String snippet = item.path("snippet")
                        .asText("");

                if (!title.isBlank()) {
                    result.append("\nTitle: ")
                            .append(title);
                }

                if (!snippet.isBlank()) {
                    result.append("\nInformation: ")
                            .append(snippet);
                }

                result.append("\n");
            }

            if (!result.isEmpty()) {
                return result.toString();
            }
        }

        return null;
    }

    // Current Weather
    @Tool(
        name = "getCurrentWeather",
        description = """
            Get current weather information for a city using SerpAPI Google Search.

            Parameters:
            - city: City name, e.g. Mumbai, Delhi, Bangalore.

            Returns weather information when available:
            - Temperature.
            - Weather conditions.
            - Humidity.
            - Wind speed.
            - Other available weather details.

            IMPORTANT:
            - Always use the city provided by the user.
            - Never fabricate temperature or weather conditions.
            - If weather information is unavailable, clearly mention it.
            """
    )
    public String getCurrentWeather(String city) {

        try {

            city = validateCity(city);

            logger.info(
                    "Current weather search started for: {}",
                    city);

            // Build SerpAPI request
            String url = UriComponentsBuilder
                    .fromUriString(API_URL)
                    .queryParam("engine", "google")
                    .queryParam(
                            "q",
                            "current weather in " + city)
                    .queryParam("api_key", apiKey)
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .build()
                    .encode()
                    .toUriString();

            logger.info(
                    "Sending current weather request to SerpAPI.");

            // Execute API request
            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {

                logger.warn(
                        "Weather API returned an empty response for {}",
                        city);

                return "No response received from weather API.";
            }

            // Check API error
            if (response.has("error")) {

                @SuppressWarnings("deprecation")
                String error = response.path("error").asText();

                logger.error(
                        "Weather API error for {}: {}",
                        city, error);

                return "Weather API error: " + error;
            }

            // Extract weather information
            String weatherData = extractWeatherData(response);

            if (weatherData == null) {

                logger.warn(
                        "Weather information not available for {}",
                        city);

                return "Weather information not available for "
                        + city;
            }

            logger.info(
                    "Current weather retrieved successfully for {}",
                    city);

            return "CURRENT WEATHER FOR " + city
                    + "\n\n" + weatherData;

        } catch (HttpStatusCodeException e) {

            logger.error(
                    "Weather API HTTP error. Status: {}, Response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            return "Weather API request failed with HTTP status "
                    + e.getStatusCode()
                    + ". Please check your API configuration.";

        } catch (RestClientException e) {

            logger.error(
                    "Weather API connection error",
                    e);

            return "Unable to connect to the weather API. "
                    + "Please check your internet connection or API availability.";

        } catch (Exception e) {

            logger.error(
                    "Unexpected current weather error",
                    e);

            return "Unable to get current weather: "
                    + e.getMessage();
        }
    }

    // Weather Forecast
    @Tool(
        name = "getWeatherForecast",
        description = """
            Get weather forecast information for a city on a specific date
            using SerpAPI Google Search.

            Parameters:
            - city: City name, e.g. Mumbai, Delhi.
            - date: Forecast date in DD-MMM-YYYY format, e.g. 15-Nov-2026.

            Returns available forecast information:
            - Expected temperature.
            - Weather conditions.
            - Rain information when available.
            - Other forecast details.

            IMPORTANT:
            - Always use the requested city and date.
            - Never fabricate forecast information.
            - Forecast data may not be available for dates outside
              the provider's forecast range.
            - Clearly explain when forecast data is unavailable.
            """
    )
    public String getWeatherForecast(
            String city,
            String date) {

        try {

            city = validateCity(city);

            // Validate and convert forecast date
            String formattedDate = convertDate(date);

            LocalDate forecastDate = LocalDate.parse(
                    formattedDate, API_FORMAT);

            logger.info(
                    "Weather forecast search started: {}, date: {}",
                    city, formattedDate);

            // Build SerpAPI request
            String url = UriComponentsBuilder
                    .fromUriString(API_URL)
                    .queryParam("engine", "google")
                    .queryParam(
                            "q",
                            "weather forecast in " + city
                                    + " on " + formattedDate)
                    .queryParam("api_key", apiKey)
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .build()
                    .encode()
                    .toUriString();

            logger.info(
                    "Sending weather forecast request to SerpAPI.");

            // Execute API request
            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {

                logger.warn(
                        "Weather forecast API returned empty response.");

                return "No response received from weather forecast API.";
            }

            // Check API error
            if (response.has("error")) {

                @SuppressWarnings("deprecation")
                String error = response.path("error").asText();

                logger.error(
                        "Weather forecast API error: {}",
                        error);

                return "Weather API error: " + error;
            }

            // Extract forecast information
            String forecastData = extractWeatherData(response);

            if (forecastData == null) {

                logger.warn(
                        "Forecast unavailable for {} on {}",
                        city, formattedDate);

                return "Weather forecast not available for "
                        + city + " on " + date
                        + ". The requested date may be outside the available forecast range.";
            }

            logger.info(
                    "Weather forecast retrieved successfully for {} on {}",
                    city, formattedDate);

            return "WEATHER FORECAST"
                    + "\nCity: " + city
                    + "\nDate: " + date
                    + "\n\n" + forecastData;

        } catch (HttpStatusCodeException e) {

            logger.error(
                    "Weather forecast HTTP error. Status: {}, Response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            return "Weather forecast request failed with HTTP status "
                    + e.getStatusCode()
                    + ". Please check your API configuration.";

        } catch (RestClientException e) {

            logger.error(
                    "Weather forecast connection error",
                    e);

            return "Unable to connect to the weather forecast API. "
                    + "Please check your internet connection or API availability.";

        } catch (Exception e) {

            logger.error(
                    "Unexpected weather forecast error",
                    e);

            return "Unable to get weather forecast: "
                    + e.getMessage();
        }
    }
}
