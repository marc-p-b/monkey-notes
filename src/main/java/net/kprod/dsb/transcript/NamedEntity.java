package net.kprod.dsb.transcript;

public class NamedEntity {
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

        private NamedEntityVerb verb;
        private String value;
        private int start;
        private int end;

        public NamedEntity(NamedEntityVerb verb, String value, Integer start, Integer end) {
            this.verb = verb;
            this.value = value;
            this.start = start;
            this.end = end;
        }

        public NamedEntityVerb getVerb() {
            return verb;
        }

        public String getValue() {
            return value;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

    @Override
    public String toString() {
        return "TranscriptCommand{" +
                "verb=" + verb +
                ", value='" + value + '\'' +
                ", start=" + start +
                ", end=" + end +
                '}';
    }
}