package co.wethinkcode.logisticsconnect;

import com.opencsv.CSVReader;
import io.javalin.Javalin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;

public class IngestionServiceApp {

    //Tokens meaning "no value", case-insensitive. checked after trimming
    private static final Set<String> MISSING_TOKENS =
            Set.of("n/a", "na", "unknown", "-", "", "nan");

    private static final Set<String> TRUE_TOKENS =
            Set.of("y", "yes", "true", "1");

    private static final Set<String> FALSE_TOKENS =
            Set.of("n", "no", "false", "0");

    public static void main(String[] args) throws Exception {

        // TODO: read and clean src/main/resources/hubs-global.csv (hubs, sorting centers, regional districts data —
        // trim whitespace, fix casing, normalize dates/booleans) and expose the
        // cleaned records here for the other services to consume.

        List<HubRecord> cleaned = dedupe(loadAndClean());

        Javalin app = Javalin.create().start(7050);
        app.get("/health", ctx -> ctx.result("OK"));

        // Stage 1 done: cleaned + deduped hub data, available to every other service
        app.get("/hubs", ctx -> ctx.json(cleaned));

        System.out.println("ingestion-service ready on :7050 with " + cleaned.size() + " hubs");
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


    // Groups records that share a sortingCenter (the one field that stays consistent
    // across duplicates even when province formatting or hubId doesn't) and merges
    // each group into a single canonical HubRecord.
    static List<HubRecord> dedupe(List<HubRecord> records) {
        LinkedHashMap<String, List<HubRecord>> groups = new LinkedHashMap<>();

        for (HubRecord r : records) {
            // No sortingCenter to group on -> keep it standalone, keyed by its own hubId.
            String key = (r.sortingCenter() != null)
                    ? r.sortingCenter().toLowerCase() // normalize case so "johannesburg central"
                    : "__no-center__" + r.hubId();    // groups with "Johannesburg Central"
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(r);
        }

        List<HubRecord> merged = new ArrayList<>();
        for (List<HubRecord> group : groups.values()) {
            if (group.size() == 1) {
                merged.add(group.get(0));
                continue;
            }

            System.out.println("Merging " + group.size() + " duplicate rows for \""
                    + group.get(0).sortingCenter() + "\" : "
                    + group.stream().map(HubRecord::hubId).toList());

            String canonicalId = group.stream()
                    .map(HubRecord::hubId)
                    .min(Comparator.naturalOrder())
                    .orElseThrow();

            String canonicalProvince = group.stream()
                    .map(HubRecord::province)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            //Prefer the properly title-cased spelling for the display name,
            //instead of whichever row happened to be inserted first.
            String canonicalSortingCenter = group.stream()
                    .map(HubRecord::sortingCenter)
                    .max(Comparator.comparing(s -> (int) s.chars().filter(Character::isUpperCase).count()))
                    .orElseThrow();

            //Any true wins: one working record is trusted over conflicting down-flags
            Boolean canonicalActive = group.stream().anyMatch(hr -> Boolean.TRUE.equals(hr.active()))
                    ? Boolean.TRUE
                    : group.stream().allMatch(hr -> hr.active() == null) ? null : Boolean.FALSE;

            merged.add(new HubRecord(canonicalId, canonicalProvince, group.get(0).sortingCenter(), canonicalActive));
        }

        return merged;
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
        return s.trim().replaceAll("\\s+", " ");
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
