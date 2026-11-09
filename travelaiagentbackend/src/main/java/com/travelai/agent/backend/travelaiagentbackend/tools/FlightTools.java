package com.travelai.agent.backend.travelaiagentbackend.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;


@Component
public class FlightTools {

    
    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String API_URL;

    private static final Map<String, String> AIRPORT_CODES = Map.ofEntries(
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

    @Tool(
        name = "searchFlight",
    description = """
        Search available flights using Google Flights API.

        Parameters:
        - source: Departure city (e.g. Mumbai).
        - destination: Arrival city (e.g. Delhi).
        - date: Travel date in DD-MMM-YYYY format (e.g. 15-Dec-2026).

        Always provide the date in DD-MMM-YYYY format.
        """
    )
    public String searchFlight(
            String source,
            String destination,
            String date) {

        try {

            String sourceCode = getAirportCode(source);
            String destinationCode = getAirportCode(destination);

            String url = UriComponentsBuilder
                    .fromUriString(API_URL)
                    .queryParam("engine", "google_flights")
                    .queryParam("departure_id", sourceCode)
                    .queryParam("arrival_id", destinationCode)
                    .queryParam("type", 2)
                    .queryParam("outbound_date", date)
                    .queryParam("currency", "INR")
                    .queryParam("gl", "in")
                    .queryParam("hl", "en")
                    .queryParam("api_key", apiKey)
                    .build()
                    .encode()
                    .toUriString();

            JsonNode response = restTemplate.getForObject(
                    url, JsonNode.class);

            if (response == null) {
                return "No response received from flight API.";
            }

            if (response.has("error")) {
                return "Flight API error: "
                        + response.get("error").asText();
            }

            List<JsonNode> flights = new ArrayList<>();

            addFlights(response, "best_flights", flights);
            addFlights(response, "other_flights", flights);

            if (flights.isEmpty()) {
                return "No flights found from " + source
                        + " to " + destination
                        + " on " + date;
            }

            StringBuilder result = new StringBuilder();

            result.append("Flight results: ")
                    .append(source).append(" to ")
                    .append(destination)
                    .append("\nDate: ").append(date)
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

                    airline = first.path("airline").asText("Unknown");

                    flightNumber = first.path("flight_number")
                            .asText("N/A");

                    departure = first.path("departure_airport")
                            .path("time").asText("N/A");

                    arrival = last.path("arrival_airport")
                            .path("time").asText("N/A");
                }

                double price = flight.path("price").asDouble(0);

                int duration = flight.path("total_duration")
                        .asInt(0);

                int stops = segments.isArray()
                        ? Math.max(0, segments.size() - 1)
                        : 0;

                result.append("Flight ").append(count++)
                        .append("\nAirline: ").append(airline)
                        .append("\nFlight Number: ").append(flightNumber)
                        .append("\nDeparture: ").append(departure)
                        .append("\nArrival: ").append(arrival)
                        .append("\nDuration: ")
                        .append(duration / 60).append("h ")
                        .append(duration % 60).append("m")
                        .append("\nStops: ").append(stops)
                        .append("\nPrice: INR ")
                        .append(String.format("%.2f", price))
                        .append("\n\n");
            }

            return result.toString();

        } catch (Exception e) {

            return "Unable to search flights: " + e.getMessage();
        }
    }

    private String getAirportCode(String city) {

        String normalized = city.trim().toLowerCase();

        if (normalized.matches("[a-z]{3}")) {
            return normalized.toUpperCase();
        }

        return AIRPORT_CODES.getOrDefault(
                normalized, city.trim().toUpperCase());
    }

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

        String url = UriComponentsBuilder
                .fromUriString(API_URL)
                .queryParam("engine", "google")
                .queryParam("q", flightNumber)
                .queryParam("api_key", apiKey)
                .build()
                .encode()
                .toUriString();

        JsonNode response = restTemplate.getForObject(
                url, JsonNode.class);

        if (response == null) {
            return "No response received from flight status API.";
        }

        if (response.has("error")) {
            return "API error: " + response.path("error").asText();
        }

        JsonNode answerBox = response.path("answer_box");

        if (!"flight_status".equals(
                answerBox.path("type").asText())) {
            return "Flight status information not available for "
                    + flightNumber;
        }

        return answerBox.toPrettyString();

    } catch (Exception e) {
        return "Unable to check flight status: " + e.getMessage();
    }
}

    // Flight Booking

}
