package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import io.javalin.Javalin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class IngestionServiceApp {

    public static void main(String[] args) throws Exception {
        List<HubRecord> cleaned = loadAndClean();

        Javalin app = Javalin.create().start(7050);
        app.get("/health", ctx -> ctx.result("OK"));

        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        cleaned.forEach(System.out::println);
    }

    static List<HubRecord> loadAndClean() throws Exception {
        List<HubRecord> records = new ArrayList<>();

        try (InputStream in = IngestionServiceApp.class
                .getResourceAsStream("/hubs-global.csv");
             CSVReader reader = new CSVReader(new InputStreamReader(in)))  {

            reader.skip(1);
            String[] row;
            while ((row = reader.readNext()) != null) {
                records.add(new HubRecord(
                        cleanId(row[0]),
                        titleCase(row[1]),
                        cleanText(row[2]),
                        row[3].trim()
                ));
            }
        }
        return records;
    }

    // Trims and collapses internal double-space
    static String cleanText(String s) {
        if (s == null) return null;
        return s.trim().replace("\\s+" , " ");
    }

    //Hub IDs: uppercase, trimmed. "h-501 " -> "H-501"
    static String cleanId(String s) {
        return cleanText(s).toUpperCase();
    }

    //"gauteng" / "GAUTENG" / " Gauteng " -> "Gauteng"
    static String titleCase(String s) {
        String clean = cleanText(s);
        if (clean.isEmpty()) return clean;
        String[] words = clean.toLowerCase().split(" ");
        StringBuilder result = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                result.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
}
