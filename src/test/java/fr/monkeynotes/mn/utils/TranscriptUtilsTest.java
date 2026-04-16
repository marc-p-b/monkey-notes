package fr.monkeynotes.mn.utils;

import fr.monkeynotes.mn.data.dto.DtoNamedEntity;
import fr.monkeynotes.mn.data.enums.NamedEntityVerb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TranscriptUtilsTest {

    @Test
    void identifyNamedIdentities_withNullText_returnsEmptyList() {
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void identifyNamedIdentities_withEmptyText_returnsEmptyList() {
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities("");
        assertTrue(result.isEmpty());
    }

    // Test all marker types: [] <> ()
    static Stream<Arguments> markerVariations() {
        return Stream.of(
                Arguments.of("[T:myTag]", "square brackets"),
                Arguments.of("<T:myTag>", "angle brackets"),
                Arguments.of("(T:myTag)", "parentheses")
        );
    }

    @ParameterizedTest(name = "marker {1}")
    @MethodSource("markerVariations")
    void identifyNamedIdentities_withDifferentMarkers_parsesCorrectly(String input, String description) {
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(input);

        assertEquals(1, result.size());
        assertEquals(NamedEntityVerb.tag, result.get(0).getVerb());
        assertEquals("myTag", result.get(0).getValue());
    }

    // Test spacing variations
    static Stream<Arguments> spacingVariations() {
        return Stream.of(
                Arguments.of("[T:value]", "no spaces"),
                Arguments.of("[ T:value]", "space after open"),
                Arguments.of("[T :value]", "space before colon"),
                Arguments.of("[T: value]", "space after colon"),
                Arguments.of("[T:value ]", "space before close"),
                Arguments.of("[ T : value ]", "spaces everywhere"),
                Arguments.of("[  T  :  value  ]", "multiple spaces")
        );
    }

    @ParameterizedTest(name = "spacing: {1}")
    @MethodSource("spacingVariations")
    void identifyNamedIdentities_withDifferentSpacing_parsesCorrectly(String input, String description) {
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(input);

        assertEquals(1, result.size());
        assertEquals(NamedEntityVerb.tag, result.get(0).getVerb());
        assertEquals("value", result.get(0).getValue());
    }

    // Test all verb types with square brackets
    static Stream<Arguments> verbVariations() {
        return Stream.of(
                Arguments.of("[T:val]", NamedEntityVerb.tag, "tag"),
                Arguments.of("[P:val]", NamedEntityVerb.person, "person"),
                Arguments.of("[@:val]", NamedEntityVerb.email, "email"),
                Arguments.of("[L:val]", NamedEntityVerb.link, "link"),
                Arguments.of("[V:val]", NamedEntityVerb.checked, "checked"),
                Arguments.of("[X:val]", NamedEntityVerb.unchecked, "unchecked"),
                Arguments.of("[DG:val]", NamedEntityVerb.diagram, "diagram"),
                Arguments.of("[DGN:val]", NamedEntityVerb.diagramNextPage, "diagram next")
        );
    }

    @ParameterizedTest(name = "verb: {2}")
    @MethodSource("verbVariations")
    void identifyNamedIdentities_withDifferentVerbs_parsesCorrectly(String input, NamedEntityVerb expectedVerb, String description) {
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(input);

        assertEquals(1, result.size());
        assertEquals(expectedVerb, result.get(0).getVerb());
        assertEquals("val", result.get(0).getValue());
    }

    // Test case insensitivity
    static Stream<Arguments> caseVariations() {
        return Stream.of(
                Arguments.of("[t:value]", "lowercase"),
                Arguments.of("[T:value]", "uppercase"),
                Arguments.of("[p:John]", "lowercase person"),
                Arguments.of("[P:John]", "uppercase person")
        );
    }

    @ParameterizedTest(name = "case: {1}")
    @MethodSource("caseVariations")
    void identifyNamedIdentities_caseInsensitive_parsesCorrectly(String input, String description) {
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(input);

        assertEquals(1, result.size());
        assertNotNull(result.get(0).getVerb());
    }

    // Test combined marker + spacing variations
    static Stream<Arguments> markerAndSpacingCombinations() {
        return Stream.of(
                // Square brackets
                Arguments.of("[T:tag]", NamedEntityVerb.tag, "tag"),
                Arguments.of("[ T : tag ]", NamedEntityVerb.tag, "tag"),
                // Angle brackets
                Arguments.of("<T:tag>", NamedEntityVerb.tag, "tag"),
                Arguments.of("< T : tag >", NamedEntityVerb.tag, "tag"),
                // Parentheses
                Arguments.of("(T:tag)", NamedEntityVerb.tag, "tag"),
                Arguments.of("( T : tag )", NamedEntityVerb.tag, "tag"),
                // Person with spaces in value
                Arguments.of("[P:John Doe]", NamedEntityVerb.person, "John Doe"),
                Arguments.of("<P:John Doe>", NamedEntityVerb.person, "John Doe"),
                Arguments.of("(P:John Doe)", NamedEntityVerb.person, "John Doe")
        );
    }

    @ParameterizedTest(name = "input: {0}")
    @MethodSource("markerAndSpacingCombinations")
    void identifyNamedIdentities_markerAndSpacingCombinations_parsesCorrectly(String input, NamedEntityVerb expectedVerb, String expectedValue) {
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(input);

        assertEquals(1, result.size());
        assertEquals(expectedVerb, result.get(0).getVerb());
        assertEquals(expectedValue, result.get(0).getValue());
    }

    // Test semicolon as separator (alternative to colon)
    static Stream<Arguments> separatorVariations() {
        return Stream.of(
                Arguments.of("[T:value]", "colon"),
                Arguments.of("[T;value]", "semicolon")
        );
    }

    @ParameterizedTest(name = "separator: {1}")
    @MethodSource("separatorVariations")
    void identifyNamedIdentities_withDifferentSeparators_parsesCorrectly(String input, String description) {
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(input);

        assertEquals(1, result.size());
        assertEquals(NamedEntityVerb.tag, result.get(0).getVerb());
        assertEquals("value", result.get(0).getValue());
    }

    @Test
    void identifyNamedIdentities_withMultipleEntities_returnsAll() {
        String text = "Text [T:tag1] more <P:person1> end (V:done)";
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(text);

        assertEquals(3, result.size());
        assertEquals(NamedEntityVerb.tag, result.get(0).getVerb());
        assertEquals(NamedEntityVerb.person, result.get(1).getVerb());
        assertEquals(NamedEntityVerb.checked, result.get(2).getVerb());
    }

    @Test
    void identifyNamedIdentities_recordsCorrectPositions() {
        String text = "prefix [T:tag] suffix";
        List<DtoNamedEntity> result = TranscriptUtils.identifyNamedIdentities(text);

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getStart());
        assertEquals(14, result.get(0).getEnd());
    }
}