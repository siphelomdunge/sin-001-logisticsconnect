package co.wethinkcode.logisticsconnect;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class TransitServiceApp {

    private static final String HUB_SERVICE_URL = "http://localhost:7051/hubs/";
    private static final String DELAY_STAGE_URL = "http://localhost:7052/delay-stage/";

    // Base transit time plus a per-stage penalty . Simple and adjustable -
    // the point of this project is the service composition, not a realistic model.
    private static final int BASE_HOURS = 24;
    private static final int HOURS_PER_DELAY_STAGE = 6;

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7053);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Calculates estimated arrival windows based on hub and delay stage.)
        // Add domain endpoints for transit-service here.

        app.get("/eta/{hubId}", ctx ->{
            String hubId = ctx.pathParam("hubId").toUpperCase();

            HubRecord hub;
            try {
                hub = fetchHub(hubId);
            } catch (NotFoundException e) {
                ctx.status(HttpStatus.NOT_FOUND).json(Map.of("error", "No such hub: " + hubId));
                return;
            } catch (Exception e) {
                // Fail fast: don't retry on the request path, just tell the caller clearly.
                ctx.status(HttpStatus.BAD_GATEWAY).json(Map.of(
                        "error", "Could not read hub-service", "detail", e.getMessage()));
                return;
            }

            if (!Boolean.TRUE.equals(hub.active())) {
                ctx.status(HttpStatus.OK).json(Map.of(
                        "hubId", hub.hubId(),
                        "sortingCenter", hub.sortingCenter(),
                        "eta" , "unavailable",
                        "reason" , "hub is not active"
                ));
                return;
            }

            int stage;
            try {
                stage = fetchDelayStage(hubId);
            } catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                        "error", "Could not reach delay-stage-service", "detail" , e.getMessage()));
                return;
            }

            int estimatedHours = BASE_HOURS + (stage * HOURS_PER_DELAY_STAGE);

            ctx.json(Map.of(
                    "hubId", hub.hubId(),
                    "sortingCenter", hub.sortingCenter(),
                    "delayStage", stage,
                    "estimatedHours", estimatedHours
            ));
        });

        System.out.println("transit-service ready on 7053");
    }

    static HubRecord fetchHub(String hubId) throws Exception{
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HUB_SERVICE_URL + hubId))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            throw new NotFoundException();
        }
        if (response.statusCode() != 200) {
            throw new RuntimeException("hub-service returned HTTP " + response.statusCode());
        }

        return mapper.readValue(response.body(), HubRecord.class);
    }

    static int fetchDelayStage(String hubId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(DELAY_STAGE_URL + hubId))
                .timeout(Duration.ofSeconds(3))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("delay-stage-service returned HTTP " + response.statusCode());
        }

        return mapper.readValue(response.body(), DelayStageResponse.class).stage();
    }

    static class NotFoundException extends RuntimeException {}
}

// MQ TODO: subscribes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)































