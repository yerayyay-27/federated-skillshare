package it.unibo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

class FederationEventTest {

    @Test
    void locallyCreatedEventsHaveUniqueNonBlankIds() {
        FederationEvent first = FederationEvent.announcementDeleted("inst-a", "ann-1");
        FederationEvent second = FederationEvent.announcementDeleted("inst-a", "ann-2");

        assertNotNull(first.getEventId());
        assertFalse(first.getEventId().trim().isEmpty());
        assertFalse(first.getEventId().equals(second.getEventId()));
    }

    @Test
    void eventIdSurvivesJsonRoundTrip() {
        FederationEvent original = FederationEvent.announcementDeleted("inst-a", "ann-1");
        Gson gson = new Gson();

        FederationEvent restored = gson.fromJson(gson.toJson(original), FederationEvent.class);

        assertEquals(original.getEventId(), restored.getEventId());
        assertEquals(original.getType(), restored.getType());
    }
}
