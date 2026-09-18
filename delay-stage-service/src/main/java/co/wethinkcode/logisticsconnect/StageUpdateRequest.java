package co.wethinkcode.logisticsconnect;

// Shape of the JSON body for POST /delay-stage/{hubId}, e.g. { "stage": 3 }
public record StageUpdateRequest(int stage) {}

