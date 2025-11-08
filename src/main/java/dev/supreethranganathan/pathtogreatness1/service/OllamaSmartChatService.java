package dev.supreethranganathan.pathtogreatness1.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.supreethranganathan.pathtogreatness1.model.ChatModel;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class OllamaSmartChatService {

    @Autowired
    OllamaChatService ollamaChatService;

    @Autowired
    ObjectMapper mapper;

    public Flux<String> getSmartOllamaResponse(ChatModel chatModel) {
        return ollamaChatService.getStreamingOllamaResponse(chatModel)
                .collectList()
                .flatMapMany(responses -> {
                    String firstResponse = String.join("", responses);

                    // 🧠 Step 1: Check if Ollama seemed unsure
                    if (isOllamaUncertain(firstResponse)) {
                        System.out.println("🤖 Ollama seems unsure — fetching real web context...");

                        // 🕸️ Step 2: Fetch real info dynamically (weather/news/general)
                        String webInfo = fetchQueryType(chatModel.prompt());

                        // 🧩 Step 3: Construct a contextual, enhanced prompt for Ollama
                        String enhancedPrompt = """
                        You previously said you weren't sure.
                        Here is verified live information from the web:
                        %s

                        Now use this data to give an updated and accurate answer.
                        Keep it concise and human-like.
                        """.formatted(webInfo);

                        ChatModel enriched = new ChatModel(enhancedPrompt);


                        // 🧠 Step 4: Stream second round of reasoning with context
                        return ollamaChatService.getStreamingOllamaResponse(enriched);
                    }

                    // ✅ Step 5: Otherwise, return the first confident response
                    return Flux.fromIterable(responses);
                });
    }


    private boolean isOllamaUncertain(String response) {
        String lower = response.toLowerCase();
        return lower.contains("not sure") ||
                lower.contains("don't have access") ||
                lower.contains("as of my last update") ||
                lower.contains("real-time") ||
                lower.contains("unknown") ||
                lower.contains("cannot provide") ||
                lower.contains("today") ||
                lower.contains("current") ||
                lower.contains("now");
    }

    private String detectQueryType(String prompt) {
        String lower = prompt.toLowerCase();
        if (lower.contains("weather")) return "weather";
        if (lower.contains("news")) return "news";
        if (lower.contains("stock")) return "stock";
        return "general";
    }

    private String fetchQueryType(String query) {
        switch (detectQueryType(query)) {
            case "weather": return fetchWeather(query);
            case "news": return fetchNews(query);
            default: return fetchDuckDuckGo(query);
        }
    }

    private String fetchWeather(String query) {
        try {
            // Extract location keywords from prompt
            String location = extractLocationForWeather(query);
            if (location == null || location.isBlank()) {
                location = "Bangalore"; // fallback
            }

            // 🔹 Step 1: Get coordinates from Open-Meteo geocoding API
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                    + URLEncoder.encode(location, StandardCharsets.UTF_8);

            String geoJson = Jsoup.connect(geoUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .execute()
                    .body();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode geoRoot = mapper.readTree(geoJson);

            if (!geoRoot.has("results") || geoRoot.get("results").isEmpty()) {
                return "Couldn't find coordinates for " + location;
            }

            JsonNode firstResult = geoRoot.get("results").get(0);
            double lat = firstResult.get("latitude").asDouble();
            double lon = firstResult.get("longitude").asDouble();

            // 🔹 Step 2: Fetch live weather data
            String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%f&longitude=%f&current_weather=true",
                    lat, lon
            );

            String weatherJson = Jsoup.connect(weatherUrl)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(5000)
                    .execute()
                    .body();

            JsonNode root = mapper.readTree(weatherJson);
            JsonNode weather = root.path("current_weather");

            String description = switch (weather.get("weathercode").asInt()) {
                case 0 -> "Clear sky ☀️";
                case 1, 2, 3 -> "Partly cloudy ⛅";
                case 45, 48 -> "Fog 🌫️";
                case 51, 53, 55 -> "Drizzle 🌦️";
                case 61, 63, 65 -> "Rain 🌧️";
                case 71, 73, 75 -> "Snow 🌨️";
                case 95 -> "Thunderstorm ⛈️";
                default -> "Unclear weather";
            };

            return "🌍 Weather in " + location + ":\n" +
                    "Temperature: " + weather.get("temperature").asText() + "°C\n" +
                    "Wind Speed: " + weather.get("windspeed").asText() + " km/h\n" +
                    "Condition: " + description;

        } catch (Exception e) {
            return "❌ Couldn't fetch live weather: " + e.getMessage();
        }
    }

    private String extractLocationForNews(String query) {
        query = query.toLowerCase();
        if (query.contains("in ")) {
            return query.substring(query.indexOf("in ") + 3).replaceAll("[^a-zA-Z,\\s]", "").trim();
        } else if (query.contains("at ")) {
            return query.substring(query.indexOf("at ") + 3).replaceAll("[^a-zA-Z,\\s]", "").trim();
        }
        return query.replaceAll("news|headlines|latest|today", "")
                .replaceAll("[^a-zA-Z,\\s]", "")
                .trim();
    }


    private String fetchNews(String query) {
        try {
            String location = extractLocationForNews(query);
            if (location == null || location.isBlank()) {
                location = "world";
            }

            String encodedQuery = URLEncoder.encode("latest news " + location, StandardCharsets.UTF_8);
            String url = "https://gnews.io/api/v4/search?q=" + encodedQuery
                    + "&lang=en&max=5&token=" + System.getenv("GNews_API_KEY");
            // ⚠️ This is a demo token (public). Replace with yours if needed.

            String json = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0")
                    .timeout(6000)
                    .execute()
                    .body();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode articles = root.path("articles");

            if (articles.isEmpty()) {
                return "No recent news found for " + location;
            }

            StringBuilder sb = new StringBuilder("🗞️ Top headlines for " + location + ":\n\n");
            for (int i = 0; i < Math.min(5, articles.size()); i++) {
                JsonNode article = articles.get(i);
                sb.append(i + 1).append(". ")
                        .append(article.path("title").asText())
                        .append("\n   📰 ").append(article.path("description").asText())
                        .append("\n   🔗 ").append(article.path("url").asText())
                        .append("\n\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "❌ Couldn't fetch news: " + e.getMessage();
        }
    }


    private String extractLocationForWeather(String query) {
        // Very lightweight extraction
        query = query.toLowerCase();
        if (query.contains("in ")) {
            return query.substring(query.indexOf("in ") + 3).replaceAll("[^a-zA-Z,\\s]", "").trim();
        } else if (query.contains("at ")) {
            return query.substring(query.indexOf("at ") + 3).replaceAll("[^a-zA-Z,\\s]", "").trim();
        }
        return query.replaceAll("weather|current|forecast|temperature|today", "")
                .replaceAll("[^a-zA-Z,\\s]", "")
                .trim();
    }

    private String fetchDuckDuckGo(String query) {
        try {
            String url = "https://api.duckduckgo.com/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .get();
            return doc.text().substring(0, Math.min(doc.text().length(), 1500));
        } catch (Exception e) {
            return "No additional context found.";
        }
    }



}
