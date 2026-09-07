package fr.monkeynotes.mn.data.enums;

public enum NamedEntityVerb {

    /*
        T tag
        DI date iso (YY/MM/DD)
        DE date EU (DD/MM/YY)
        DU date US (MM/DD/YY)
        P person
        @ email
        L link
        DGN schema next page
        DG schema current page
        V checked box    (or any glyph in Aliases.CHECKED)
        X unchecked box  (or any glyph in Aliases.UNCHECKED)
     */

    tag("T", true),
    dateISO("DT", false),
    dateEU("DE", false),
    dateUS("DU", false),
    person("P", true),
    email("@", true),
    link("L", false),
    diagram("DG", true),
    diagramNextPage("DGN", true),
    checked("V", false, Aliases.CHECKED),
    unchecked("X", false, Aliases.UNCHECKED),
    h2("#", false),
    h3("##", false),
    h4("###", false),
    h5("####", false),
    h6("#####", false),
    h_2("1", false),
    h_3("2", false),
    h_4("3", false),
    h_5("4", false),
    h_6("5", false),
    unknown("", false),
    refSchema2_DEL("", false),
    noTranscript_DEL("", false);

    /**
     * Glyphs accepted in place of a verb's letter. OCR of a handwritten tick returns a symbol far
     * more often than the letter V, and both readings mean the same thing — so these are aliases of
     * one verb rather than verbs of their own, and nothing downstream has to know which form was
     * written (the Lucene index and the frontend renderer both switch on the verb name).
     * <p>
     * They sit in a nested holder because an enum constant's constructor may not reference a static
     * field of its own enum, and are written as \\u escapes because the build declares no source
     * encoding — a literal glyph here would depend on the platform default.
     */
    private static final class Aliases {
        //check mark, heavy check mark, ballot box with check, white heavy check mark, and the
        //square root sign — not a tick by Unicode, but what OCR commonly returns for a handwritten
        //one, and it has no other meaning in this position
        static final String CHECKED = "\u2713\u2714\u2611\u2705\u221A";
        //ballot x, heavy ballot x, multiplication x, heavy multiplication x, ballot box with x,
        //cross mark, and the empty ballot box — an unticked box is exactly what X means here
        static final String UNCHECKED = "\u2717\u2718\u2715\u2716\u2612\u274C\u2610";
    }

    private String name;
    private boolean indexable;
    //every accepted glyph for this verb, concatenated; empty for a verb that has none
    private String aliases;

    NamedEntityVerb(String command, boolean indexable) {
        this(command, indexable, "");
    }

    NamedEntityVerb(String command, boolean indexable, String aliases) {
        this.name = command;
        this.indexable = indexable;
        this.aliases = aliases;
    }

    public static NamedEntityVerb fromString(String command) {
        for (NamedEntityVerb verb : NamedEntityVerb.values()) {
            if (verb.name.equalsIgnoreCase(command)) {
                return verb;
            }
        }
        //a glyph written in place of the letter, e.g. <\u2713 : done> for <V : done>
        if (command != null && command.length() == 1) {
            for (NamedEntityVerb verb : NamedEntityVerb.values()) {
                if (verb.aliases.indexOf(command.charAt(0)) >= 0) {
                    return verb;
                }
            }
        }
        //throw new IllegalArgumentException("Unknown command: " + command);
        return unknown;
    }

    /**
     * This verb's alias glyphs, concatenated — the body of a regex character class, same contract
     * as {@link #aliasChars()}. Use this rather than the all-verbs version whenever a pattern is
     * about specific verbs, or it will silently start accepting an alias added to an unrelated one.
     */
    public String aliases() {
        return aliases;
    }

    /**
     * Every alias glyph of every verb, meant as the body of a regex character class so the
     * transcript pattern is built from this enum instead of repeating the list. Callers must wrap
     * it in \Q...\E (legal inside a character class in java.util.regex): none of the glyphs is
     * special today, but a future alias could be.
     */
    public static String aliasChars() {
        StringBuilder chars = new StringBuilder();
        for (NamedEntityVerb verb : values()) {
            chars.append(verb.aliases);
        }
        return chars.toString();
    }

    public boolean isIndexable() {
        return indexable;
    }

    static public boolean isToc(NamedEntityVerb verb) {
        switch (verb) {
            case h2, h3, h4, h5, h6-> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }


}