package com.dashboard.common.utility.diff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DiffResult {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final List<DiffEntry> entries;

    public DiffResult() {
        this.entries = new ArrayList<>();
    }

    public DiffResult(List<DiffEntry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public void addEntry(DiffEntry entry) {
        entries.add(entry);
    }

    public void addAll(DiffResult other) {
        entries.addAll(other.getEntries());
    }

    public List<DiffEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public boolean hasDifferences() {
        return !entries.isEmpty();
    }

    public String toJson() {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("hasDifferences", hasDifferences());

        ArrayNode entriesArray = root.putArray("entries");
        for (DiffEntry entry : entries) {
            ObjectNode entryNode = entriesArray.addObject();
            entryNode.put("fieldPath", entry.getFieldPath());
            entryNode.putPOJO("oldValue", entry.getOldValue());
            entryNode.putPOJO("newValue", entry.getNewValue());
        }

        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize diff to JSON", e);
        }
    }

    @Override
    public String toString() {
        if (entries.isEmpty()) {
            return "No differences";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < entries.size(); i++) {
            sb.append(entries.get(i).toString());
            if (i < entries.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
