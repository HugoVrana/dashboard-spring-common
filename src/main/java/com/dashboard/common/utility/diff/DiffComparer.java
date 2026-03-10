package com.dashboard.common.utility.diff;

import java.lang.reflect.Field;
import java.util.*;

public class DiffComparer<T> {

    private final T oldValue;
    private final T currentValue;
    private final Set<ObjectPair> visited = new HashSet<>();

    private static final Set<Class<?>> PRIMITIVE_TYPES = Set.of(
            Boolean.class, Byte.class, Short.class, Integer.class, Long.class,
            Float.class, Double.class, Character.class, String.class,
            boolean.class, byte.class, short.class, int.class, long.class,
            float.class, double.class, char.class
    );

    public DiffComparer(T oldValue, T currentValue) {
        this.oldValue = oldValue;
        this.currentValue = currentValue;
    }

    public DiffResult compare() {
        DiffResult result = new DiffResult();

        if (oldValue == null && currentValue == null) {
            return result;
        }

        if (oldValue == null) {
            result.addEntry(new DiffEntry("", null, currentValue));
            return result;
        }

        if (currentValue == null) {
            result.addEntry(new DiffEntry("", oldValue, null));
            return result;
        }

        compareObjects(oldValue, currentValue, "", result);
        return result;
    }

    private void compareObjects(Object oldObj, Object newObj, String pathPrefix, DiffResult result) {
        if (oldObj == null && newObj == null) {
            return;
        }

        if (oldObj == null || newObj == null) {
            result.addEntry(new DiffEntry(pathPrefix, oldObj, newObj));
            return;
        }

        Class<?> clazz = oldObj.getClass();

        if (isPrimitiveOrWrapper(clazz)) {
            if (!Objects.equals(oldObj, newObj)) {
                result.addEntry(new DiffEntry(pathPrefix, oldObj, newObj));
            }
            return;
        }

        if (clazz.isEnum()) {
            if (!Objects.equals(oldObj, newObj)) {
                result.addEntry(new DiffEntry(pathPrefix, oldObj, newObj));
            }
            return;
        }

        // Check for circular reference before recursing into complex objects
        ObjectPair pair = new ObjectPair(oldObj, newObj);
        if (visited.contains(pair)) {
            return;
        }
        visited.add(pair);

        if (oldObj instanceof Collection && newObj instanceof Collection) {
            compareCollections((Collection<?>) oldObj, (Collection<?>) newObj, pathPrefix, result);
            return;
        }

        if (oldObj instanceof Map && newObj instanceof Map) {
            compareMaps((Map<?, ?>) oldObj, (Map<?, ?>) newObj, pathPrefix, result);
            return;
        }

        compareFields(oldObj, newObj, pathPrefix, result);
    }

    private void compareFields(Object oldObj, Object newObj, String pathPrefix, DiffResult result) {
        Class<?> clazz = oldObj.getClass();

        while (clazz != null && clazz != Object.class) {
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                if (field.isAnnotationPresent(DiffIgnore.class)) {
                    continue;
                }

                if (field.isSynthetic()) {
                    continue;
                }

                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    // Skip fields that cannot be made accessible
                    continue;
                }

                try {
                    Object oldFieldValue = field.get(oldObj);
                    Object newFieldValue = field.get(newObj);

                    String fieldPath = pathPrefix.isEmpty() ? field.getName() : pathPrefix + "." + field.getName();

                    if (oldFieldValue == null && newFieldValue == null) {
                        continue;
                    }

                    if (oldFieldValue == null || newFieldValue == null) {
                        result.addEntry(new DiffEntry(fieldPath, oldFieldValue, newFieldValue));
                        continue;
                    }

                    if (isPrimitiveOrWrapper(field.getType())) {
                        if (!Objects.equals(oldFieldValue, newFieldValue)) {
                            result.addEntry(new DiffEntry(fieldPath, oldFieldValue, newFieldValue));
                        }
                    } else if (field.getType().isEnum()) {
                        if (!Objects.equals(oldFieldValue, newFieldValue)) {
                            result.addEntry(new DiffEntry(fieldPath, oldFieldValue, newFieldValue));
                        }
                    } else if (Collection.class.isAssignableFrom(field.getType())) {
                        compareCollections((Collection<?>) oldFieldValue, (Collection<?>) newFieldValue, fieldPath, result);
                    } else if (Map.class.isAssignableFrom(field.getType())) {
                        compareMaps((Map<?, ?>) oldFieldValue, (Map<?, ?>) newFieldValue, fieldPath, result);
                    } else {
                        compareObjects(oldFieldValue, newFieldValue, fieldPath, result);
                    }
                } catch (IllegalAccessException e) {
                    // Skip inaccessible fields
                }
            }

