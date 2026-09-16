package co.wethinkcode.logisticsconnect;

public record HubRecord(
        String hubId,
        String province,  // null if missing
        String sortingCenter,  // null if missing
        Boolean active  // null if missing/unparseable - true "unknown" state
){}
