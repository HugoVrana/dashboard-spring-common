package com.dashboard.common.utility.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiffResultTest {

    private final ObjectMapper mapper = new ObjectMapper();

    // ==================== Basic Functionality ====================

    @Test
    void emptyResult_hasDifferences_returnsFalse() {
        DiffResult result = new DiffResult();
        assertFalse(result.hasDifferences());
    }

    @Test
    void resultWithEntry_hasDifferences_returnsTrue() {
        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("field", "old", "new"));
        assertTrue(result.hasDifferences());
    }

    @Test
    void getEntries_returnsUnmodifiableList() {
        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("field", "old", "new"));

        List<DiffEntry> entries = result.getEntries();
        assertThrows(UnsupportedOperationException.class, () -> entries.add(new DiffEntry("x", "y", "z")));
    }

    @Test
    void addAll_mergesResults() {
        DiffResult result1 = new DiffResult();
        result1.addEntry(new DiffEntry("field1", "a", "b"));

        DiffResult result2 = new DiffResult();
        result2.addEntry(new DiffEntry("field2", "c", "d"));

        result1.addAll(result2);

        assertEquals(2, result1.getEntries().size());
    }

    // ==================== JSON Serialization ====================

    @Test
    void toJson_emptyResult_returnsValidJson() throws Exception {
        DiffResult result = new DiffResult();
        String json = result.toJson();

        JsonNode node = mapper.readTree(json);
        assertFalse(node.get("hasDifferences").asBoolean());
        assertTrue(node.get("entries").isArray());
        assertEquals(0, node.get("entries").size());
    }

    @Test
    void toJson_withEntries_serializesCorrectly() throws Exception {
        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("name", "old", "new"));

        String json = result.toJson();
        JsonNode node = mapper.readTree(json);

        assertTrue(node.get("hasDifferences").asBoolean());
        assertEquals(1, node.get("entries").size());

        JsonNode entry = node.get("entries").get(0);
        assertEquals("name", entry.get("fieldPath").asText());
        assertEquals("old", entry.get("oldValue").asText());
        assertEquals("new", entry.get("newValue").asText());
    }

    @Test
    void toJson_withNullValues_serializesAsNull() throws Exception {
        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("field", null, "new"));

        String json = result.toJson();
        JsonNode node = mapper.readTree(json);

        JsonNode entry = node.get("entries").get(0);
        assertTrue(entry.get("oldValue").isNull());
        assertEquals("new", entry.get("newValue").asText());
    }

    @Test
    void toJson_withIntegerValues_serializesCorrectly() throws Exception {
        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("count", 1, 2));

        String json = result.toJson();
        JsonNode node = mapper.readTree(json);

        JsonNode entry = node.get("entries").get(0);
        assertEquals(1, entry.get("oldValue").asInt());
        assertEquals(2, entry.get("newValue").asInt());
    }

    @Test
    void toJson_withBooleanValues_serializesCorrectly() throws Exception {
        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("active", true, false));

        String json = result.toJson();
        JsonNode node = mapper.readTree(json);

        JsonNode entry = node.get("entries").get(0);
        assertTrue(entry.get("oldValue").asBoolean());
        assertFalse(entry.get("newValue").asBoolean());
    }

    // ==================== Java 8 Date/Time Types ====================

    @Test
    void toJson_withLocalDate_serializesAsString() throws Exception {
        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("date", LocalDate.of(2024, 1, 15), LocalDate.of(2024, 2, 20)));

        String json = result.toJson();
        JsonNode node = mapper.readTree(json);

        JsonNode entry = node.get("entries").get(0);
        assertEquals("2024-01-15", entry.get("oldValue").asText());
        assertEquals("2024-02-20", entry.get("newValue").asText());
    }

    @Test
    void toJson_withInstant_serializesAsString() throws Exception {
        Instant oldInstant = Instant.parse("2024-01-15T10:30:00Z");
        Instant newInstant = Instant.parse("2024-02-20T14:45:00Z");

        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("timestamp", oldInstant, newInstant));

        String json = result.toJson();
        JsonNode node = mapper.readTree(json);

        JsonNode entry = node.get("entries").get(0);
        assertTrue(entry.get("oldValue").asText().contains("2024-01-15"));
        assertTrue(entry.get("newValue").asText().contains("2024-02-20"));
    }

    // ==================== Complex Objects ====================

    static class NestedObject {
        String name;
        int value;

        NestedObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() { return name; }
        public int getValue() { return value; }
    }

    @Test
    void toJson_withComplexObject_serializesNestedStructure() throws Exception {
        NestedObject obj = new NestedObject("test", 42);

        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("object", null, obj));

        String json = result.toJson();
        JsonNode node = mapper.readTree(json);

        JsonNode entry = node.get("entries").get(0);
        assertTrue(entry.get("oldValue").isNull());

        JsonNode newValue = entry.get("newValue");
        assertEquals("test", newValue.get("name").asText());
        assertEquals(42, newValue.get("value").asInt());
    }

    // ==================== toString ====================

    @Test
    void toString_emptyResult_returnsNoDifferences() {
        DiffResult result = new DiffResult();
        assertEquals("No differences", result.toString());
    }

    @Test
    void toString_withEntries_formatsCorrectly() {
        DiffResult result = new DiffResult();
        result.addEntry(new DiffEntry("name", "old", "new"));

        String str = result.toString();
        assertTrue(str.contains("name"));
        assertTrue(str.contains("old"));
        assertTrue(str.contains("new"));
    }
}
