package co.wethinkcode.logisticsconnect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class DelayStageServiceAppTest {

    @Test
    @DisplayName("boundary values 0 and 8 are valid")
    void boundaryValuesAreValid() {
        assertTrue(DelayStageServiceApp.isValidStage(0));
        assertTrue(DelayStageServiceApp.isValidStage(8));
    }

    @Test
    @DisplayName("values just outside the boundary are invalid")
    void justOutsideBoundaryIsInvalid() {
        assertFalse(DelayStageServiceApp.isValidStage(-1));
        assertFalse(DelayStageServiceApp.isValidStage(9));
    }

    @Test
    @DisplayName("a mid-range value is valid")
    void midRangeIsValid() {
        assertTrue(DelayStageServiceApp.isValidStage(4));
    }

    @Test
    @DisplayName("wildly out-of-range values are invalid")
    void wildlyOutOfRangeIsInvalid() {
        assertFalse(DelayStageServiceApp.isValidStage(99));
        assertFalse(DelayStageServiceApp.isValidStage(-100));
    }
}