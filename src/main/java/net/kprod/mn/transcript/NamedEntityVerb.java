package net.kprod.mn.transcript;

public enum NamedEntityVerb {
    tag("T"),
    dateUs("DU"),
    dateIntl("D"),
    person("P"),
    email("@"),
    schema("S"),
    refSchema("SREF"),
    noTranscript("XT"),
    unknown("");

    private String command;
    NamedEntityVerb(String command) {
        this.command = command;
    }

    public static NamedEntityVerb fromString(String command) {
        for (NamedEntityVerb verb : NamedEntityVerb.values()) {
            if (verb.command.equalsIgnoreCase(command)) {
                return verb;
            }
        }
        //throw new IllegalArgumentException("Unknown command: " + command);
        return unknown;
    }
}