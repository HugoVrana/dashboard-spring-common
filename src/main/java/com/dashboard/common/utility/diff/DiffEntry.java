package com.dashboard.common.utility.diff;

import java.util.Objects;

public class DiffEntry {

    private final String fieldPath;
    private final Object oldValue;
    private final Object newValue;

    public DiffEntry(String fieldPath, Object oldValue, Object newValue) {
        this.fieldPath = fieldPath;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getFieldPath() {
        return fieldPath;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    @Override
    public String toString() {
        return fieldPath + ": " + formatValue(oldValue) + " -> " + formatValue(newValue);
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        return String.valueOf(value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiffEntry diffEntry = (DiffEntry) o;
        return Objects.equals(fieldPath, diffEntry.fieldPath) &&
                Objects.equals(oldValue, diffEntry.oldValue) &&
                Objects.equals(newValue, diffEntry.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldPath, oldValue, newValue);
    }
}
