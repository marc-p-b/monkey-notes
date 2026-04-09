package fr.monkeynotes.mn.data.enums;

public enum NamedEntityVerb {

    /*
        T tag
        D date (YY/MM/DD)
        P person
        @ email
        L link
        SN schema next page
        S schema current page
        V checked box
        X unchecked box
     */


    tag("T", true),
    dateUs("DU", false),
    dateIntl("D", false),
    person("P", true),
    email("@", true),
    link("L", false),
    schema("S", false),
    refSchema("SREF", false),
    refSchema2("SN", false),
    noTranscript("XT", false),
    checked("V", false),
    unchecked("X", false),
    h2("#", false),
    h3("##", false),
    h4("###", false),
    h5("####", false),
    h6("#####", false),
    unknown("", false);

    private String name;
    private boolean indexable;

    NamedEntityVerb(String command, boolean indexable) {
        this.name = command;
        this.indexable = indexable;
    }

    public static NamedEntityVerb fromString(String command) {
        for (NamedEntityVerb verb : NamedEntityVerb.values()) {
            if (verb.name.equalsIgnoreCase(command)) {
                return verb;
            }
        }
        //throw new IllegalArgumentException("Unknown command: " + command);
        return unknown;
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