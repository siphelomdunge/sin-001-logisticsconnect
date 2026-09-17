package co.wethinkcode.logisticsconnect;

// Mirrors ingestion-service's HubRecord shape. Duplicated deliberately —
// hub-service has no shared dependency on ingestion-service's code, only
// on the JSON contract it exposes over HTTP.
public record HubRecord(
        String hubId,
        String province,
        String sortingCenter,
        Boolean active
) {}