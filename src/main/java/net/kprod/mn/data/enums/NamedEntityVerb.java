package net.kprod.mn.data.enums;

public enum NamedEntityVerb {
    tag("T", true),
    dateUs("DU", true),
    dateIntl("D", true),
    person("P", true),
    email("@", true),
    schema("S", true),
    refSchema("SREF", true),
    noTranscript("XT", false),
    link("L", true),
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
}