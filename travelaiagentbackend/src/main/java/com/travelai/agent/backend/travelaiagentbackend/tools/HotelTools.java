
package com.travelai.agent.backend.travelaiagentbackend.tools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class HotelTools {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String apiUrl;

    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    public HotelTools(
            RestTemplate restTemplate,
            @Value("${app.serpapi.api.key}") String apiKey,
            @Value("${app.serpapi.api.url}") String apiUrl) {

        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    private String convertDate(String date) {
        return LocalDate.parse(date, INPUT_FORMAT)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    @Tool(
        name = "searchHotels",
        description = """
            Search hotels and room rates for a particular destination.

            Parameters:
            - location: City or destination.
            - checkInDate: DD-MMM-YYYY format.
            - checkOutDate: DD-MMM-YYYY format.
            - adults: Number of adult guests.

            Return hotel names, ratings, nightly prices, total stay
            prices, addresses, room availability information and
            booking links when provided.

            Always use the requested dates and number of adults.
            """
    )
    public String searchHotels(
            String location,
            String checkInDate,
            String checkOutDate,
            int adults) {

        try {

            LocalDate checkIn = LocalDate.parse(
                    checkInDate, INPUT_FORMAT);

            LocalDate checkOut = LocalDate.parse(
                    checkOutDate, INPUT_FORMAT);

            if (!checkOut.isAfter(checkIn)) {
                return "Check-out date must be after check-in date.";
            }

            if (adults < 1) {
                return "At least one adult is required.";
            }

            String url = UriComponentsBuilder
                    .fromUriString(apiUrl)
                    .queryParam("engine", "google_hotels")
                    .queryParam("q", location)
                    .queryParam("check_in_date", convertDate(checkInDate))
                    .queryParam("check_out_date", convertDate(checkOutDate))
                    .queryParam("adults", adults)
                    .queryParam("currency", "INR")
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .queryParam("sort_by", 3)
                    .queryParam("api_key", apiKey)
                    .build()
                    .encode()
                    .toUriString();

            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {
                return "No response received from hotel API.";
            }

            if (response.has("error")) {
                return "Hotel API error: "
                        + response.path("error").asText();
            }

            JsonNode properties = response.path("properties");

            if (!properties.isArray() || properties.isEmpty()) {
                return "No hotel results found for "
                        + location + " for the selected dates.";
            }

            StringBuilder result = new StringBuilder();

            result.append("HOTEL SEARCH RESULTS")
                    .append("\nLocation: ").append(location)
                    .append("\nCheck-in: ").append(checkInDate)
                    .append("\nCheck-out: ").append(checkOutDate)
                    .append("\nAdults: ").append(adults)
                    .append("\n\n");

            int count = 1;

            for (JsonNode hotel : properties) {

                String name = hotel.path("name").asText("N/A");

                String address = hotel.path("address")
                        .asText("N/A");

                String rating = hotel.path("overall_rating")
                        .asText("N/A");

                String propertyToken = hotel.path("property_token")
                        .asText("N/A");

                JsonNode nightlyRate = hotel.path("rate_per_night");
                JsonNode totalRate = hotel.path("total_rate");

                String pricePerNight = getPrice(nightlyRate);
                String totalPrice = getPrice(totalRate);

                result.append("Hotel ").append(count++)
                        .append("\nName: ").append(name)
                        .append("\nAddress: ").append(address)
                        .append("\nRating: ").append(rating)
                        .append("\nPrice per night: ").append(pricePerNight)
                        .append("\nTotal stay price: ").append(totalPrice)
                        .append("\nProperty Token: ").append(propertyToken);

                JsonNode prices = hotel.path("prices");

                if (prices.isArray() && !prices.isEmpty()) {

                    result.append("\nBooking Options:");

                    for (JsonNode option : prices) {

                        result.append("\n- Provider: ")
                                .append(option.path("source").asText("N/A"));

                        result.append(", Price: ")
                                .append(getPrice(option.path("total_rate")));

                        result.append(", Booking Link: ")
                                .append(option.path("link").asText("N/A"));
                    }
                }

                JsonNode amenities = hotel.path("amenities");

                if (amenities.isArray() && !amenities.isEmpty()) {

                    result.append("\nAmenities: ");

                    for (JsonNode amenity : amenities) {
                        result.append(amenity.asText()).append(", ");
                    }
                }

                result.append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {
            return "Unable to search hotels: " + e.getMessage();
        }
    }

    private String getPrice(JsonNode rate) {

        if (rate == null || rate.isMissingNode() || rate.isNull()) {
            return "Price unavailable";
        }

        String price = rate.path("lowest").asText("");

        if (!price.isBlank()) {
            return price;
        }

        price = rate.path("display_price").asText("");

        return price.isBlank() ? "Price unavailable" : price;
    }
}
