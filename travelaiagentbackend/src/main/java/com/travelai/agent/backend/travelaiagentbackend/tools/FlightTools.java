
package com.travelai.agent.backend.travelaiagentbackend.tools;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

// import com.fasterxml.jackson.databind.JsonNode;
import tools.jackson.databind.JsonNode;

@Component
public class FlightTools {

    private static final Logger logger =
            LoggerFactory.getLogger(FlightTools.class);

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String API_URL;

    // Input date format: 11-Nov-2026
    private static final DateTimeFormatter INPUT_FORMAT =
            DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.ENGLISH);

    // SerpAPI date format: 2026-11-11
    private static final DateTimeFormatter API_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE;

    private static final Map<String, String> AIRPORT_CODES =
            Map.ofEntries(
                    Map.entry("mumbai", "BOM"),
                    Map.entry("delhi", "DEL"),
                    Map.entry("new delhi", "DEL"),
                    Map.entry("bangalore", "BLR"),
                    Map.entry("bengaluru", "BLR"),
                    Map.entry("goa", "GOI"),
                    Map.entry("hyderabad", "HYD"),
                    Map.entry("chennai", "MAA"),
                    Map.entry("kolkata", "CCU"),
                    Map.entry("pune", "PNQ"),
                    Map.entry("ahmedabad", "AMD"),
                    Map.entry("jaipur", "JAI"),
                    Map.entry("kochi", "COK"),
                    Map.entry("lucknow", "LKO"),
                    Map.entry("nagpur", "NAG")
            );

    public FlightTools(
            RestTemplate restTemplate,
            @Value("${app.serpapi.api.key}") String apiKey,
            @Value("${app.serpapi.api.url}") String API_URL) {

        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.API_URL = API_URL;
    }

    // Convert DD-MMM-YYYY to YYYY-MM-DD
    private String convertFlightDate(String date) {

        try {
            return LocalDate.parse(date.trim(), INPUT_FORMAT)
                    .format(API_FORMAT);

        } catch (DateTimeParseException e) {

            throw new IllegalArgumentException(
                    "Invalid date: " + date
                    + ". Please use DD-MMM-YYYY format, e.g. 11-Nov-2026.");
        }
    }

    // Get airport code from city name
    private String getAirportCode(String city) {

        if (city == null || city.isBlank()) {
            throw new IllegalArgumentException(
                    "Departure and destination cities are required.");
        }

        String normalized = city.trim().toLowerCase(Locale.ROOT);

        if (normalized.matches("[a-z]{3}")) {
            return normalized.toUpperCase(Locale.ROOT);
        }

        String airportCode = AIRPORT_CODES.get(normalized);

        if (airportCode == null) {
            throw new IllegalArgumentException(
                    "Airport code not available for city: " + city
                    + ". Please provide a supported city or a three-letter airport code.");
        }

        return airportCode;
    }

    // Flight Search
    @Tool(
        name = "searchFlight",
        description = """
            Search available flights using SerpAPI Google Flights.

            Parameters:
            - source: Departure city, e.g. Mumbai.
            - destination: Arrival city, e.g. New Delhi.
            - departureDate: Departure date in DD-MMM-YYYY format.
            - returnDate: Return date in DD-MMM-YYYY format.
              For a one-way flight, provide an empty string.

            IMPORTANT:
            - Use type=1 for round-trip flights when returnDate is provided.
            - Use type=2 for one-way flights when returnDate is empty.
            - Always provide dates in DD-MMM-YYYY format.
            - Never fabricate flight information or prices.
            """
    )
    public String searchFlight(
            String source,
            String destination,
            String departureDate,
            String returnDate) {

        try {

            logger.info(
                    "Flight search started: {} -> {}, departure: {}, return: {}",
                    source, destination, departureDate, returnDate);

            // Validate and convert airport codes
            String sourceCode = getAirportCode(source);
            String destinationCode = getAirportCode(destination);

            // Convert dates to SerpAPI format
            String formattedDepartureDate =
                    convertFlightDate(departureDate);

            boolean isRoundTrip =
                    returnDate != null && !returnDate.isBlank();

            String formattedReturnDate = null;

            if (isRoundTrip) {

                formattedReturnDate =
                        convertFlightDate(returnDate);

                LocalDate departure = LocalDate.parse(
                        formattedDepartureDate, API_FORMAT);

                LocalDate returning = LocalDate.parse(
                        formattedReturnDate, API_FORMAT);

                if (!returning.isAfter(departure)) {
                    return "Return date must be after departure date.";
                }
            }

            // Build API request
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromUriString(API_URL)
                    .queryParam("engine", "google_flights")
                    .queryParam("departure_id", sourceCode)
                    .queryParam("arrival_id", destinationCode)
                    .queryParam("type", isRoundTrip ? 1 : 2)
                    .queryParam("outbound_date", formattedDepartureDate)
                    .queryParam("currency", "INR")
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .queryParam("api_key", apiKey);

            if (isRoundTrip) {
                builder.queryParam(
                        "return_date", formattedReturnDate);
            }

            String url = builder
                    .build()
                    .encode()
                    .toUriString();

            logger.info("Sending flight request to SerpAPI");

            // Execute API request
            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {
                logger.warn("Flight API returned an empty response.");
                return "No response received from flight API.";
            }

            // Check API error
            if (response.has("error")) {

                String error = response.path("error").asText();

                logger.error("Flight API error: {}", error);

                return "Flight API error: " + error;
            }

            // Collect flights
            List<JsonNode> flights = new ArrayList<>();

            addFlights(response, "best_flights", flights);
            addFlights(response, "other_flights", flights);

            if (flights.isEmpty()) {

                logger.warn(
                        "No flights found for {} -> {} on {}",
                        source, destination, departureDate);

                return "No flights found from " + source
                        + " to " + destination
                        + " on " + departureDate;
            }

            // Format results
            StringBuilder result = new StringBuilder();

            result.append("FLIGHT SEARCH RESULTS")
                    .append("\nFrom: ").append(source)
                    .append("\nTo: ").append(destination)
                    .append("\nDeparture Date: ").append(departureDate);

            if (isRoundTrip) {
                result.append("\nReturn Date: ").append(returnDate);
            }

            result.append("\nTrip Type: ")
                    .append(isRoundTrip ? "Round Trip" : "One Way")
                    .append("\n\n");

            int count = 1;

            for (JsonNode flight : flights) {

                JsonNode segments = flight.path("flights");

                String airline = "Unknown";
                String flightNumber = "N/A";
                String departure = "N/A";
                String arrival = "N/A";

                if (segments.isArray() && !segments.isEmpty()) {

                    JsonNode first = segments.get(0);
                    JsonNode last = segments.get(segments.size() - 1);

                    airline = first.path("airline")
                            .asText("Unknown");

                    flightNumber = first.path("flight_number")
                            .asText("N/A");

                    departure = first.path("departure_airport")
                            .path("time")
                            .asText("N/A");

                    arrival = last.path("arrival_airport")
                            .path("time")
                            .asText("N/A");
                }

                // Price handling
                JsonNode priceNode = flight.path("price");

                String price = priceNode.isMissingNode()
                        || priceNode.isNull()
                        ? "Price unavailable"
                        : "INR " + priceNode.asText();

                // Duration handling
                int duration = flight.path("total_duration")
                        .asInt(0);

                String durationText = duration > 0
                        ? (duration / 60) + "h "
                                + (duration % 60) + "m"
                        : "Unavailable";

                // Stops
                int stops = segments.isArray()
                        ? Math.max(0, segments.size() - 1)
                        : 0;

                result.append("Flight ")
                        .append(count++)

                        .append("\nAirline: ")
                        .append(airline)

                        .append("\nFlight Number: ")
                        .append(flightNumber)

                        .append("\nDeparture: ")
                        .append(departure)

                        .append("\nArrival: ")
                        .append(arrival)

                        .append("\nDuration: ")
                        .append(durationText)

                        .append("\nStops: ")
                        .append(stops)

                        .append("\nPrice: ")
                        .append(price)

                        .append("\n\n");
            }

            logger.info(
                    "Flight search successful. Found {} flights.",
                    flights.size());

            return result.toString();

        } catch (HttpStatusCodeException e) {

            logger.error(
                    "Flight API HTTP error. Status: {}, Response: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString());

            return "Flight API request failed with HTTP status "
                    + e.getStatusCode()
                    + ". Please check your API configuration and request parameters.";

        } catch (RestClientException e) {

            logger.error("Flight API connection error", e);

            return "Unable to connect to the flight API. "
                    + "Please check your internet connection or API availability.";

        } catch (Exception e) {

            logger.error("Unexpected flight search error", e);

            return "Unable to search flights: " + e.getMessage();
        }
    }

    // Add flights from API response
    private void addFlights(
            JsonNode response,
            String field,
            List<JsonNode> flights) {

        JsonNode flightList = response.path(field);

        if (flightList.isArray()) {
            flightList.forEach(flights::add);
        }
    }

    // Flight Status
    @Tool(
        name = "checkFlightStatus",
        description = """
            Check the current status of a flight.

            Parameters:
            - flightNumber: Airline flight number, e.g. 6E203 or AI101.

            Returns flight status, departure and arrival information,
            and delay information when available.
            """
    )
    public String checkFlightStatus(String flightNumber) {

        try {

            if (flightNumber == null || flightNumber.isBlank()) {
                return "Please provide a valid flight number.";
            }

            logger.info(
                    "Checking flight status for: {}",
                    flightNumber);

            String url = UriComponentsBuilder
                    .fromUriString(API_URL)
                    .queryParam("engine", "google")
                    .queryParam("q", flightNumber.trim())
                    .queryParam("api_key", apiKey)
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .build()
                    .encode()
                    .toUriString();

            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {
                return "No response received from flight status API.";
            }

            if (response.has("error")) {

                String error = response.path("error").asText();

                logger.error("Flight status API error: {}", error);

                return "API error: " + error;
            }

            JsonNode answerBox = response.path("answer_box");

            if (!"flight_status".equals(
                    answerBox.path("type").asText())) {

                return "Flight status information not available for "
                        + flightNumber;
            }

            logger.info(
                    "Flight status retrieved for: {}",
                    flightNumber);

            return answerBox.toPrettyString();

        } catch (HttpStatusCodeException e) {

            logger.error(
                    "Flight status HTTP error: {}",
                    e.getStatusCode(),
                    e);

            return "Flight status API request failed with HTTP status "
                    + e.getStatusCode();

        } catch (RestClientException e) {

            logger.error("Flight status connection error", e);

            return "Unable to connect to flight status API.";

        } catch (Exception e) {

            logger.error("Unexpected flight status error", e);

            return "Unable to check flight status: "
                    + e.getMessage();
        }
    }

    // Flight Booking
}