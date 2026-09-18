package co.wethinkcode.logisticsconnect;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

import java.awt.desktop.PreferencesEvent;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DelayStageServiceApp {

    private static final int MIN_STAGE = 0;
    private static final int MAX_STAGE = 8;


    //hubId -> current delay stage. ConcurrentHashMap because multiple
    // requests can read/write this at the same time.
    private static final Map<String, Integer> delayStages = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        Javalin app = Javalin.create().start(7052);

        app.get("/health", ctx -> ctx.result("OK"));

        // TODO (Tracks the Transit Delay Stage (0-8, e.g. weather shutdowns).)
        // Add domain endpoints for delay-stage-service here.

        // No delay recorded for a hub simply means "not delayed" - 0, not a 404
        app.get("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").toUpperCase();
            int stage = delayStages.getOrDefault(hubId, 0);
            ctx.json(Map.of("hubId", hubId, "stage",stage));
        });

        app.post("/delay-stage/{hubId}", ctx -> {
            String hubId = ctx.pathParam("hubId").toUpperCase();

            StageUpdateRequest body;
            try {
                body = ctx.bodyAsClass(StageUpdateRequest.class);
            }catch (Exception e) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "Body must be JSON like {\"stage\": 3}"));
                return;
            }

            if (body.stage() < MIN_STAGE || body.stage() > MAX_STAGE) {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
                        "error", "stage must be between " + MIN_STAGE + " and " + MAX_STAGE
                ));
                return;
            }

            delayStages.put(hubId,body.stage());
            System.out.println(hubId + "delay stage updated to " + body.stage());

            // MQ TODO (Day 7): publish this change to MqConfig.TOPIC instead of
            // (or as well as) just storing it here, to transit-service can react
            ctx.json(Map.of("hubId", hubId ,"stage", body.stage()));
        });

        System.out.println("delay-stage-service ready on :7052");
    }
}

// MQ TODO: publishes to ActiveMQ topic MqConfig.TOPIC at MqConfig.BROKER_URL (see co.wethinkcode.logisticsconnect.mq.MqConfig)






















