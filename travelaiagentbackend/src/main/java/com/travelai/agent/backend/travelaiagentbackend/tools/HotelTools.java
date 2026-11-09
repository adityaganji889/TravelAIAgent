
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
public class HotelTools {

    private static final Logger logger =
            LoggerFactory.getLogger(HotelTools.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;

    // Input format: 11-Nov-2026
    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    // SerpAPI format: 2026-11-11
    private static final DateTimeFormatter API_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE;

    public HotelTools(
            RestTemplate restTemplate,
            @Value("${app.serpapi.api.key}") String apiKey,
            @Value("${app.serpapi.api.url}") String apiUrl) {

        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    // Convert DD-MMM-YYYY to YYYY-MM-DD
    private String convertDate(String date) {

        if (date == null || date.isBlank()) {
            throw new IllegalArgumentException(
                    "Date cannot be empty.");
        }

        try {

            return LocalDate.parse(
                    date.trim(), INPUT_FORMAT)
                    .format(API_FORMAT);

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    "Invalid date: " + date
                    + ". Please use DD-MMM-YYYY format, e.g. 11-Nov-2026.");
        }
    }

    // Search Hotels
    @SuppressWarnings("deprecation")
@Tool(
        name = "searchHotels",
        description = """
            Search available hotels and room rates using SerpAPI Google Hotels.

            Parameters:
            - location: City or destination, e.g. New Delhi.
            - checkInDate: Check-in date in DD-MMM-YYYY format.
            - checkOutDate: Check-out date in DD-MMM-YYYY format.
            - adults: Number of adult guests.

            Returns:
            - Hotel names and addresses.
            - Hotel ratings.
            - Price per night.
            - Total stay price.
            - Booking providers and booking links when available.
            - Amenities when available.

            IMPORTANT:
            - Always use the exact dates requested by the user.
            - Always use the requested number of adults.
            - Never fabricate hotel prices or availability.
            - Do not claim a hotel is available unless the API returns it.
            """
    )
    public String searchHotels(
            String location,
            String checkInDate,
            String checkOutDate,
            int adults) {

        try {

            logger.info(
                    "Hotel search started: {}, check-in: {}, check-out: {}, adults: {}",
                    location, checkInDate, checkOutDate, adults);

            // Validate location
            if (location == null || location.isBlank()) {
                return "Please provide a valid hotel location.";
            }

            // Validate adults
            if (adults < 1) {
                return "At least one adult is required.";
            }

            // Convert dates
            String formattedCheckIn =
                    convertDate(checkInDate);

            String formattedCheckOut =
                    convertDate(checkOutDate);

            LocalDate checkIn = LocalDate.parse(
                    formattedCheckIn, API_FORMAT);

            LocalDate checkOut = LocalDate.parse(
                    formattedCheckOut, API_FORMAT);

            // Validate date range
            if (!checkOut.isAfter(checkIn)) {
                return "Check-out date must be after check-in date.";
            }

            // Validate past dates
            if (checkIn.isBefore(LocalDate.now())) {
                return "Check-in date cannot be in the past.";
            }

            // Calculate number of nights
            long nights = java.time.temporal.ChronoUnit.DAYS
                    .between(checkIn, checkOut);

            logger.info(
                    "Hotel stay duration: {} nights",
                    nights);

            // Build SerpAPI request
            String url = UriComponentsBuilder
                    .fromUriString(apiUrl)
                    .queryParam("engine", "google_hotels")
                    .queryParam("q", location.trim())
                    .queryParam("check_in_date", formattedCheckIn)
                    .queryParam("check_out_date", formattedCheckOut)
                    .queryParam("adults", adults)
                    .queryParam("currency", "INR")
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .queryParam("sort_by", 3)
                    .queryParam("api_key", apiKey)
                    .build()
                    .encode()
                    .toUriString();

            logger.info("Sending hotel search request to SerpAPI.");

            // Execute API request
            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {

                logger.warn("Hotel API returned an empty response.");

                return "No response received from hotel API.";
            }

            // Check API error
            if (response.has("error")) {

                @SuppressWarnings("deprecation")
                String error = response.path("error").asText();

                logger.error("Hotel API error: {}", error);

                return "Hotel API error: " + error;
            }

            // Check search metadata
            JsonNode metadata = response.path("search_metadata");

            @SuppressWarnings("deprecation")
            String status = metadata.path("status").asText("");

            if ("Error".equalsIgnoreCase(status)) {

                @SuppressWarnings("deprecation")
                String error = response.path("error")
                        .asText("Unknown hotel search error.");

                logger.error("Hotel search failed: {}", error);

                return "Hotel search failed: " + error;
            }

            // Extract hotel properties
            JsonNode properties = response.path("properties");

            if (!properties.isArray() || properties.isEmpty()) {

                logger.warn(
                        "No hotel results found for location: {}",
                        location);

                return "No hotel results found for "
                        + location
                        + " for the selected dates.";
            }

            // Build results
            StringBuilder result = new StringBuilder();

            result.append("HOTEL SEARCH RESULTS")
                    .append("\nLocation: ").append(location)
                    .append("\nCheck-in: ").append(checkInDate)
                    .append("\nCheck-out: ").append(checkOutDate)
                    .append("\nNumber of nights: ").append(nights)
                    .append("\nAdults: ").append(adults)
                    .append("\n\n");

            int count = 1;

            for (JsonNode hotel : properties) {

                @SuppressWarnings("deprecation")
                String name = hotel.path("name")
                        .asText("N/A");

                @SuppressWarnings("deprecation")
                String address = hotel.path("address")
                        .asText("N/A");

                @SuppressWarnings("deprecation")
                String rating = hotel.path("overall_rating")
                        .asText("N/A");

                @SuppressWarnings("deprecation")
                String propertyToken = hotel.path("property_token")
                        .asText("N/A");

                // Extract prices
                JsonNode nightlyRate = hotel.path("rate_per_night");

                JsonNode totalRate = hotel.path("total_rate");

                String pricePerNight = getPrice(nightlyRate);

                String totalPrice = getPrice(totalRate);

                result.append("HOTEL ").append(count++)
                        .append("\nName: ").append(name)
                        .append("\nAddress: ").append(address)
                        .append("\nRating: ").append(rating)
                        .append("\nPrice per night: ").append(pricePerNight)
                        .append("\nTotal stay price: ").append(totalPrice);

                // Property token
                if (!"N/A".equals(propertyToken)) {
                    result.append("\nProperty Token: ")
                            .append(propertyToken);
                }

                // Booking options
                JsonNode prices = hotel.path("prices");

                if (prices.isArray() && !prices.isEmpty()) {

                    result.append("\nBooking Options:");

                    for (JsonNode option : prices) {

                        @SuppressWarnings("deprecation")
                        String provider = option.path("source")
                                .asText("N/A");

                        String bookingPrice = getPrice(
                                option.path("total_rate"));

                        @SuppressWarnings("deprecation")
                        String bookingLink = option.path("link")
                                .asText("");

                        result.append("\n- Provider: ")
                                .append(provider);

                        result.append(", Price: ")
                                .append(bookingPrice);

                        if (!bookingLink.isBlank()) {
                            result.append(", Booking Link: ")
                                    .append(bookingLink);
                        }
                    }
                }

                // Amenities
                JsonNode amenities = hotel.path("amenities");

                if (amenities.isArray() && !amenities.isEmpty()) {

                    result.append("\nAmenities:");

                    for (JsonNode amenity : amenities) {

                        if (amenity != null && !amenity.isNull()) {
                            result.append("\n- ")
                                    .append(amenity.asText());
                        }
                    }
                }

                result.append("\n\n");
            }

            logger.info(
                    "Hotel search successful. Found {} properties for {}.",
                    properties.size(), location);

            return result.toString();

        } catch (HttpStatusCodeException e) {

            logger.error(
                    "Hotel API HTTP error. Status: {}, Response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            return "Hotel API request failed with HTTP status "
                    + e.getStatusCode()
                    + ". Please check your API key and request parameters.";

        } catch (RestClientException e) {

            logger.error("Hotel API connection error", e);

            return "Unable to connect to the hotel API. "
                    + "Please check your internet connection or API availability.";

        } catch (Exception e) {

            logger.error("Unexpected hotel search error", e);

            return "Unable to search hotels: " + e.getMessage();
        }
    }

    // Extract hotel price
    @SuppressWarnings("deprecation")
private String getPrice(JsonNode rate) {

        if (rate == null || rate.isMissingNode() || rate.isNull()) {
            return "Price unavailable";
        }

        // Prefer lowest price
        @SuppressWarnings("deprecation")
        String price = rate.path("lowest").asText("");

        if (!price.isBlank()) {
            return price;
        }

        // Fallback to display price
        price = rate.path("display_price").asText("");

        if (!price.isBlank()) {
            return price;
        }

        return "Price unavailable";
    }
}
