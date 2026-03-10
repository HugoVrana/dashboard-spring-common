package com.dashboard.common.utility.diff;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DiffComparerTest {

    // ==================== Null Handling ====================

    @Test
    void compare_bothNull_returnsNoDifferences() {
        DiffComparer<String> comparer = new DiffComparer<>(null, null);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
        assertTrue(result.getEntries().isEmpty());
    }

    @Test
    void compare_oldNull_returnsDifference() {
        DiffComparer<String> comparer = new DiffComparer<>(null, "new");
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("", result.getEntries().get(0).getFieldPath());
        assertNull(result.getEntries().get(0).getOldValue());
        assertEquals("new", result.getEntries().get(0).getNewValue());
    }

    @Test
    void compare_newNull_returnsDifference() {
        DiffComparer<String> comparer = new DiffComparer<>("old", null);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("old", result.getEntries().get(0).getOldValue());
        assertNull(result.getEntries().get(0).getNewValue());
    }

    // ==================== Primitive Types ====================

    @Test
    void compare_sameStrings_returnsNoDifferences() {
        DiffComparer<String> comparer = new DiffComparer<>("test", "test");
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }

    @Test
    void compare_differentStrings_returnsDifference() {
        DiffComparer<String> comparer = new DiffComparer<>("old", "new");
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
    }

    @Test
    void compare_sameIntegers_returnsNoDifferences() {
        DiffComparer<Integer> comparer = new DiffComparer<>(42, 42);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }

    @Test
    void compare_differentIntegers_returnsDifference() {
        DiffComparer<Integer> comparer = new DiffComparer<>(1, 2);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
    }

    // ==================== Enum Types ====================

    enum Status { PENDING, COMPLETED }

    @Test
    void compare_sameEnums_returnsNoDifferences() {
        DiffComparer<Status> comparer = new DiffComparer<>(Status.PENDING, Status.PENDING);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }

    @Test
    void compare_differentEnums_returnsDifference() {
        DiffComparer<Status> comparer = new DiffComparer<>(Status.PENDING, Status.COMPLETED);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(Status.PENDING, result.getEntries().get(0).getOldValue());
        assertEquals(Status.COMPLETED, result.getEntries().get(0).getNewValue());
    }

    // ==================== Simple Objects ====================

    static class SimpleObject {
        String name;
        int value;

        SimpleObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }

    @Test
    void compare_sameSimpleObjects_returnsNoDifferences() {
        SimpleObject old = new SimpleObject("test", 42);
        SimpleObject current = new SimpleObject("test", 42);

        DiffComparer<SimpleObject> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }

    @Test
    void compare_differentSimpleObjects_returnsDifferences() {
        SimpleObject old = new SimpleObject("old", 1);
        SimpleObject current = new SimpleObject("new", 2);

        DiffComparer<SimpleObject> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(2, result.getEntries().size());

        Map<String, DiffEntry> entryMap = new HashMap<>();
        for (DiffEntry entry : result.getEntries()) {
            entryMap.put(entry.getFieldPath(), entry);
        }

        assertEquals("old", entryMap.get("name").getOldValue());
        assertEquals("new", entryMap.get("name").getNewValue());
        assertEquals(1, entryMap.get("value").getOldValue());
        assertEquals(2, entryMap.get("value").getNewValue());
    }

    // ==================== Nested Objects ====================

    static class Address {
        String city;
        String street;

        Address(String city, String street) {
            this.city = city;
            this.street = street;
        }
    }

    static class Person {
        String name;
        Address address;

        Person(String name, Address address) {
            this.name = name;
            this.address = address;
        }
    }

    @Test
    void compare_nestedObjects_detectsNestedDifferences() {
        Person old = new Person("John", new Address("NYC", "5th Ave"));
        Person current = new Person("John", new Address("LA", "5th Ave"));

        DiffComparer<Person> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("address.city", result.getEntries().get(0).getFieldPath());
        assertEquals("NYC", result.getEntries().get(0).getOldValue());
        assertEquals("LA", result.getEntries().get(0).getNewValue());
    }

    @Test
    void compare_nestedObjectBecomesNull_returnsDifference() {
        Person old = new Person("John", new Address("NYC", "5th Ave"));
        Person current = new Person("John", null);

        DiffComparer<Person> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("address", result.getEntries().get(0).getFieldPath());
    }

    // ==================== Lists ====================

    static class WithList {
        List<String> items;

        WithList(List<String> items) {
            this.items = items;
        }
    }

    @Test
    void compare_sameLists_returnsNoDifferences() {
        WithList old = new WithList(Arrays.asList("a", "b", "c"));
        WithList current = new WithList(Arrays.asList("a", "b", "c"));

        DiffComparer<WithList> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }

    @Test
    void compare_differentListItems_returnsDifferences() {
        WithList old = new WithList(Arrays.asList("a", "b", "c"));
        WithList current = new WithList(Arrays.asList("a", "x", "c"));

        DiffComparer<WithList> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("items[1]", result.getEntries().get(0).getFieldPath());
        assertEquals("b", result.getEntries().get(0).getOldValue());
        assertEquals("x", result.getEntries().get(0).getNewValue());
    }

    @Test
    void compare_listWithAddedItem_returnsDifference() {
        WithList old = new WithList(Arrays.asList("a", "b"));
        WithList current = new WithList(Arrays.asList("a", "b", "c"));

        DiffComparer<WithList> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("items[2]", result.getEntries().get(0).getFieldPath());
        assertNull(result.getEntries().get(0).getOldValue());
        assertEquals("c", result.getEntries().get(0).getNewValue());
    }

    @Test
    void compare_listWithRemovedItem_returnsDifference() {
        WithList old = new WithList(Arrays.asList("a", "b", "c"));
        WithList current = new WithList(Arrays.asList("a", "b"));

        DiffComparer<WithList> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("items[2]", result.getEntries().get(0).getFieldPath());
        assertEquals("c", result.getEntries().get(0).getOldValue());
        assertNull(result.getEntries().get(0).getNewValue());
    }

    // ==================== Sets ====================

    static class WithSet {
        Set<String> tags;

        WithSet(Set<String> tags) {
            this.tags = tags;
        }
    }

    @Test
    void compare_sameSets_returnsNoDifferences() {
        WithSet old = new WithSet(new HashSet<>(Arrays.asList("a", "b")));
        WithSet current = new WithSet(new HashSet<>(Arrays.asList("a", "b")));

        DiffComparer<WithSet> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }

    @Test
    void compare_differentSets_returnsDifferences() {
        WithSet old = new WithSet(new HashSet<>(Arrays.asList("a", "b")));
        WithSet current = new WithSet(new HashSet<>(Arrays.asList("a", "c")));

        DiffComparer<WithSet> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(2, result.getEntries().size());
    }

    // ==================== Maps ====================

    static class WithMap {
        Map<String, Integer> scores;

        WithMap(Map<String, Integer> scores) {
            this.scores = scores;
        }
    }

    @Test
    void compare_sameMaps_returnsNoDifferences() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);

        WithMap old = new WithMap(map1);
        WithMap current = new WithMap(map2);

        DiffComparer<WithMap> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }

    @Test
    void compare_differentMapValues_returnsDifferences() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 2);

        WithMap old = new WithMap(map1);
        WithMap current = new WithMap(map2);

        DiffComparer<WithMap> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals("scores[a]", result.getEntries().get(0).getFieldPath());
    }

    @Test
    void compare_mapWithNewKey_returnsDifference() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);

        WithMap old = new WithMap(map1);
        WithMap current = new WithMap(map2);

        DiffComparer<WithMap> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("scores[b]", result.getEntries().get(0).getFieldPath());
        assertNull(result.getEntries().get(0).getOldValue());
        assertEquals(2, result.getEntries().get(0).getNewValue());
    }

    // ==================== Circular References ====================

    static class Node {
        String name;
        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    @Test
    void compare_circularReference_doesNotStackOverflow() {
        Node oldA = new Node("A");
        Node oldB = new Node("B");
        oldA.next = oldB;
        oldB.next = oldA; // circular

        Node newA = new Node("A");
        Node newB = new Node("B");
        newA.next = newB;
        newB.next = newA; // circular

        DiffComparer<Node> comparer = new DiffComparer<>(oldA, newA);

        // Should complete without StackOverflowError
        assertDoesNotThrow(() -> comparer.compare());
    }

    @Test
    void compare_selfReference_doesNotStackOverflow() {
        Node node1 = new Node("A");
        node1.next = node1; // self-reference

        Node node2 = new Node("A");
        node2.next = node2; // self-reference

        DiffComparer<Node> comparer = new DiffComparer<>(node1, node2);

        assertDoesNotThrow(() -> comparer.compare());
    }

    @Test
    void compare_circularWithDifference_detectsDifference() {
        Node oldA = new Node("A");
        Node oldB = new Node("B");
        oldA.next = oldB;
        oldB.next = oldA;

        Node newA = new Node("A");
        Node newB = new Node("C"); // Different name
        newA.next = newB;
        newB.next = newA;

        DiffComparer<Node> comparer = new DiffComparer<>(oldA, newA);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        boolean foundNameDiff = result.getEntries().stream()
                .anyMatch(e -> e.getFieldPath().contains("name") && "B".equals(e.getOldValue()) && "C".equals(e.getNewValue()));
        assertTrue(foundNameDiff);
    }

    // ==================== @DiffIgnore ====================

    static class WithIgnoredField {
        String name;
        @DiffIgnore
        String password;

        WithIgnoredField(String name, String password) {
            this.name = name;
            this.password = password;
        }
    }

    @Test
    void compare_ignoredFieldDiffers_notReported() {
        WithIgnoredField old = new WithIgnoredField("user", "secret1");
        WithIgnoredField current = new WithIgnoredField("user", "secret2");

        DiffComparer<WithIgnoredField> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }

    @Test
    void compare_nonIgnoredFieldDiffers_reported() {
        WithIgnoredField old = new WithIgnoredField("user1", "secret");
        WithIgnoredField current = new WithIgnoredField("user2", "secret");

        DiffComparer<WithIgnoredField> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("name", result.getEntries().get(0).getFieldPath());
    }

    // ==================== Inheritance ====================

    static class BaseEntity {
        String id;

        BaseEntity(String id) {
            this.id = id;
        }
    }

    static class DerivedEntity extends BaseEntity {
        String name;

        DerivedEntity(String id, String name) {
            super(id);
            this.name = name;
        }
    }

    @Test
    void compare_inheritedFields_detected() {
        DerivedEntity old = new DerivedEntity("1", "test");
        DerivedEntity current = new DerivedEntity("2", "test");

        DiffComparer<DerivedEntity> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals(1, result.getEntries().size());
        assertEquals("id", result.getEntries().get(0).getFieldPath());
    }

    // ==================== Null Fields ====================

    @Test
    void compare_fieldBecomesNull_reported() {
        SimpleObject old = new SimpleObject("test", 1);
        SimpleObject current = new SimpleObject(null, 1);

        DiffComparer<SimpleObject> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals("name", result.getEntries().get(0).getFieldPath());
        assertEquals("test", result.getEntries().get(0).getOldValue());
        assertNull(result.getEntries().get(0).getNewValue());
    }

    @Test
    void compare_fieldBecomesNonNull_reported() {
        SimpleObject old = new SimpleObject(null, 1);
        SimpleObject current = new SimpleObject("test", 1);

        DiffComparer<SimpleObject> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertTrue(result.hasDifferences());
        assertEquals("name", result.getEntries().get(0).getFieldPath());
        assertNull(result.getEntries().get(0).getOldValue());
        assertEquals("test", result.getEntries().get(0).getNewValue());
    }

    @Test
    void compare_bothFieldsNull_notReported() {
        SimpleObject old = new SimpleObject(null, 1);
        SimpleObject current = new SimpleObject(null, 1);

        DiffComparer<SimpleObject> comparer = new DiffComparer<>(old, current);
        DiffResult result = comparer.compare();

        assertFalse(result.hasDifferences());
    }
}
