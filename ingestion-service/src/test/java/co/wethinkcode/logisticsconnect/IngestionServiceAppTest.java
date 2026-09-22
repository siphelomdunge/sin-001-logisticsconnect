package co.wethinkcode.logisticsconnect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IngestionServiceAppTest {

    // This is the exact bug from Day 3 — .replace() instead of .replaceAll()
    // silently failed to collapse double spaces. This test makes that bug
    // impossible to reintroduce without a test failure telling you immediately.
    @Test
    @DisplayName("cleanText collapses double spaces")
    void cleanTextCollapsesDoubleSpaces() {
        assertEquals("Cape Town Port", IngestionServiceApp.cleanText("Cape Town  Port"));
    }

    @Test
    @DisplayName("cleanText trims leading and trailing whitespace")
    void cleanTextTrims() {
        assertEquals("Gauteng", IngestionServiceApp.cleanText(" Gauteng "));
    }

    @Test
    @DisplayName("cleanId uppercases and trims")
    void cleanIdUppercasesAndTrims() {
        assertEquals("H-501", IngestionServiceApp.cleanId("h-501 "));
    }

    @Test
    @DisplayName("titleCase handles mixed and all-caps input")
    void titleCaseHandlesMixedCasing() {
        assertEquals("Gauteng", IngestionServiceApp.titleCase("GAUTENG"));
        assertEquals("Gauteng", IngestionServiceApp.titleCase("gauteng"));
    }

    @Test
    @DisplayName("parseActive recognizes true tokens regardless of case")
    void parseActiveRecognizesTrueTokens() {
        assertEquals(true, IngestionServiceApp.parseActive("Y"));
        assertEquals(true, IngestionServiceApp.parseActive("yes"));
        assertEquals(true, IngestionServiceApp.parseActive("TRUE"));
        assertEquals(true, IngestionServiceApp.parseActive("1"));
    }

    @Test
    @DisplayName("parseActive recognizes false tokens regardless of case")
    void parseActiveRecognizesFalseTokens() {
        assertEquals(false, IngestionServiceApp.parseActive("N"));
        assertEquals(false, IngestionServiceApp.parseActive("no"));
        assertEquals(false, IngestionServiceApp.parseActive("FALSE"));
        assertEquals(false, IngestionServiceApp.parseActive("0"));
    }

    @Test
    @DisplayName("parseActive returns null for missing or unrecognized values")
    void parseActiveReturnsNullForMissingOrUnrecognized() {
        assertNull(IngestionServiceApp.parseActive("unknown"));
        assertNull(IngestionServiceApp.parseActive("N/A"));
        assertNull(IngestionServiceApp.parseActive("banana")); // unrecognized -> unknown, not false
    }

    @Test
    @DisplayName("cleanOrNull converts missing tokens to null")
    void cleanOrNullConvertsMissingTokensToNull() {
        assertNull(IngestionServiceApp.cleanOrNull("N/A", true));
        assertNull(IngestionServiceApp.cleanOrNull("", false));
        assertNull(IngestionServiceApp.cleanOrNull(null, false));
    }

    // This test directly encodes the Johannesburg Central scenario from Day 3:
    // four rows, same real-world hub, different casing, conflicting active flags.
    @Test
    @DisplayName("dedupe merges same sorting center despite case differences, any-true-wins on active")
    void dedupeMergesDuplicatesCaseInsensitively() {
        List<HubRecord> input = List.of(
                new HubRecord("H-500", "Gauteng", "Johannesburg Central", true),
                new HubRecord("H-504", "Gauteng", "Johannesburg Central", true),
                new HubRecord("H-510", "Gauteng", "johannesburg central", false),
                new HubRecord("H-515", "Gauteng", "Johannesburg Central", true)
        );

        List<HubRecord> result = IngestionServiceApp.dedupe(input);

        assertEquals(1, result.size(), "all four rows should merge into one hub");
        HubRecord merged = result.get(0);
        assertEquals("H-500", merged.hubId(), "lowest hubId should win");
        assertEquals(true, merged.active(), "any-true-wins: one true record beats one false");
    }

    @Test
    @DisplayName("dedupe fills missing province from a sibling duplicate")
    void dedupeFillsMissingProvinceFromSibling() {
        List<HubRecord> input = List.of(
                new HubRecord("H-502", "Gauteng", "Pretoria North", false),
                new HubRecord("H-508", null, "Pretoria North", true)
        );

        List<HubRecord> result = IngestionServiceApp.dedupe(input);

        assertEquals(1, result.size());
        assertEquals("Gauteng", result.get(0).province(), "province should be filled in from the sibling row");
    }

    @Test
    @DisplayName("dedupe leaves non-duplicate hubs untouched")
    void dedupeLeavesUniqueHubsAlone() {
        List<HubRecord> input = List.of(
                new HubRecord("H-507", "Free State", "Bloemfontein Hub", false),
                new HubRecord("H-509", "Eastern Cape", "Port Elizabeth Hub", false)
        );

        List<HubRecord> result = IngestionServiceApp.dedupe(input);

        assertEquals(2, result.size());
    }
}