            clazz = clazz.getSuperclass();
        }
    }

    private void compareCollections(Collection<?> oldColl, Collection<?> newColl, String pathPrefix, DiffResult result) {
        if (oldColl instanceof List && newColl instanceof List) {
            compareLists((List<?>) oldColl, (List<?>) newColl, pathPrefix, result);
        } else if (oldColl instanceof Set && newColl instanceof Set) {
            compareSets((Set<?>) oldColl, (Set<?>) newColl, pathPrefix, result);
        } else {
            List<?> oldList = new ArrayList<>(oldColl);
            List<?> newList = new ArrayList<>(newColl);
            compareLists(oldList, newList, pathPrefix, result);
        }
    }

    private void compareLists(List<?> oldList, List<?> newList, String pathPrefix, DiffResult result) {
        int maxSize = Math.max(oldList.size(), newList.size());

        for (int i = 0; i < maxSize; i++) {
            String indexPath = pathPrefix + "[" + i + "]";

            if (i >= oldList.size()) {
                result.addEntry(new DiffEntry(indexPath, null, newList.get(i)));
            } else if (i >= newList.size()) {
                result.addEntry(new DiffEntry(indexPath, oldList.get(i), null));
            } else {
                Object oldItem = oldList.get(i);
                Object newItem = newList.get(i);

                if (oldItem == null && newItem == null) {
                    continue;
                }

                if (oldItem == null || newItem == null) {
                    result.addEntry(new DiffEntry(indexPath, oldItem, newItem));
                    continue;
                }

                if (isPrimitiveOrWrapper(oldItem.getClass())) {
                    if (!Objects.equals(oldItem, newItem)) {
                        result.addEntry(new DiffEntry(indexPath, oldItem, newItem));
                    }
                } else {
                    compareObjects(oldItem, newItem, indexPath, result);
                }
            }
        }
    }

    private void compareSets(Set<?> oldSet, Set<?> newSet, String pathPrefix, DiffResult result) {
        Set<Object> onlyInOld = new LinkedHashSet<>(oldSet);
        onlyInOld.removeAll(newSet);

        Set<Object> onlyInNew = new LinkedHashSet<>(newSet);
        onlyInNew.removeAll(oldSet);

        for (Object item : onlyInOld) {
            result.addEntry(new DiffEntry(pathPrefix + "[-]", item, null));
        }

        for (Object item : onlyInNew) {
            result.addEntry(new DiffEntry(pathPrefix + "[+]", null, item));
        }
    }

    private void compareMaps(Map<?, ?> oldMap, Map<?, ?> newMap, String pathPrefix, DiffResult result) {
        Set<Object> allKeys = new LinkedHashSet<>();
        allKeys.addAll(oldMap.keySet());
        allKeys.addAll(newMap.keySet());

        for (Object key : allKeys) {
            String keyPath = pathPrefix + "[" + key + "]";
            Object oldVal = oldMap.get(key);
            Object newVal = newMap.get(key);

            if (oldVal == null && newVal == null) {
                continue;
            }

            if (!oldMap.containsKey(key)) {
                result.addEntry(new DiffEntry(keyPath, null, newVal));
            } else if (!newMap.containsKey(key)) {
                result.addEntry(new DiffEntry(keyPath, oldVal, null));
            } else if (oldVal == null || newVal == null) {
                result.addEntry(new DiffEntry(keyPath, oldVal, newVal));
            } else if (isPrimitiveOrWrapper(oldVal.getClass())) {
                if (!Objects.equals(oldVal, newVal)) {
                    result.addEntry(new DiffEntry(keyPath, oldVal, newVal));
                }
            } else {
                compareObjects(oldVal, newVal, keyPath, result);
            }
        }
    }

    private boolean isPrimitiveOrWrapper(Class<?> type) {
        return type.isPrimitive() || PRIMITIVE_TYPES.contains(type);
    }

    /**
     * Helper class to track pairs of objects being compared using identity equality.
     */
    private static final class ObjectPair {
        private final Object oldObj;
        private final Object newObj;

        ObjectPair(Object oldObj, Object newObj) {
            this.oldObj = oldObj;
            this.newObj = newObj;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ObjectPair)) return false;
            ObjectPair that = (ObjectPair) o;
            // Use identity comparison (==) instead of equals()
            return this.oldObj == that.oldObj && this.newObj == that.newObj;
        }

        @Override
        public int hashCode() {
            // Use System.identityHashCode for identity-based hashing
            return System.identityHashCode(oldObj) ^ System.identityHashCode(newObj);
        }
    }
}
