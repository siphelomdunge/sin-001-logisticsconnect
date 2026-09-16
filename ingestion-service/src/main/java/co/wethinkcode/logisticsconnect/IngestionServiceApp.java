package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import io.javalin.Javalin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class IngestionServiceApp {

    //Tokens meaning "no value", case-insensitive. checked after trimming
    private static final Set<String> MISSING_TOKENS =
            Set.of("n/a", "na", "unknown", "-", "", "nan");

    private static final Set<String> TRUE_TOKENS =
            Set.of("y", "yes", "true", "1");

    private static final Set<String> FALSE_TOKENS =
            Set.of("n", "no", "false", "0");

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
        int skipped = 0;

        try (InputStream in = IngestionServiceApp.class
                .getResourceAsStream("/hubs-global.csv");
             CSVReader reader = new CSVReader(new InputStreamReader(in)))  {

            reader.skip(1);
            String[] row;
            while ((row = reader.readNext()) != null) {
                String hubId = cleanId(row[0]);

                //hubId is our primary key - a row without one is unusable, skip it
                if (hubId == null || hubId.isEmpty()) {
                    skipped++;
                    continue;
                }

                records.add(new HubRecord(
                        hubId,
                        cleanOrNull(row[1], true), //province, title-cased
                        cleanOrNull(row[2], false), // sortingCenter
                        parseActive(row[3])
                ));
            }
        }

        System.out.println("Loaded " + records.size() + " hubs, skipped " + skipped + " with no hubId.");
        return records;
    }


    // Cleans text, applies title-casing if requested, and converts missing tokens to null
    static String cleanOrNull(String raw, boolean titleCase) {
        String clean = cleanText(raw);
        if (clean == null || MISSING_TOKENS.contains(clean.toLowerCase())) {
            return null;
        }
        return titleCase ? titleCase(clean) : clean;
    }

    // "Y" / "yes" / "1" / "TRUE" -> true. "N" / "no" / "0" / "FALSE" -> false.
    // Missing tokens or anything unrecognized -> null (unknown, not assumed false).
    static Boolean parseActive(String raw) {
        String clean = cleanText(raw);
        if (clean == null) return null;

        String lower = clean.toLowerCase();
        if (MISSING_TOKENS.contains(lower)) return null;
        if (TRUE_TOKENS.contains(lower)) return true;
        if (FALSE_TOKENS.contains(lower)) return false;

        System.err.println("Unrecognized active value, treating as unknown: \"" + raw + "\"");
        return null;
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
