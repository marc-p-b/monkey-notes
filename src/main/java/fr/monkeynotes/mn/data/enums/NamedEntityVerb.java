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
        V checked box
        X unchecked box
     */


    tag("T", true),
    dateISO("DT", false),
    dateEU("DE", false),
    dateUS("DU", false),
    person("P", true),
    email("@", true),
    link("L", false),
    diagram("DG", false),
    diagramNextPage("DGN", false),
    checked("V", false),
    unchecked("X", false),
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