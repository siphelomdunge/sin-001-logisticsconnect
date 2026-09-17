package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class HubServiceApp {

    private static final String INGESTION_URL = "http://localhost:7050/hubs";
    private static final int MAX_ATTEMPTS = 5;
    private static final long RETRY_DELAY_MS = 1000;

    public static void main(String[] args) {
        Map<String, HubRecord> hubsById = fetchHubsWithRetry();

        Javalin app = Javalin.create().start(7051);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Serves provinces and sorting centers (place-name source of truth).)
        // Add domain endpoints for hub-service here.

        app.get("/hubs", ctx -> ctx.json(hubsById.values()));

        app.get("/hubs/{hubId}", ctx -> {
            String id = ctx.pathParam("hubId").toUpperCase();
            HubRecord hub = hubsById.get(id);
            if (hub == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No such hub: " + id));
            } else {
                ctx.json(hub);
            }
        });

        System.out.println("hub-service ready on :7051 with " + hubsById.size() + " hubs cached");
    }

    //Reference data (hub list) is fetched once at startup and cached - it
    // doesn't change while the system runs, unlike delay stages.
    // Retries because ingestion-service might not be up yet when this starts.
    static Map<String, HubRecord> fetchHubsWithRetry() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(INGESTION_URL))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        Exception lastError = null;
        for (int attempt = 1;attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response response
            }
        }
    }
}


























