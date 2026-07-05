package fr.monkeynotes.mn.utils;

import fr.monkeynotes.mn.data.File2Process;
import fr.monkeynotes.mn.data.dto.DtoNamedEntity;
import fr.monkeynotes.mn.data.enums.NamedEntityVerb;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranscriptUtils {
    private static final Set<NamedEntityVerb> NON_HASH_HEADERS = Set.of(
            NamedEntityVerb.h_2, NamedEntityVerb.h_3, NamedEntityVerb.h_4, NamedEntityVerb.h_5, NamedEntityVerb.h_6
            );

    public record TranscriptTitle(String title, Optional<OffsetDateTime> documentTitleDate) {}

    public static TranscriptTitle identifyTitleDates(File2Process f2p) {
        // extract date from title (manually created)
        Pattern titleDatePattern = Pattern.compile("(\\d{6})\\s*-\\s*(.*)\\.pdf");
        Matcher m1 = titleDatePattern.matcher(f2p.getFileName());

        if (m1.find()) { //look for a date in doc name
            try {
                //TODO Date must be neutral (GMT) and then adapted to user according its setting
                LocalDate ld = LocalDate.parse(m1.group(1), DateTimeFormatter.ofPattern("yyMMdd"));
                ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("GMT+1"));
                return new TranscriptTitle(m1.group(2),
                        Optional.of(zdt.withZoneSameInstant(ZoneId.of("GMT+1")).toOffsetDateTime()));
            } catch (DateTimeParseException e) {
                //  todo
                //LOG.warn("Could not parse date in file title {}", m1.group(1), e);
            }

        } else if (f2p.getParentFolderName() != null) { //look for a date in parent folder name
            Matcher m2 = titleDatePattern.matcher(f2p.getParentFolderName());

            if (m2.find()) {
                try {
                    LocalDate ld = LocalDate.parse(m2.group(1), DateTimeFormatter.ofPattern("yyMMdd"));
                    ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("GMT+1"));
                    return new TranscriptTitle(m1.group(2),
                            Optional.of(zdt.withZoneSameInstant(ZoneId.of("GMT+1")).toOffsetDateTime()));
                } catch (DateTimeParseException e) {
                    //  todo
                    //LOG.warn("Could not parse date in parent title {}", m1.group(1), e);
                }
            }
        } else {
            Pattern titlePattern = Pattern.compile("(.*)\\.pdf");
            Matcher m3 = titlePattern.matcher(f2p.getFileName());
            if (m3.find()) {
                return new TranscriptTitle(m3.group(1), Optional.empty());
            }
        }
        return new TranscriptTitle(f2p.getFileName(), Optional.empty());
    }

    public static List<DtoNamedEntity> identifyNamedIdentities(String text) {
        if(text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        Pattern p = Pattern.compile(
                "(?<open>[<(\\[])\\s*(?<verb>(?:DG|DGN|T|DT|DE|DU|P|@|L|V|X|[1-6]))\\s*[:;]\\s*(?<value>(?:[^>)\\]]+?))\\s*(?<close>[>)\\]])",
                Pattern.CASE_INSENSITIVE
        );


        Matcher m = p.matcher(text);
        List<DtoNamedEntity> identities = new ArrayList<>();
        while (m.find()) {

            NamedEntityVerb verb = NamedEntityVerb.fromString(m.group(2));
            String value = m.group(3);
            if(value != null) {
                value = value.trim();
            }

            Pattern patternDate = Pattern.compile("\\d\\d/\\d\\d/\\d\\d");

            if(NON_HASH_HEADERS.contains(verb)) {
                switch (verb) {
                    case h_2:
                        verb = NamedEntityVerb.h2;
                        break;
                    case h_3:
                        verb = NamedEntityVerb.h3;
                        break;
                    case h_4:
                        verb = NamedEntityVerb.h4;
                        break;
                    case h_5:
                        verb = NamedEntityVerb.h5;
                        break;
                    case h_6:
                        verb = NamedEntityVerb.h6;
                        break;
                }
            }

            if(value != null && !value.isEmpty() && patternDate.matcher(value).matches()) {
                if (verb.equals(NamedEntityVerb.dateISO)) {
                    LocalDate ld = LocalDate.parse(value, DateTimeFormatter.ofPattern("yy/MM/dd"));
                    LocalDateTime ldt = ld.atStartOfDay();
                    Instant instant = ldt.toInstant(ZoneOffset.UTC);
                    value = DateTimeFormatter.ISO_INSTANT.format(instant);
                } else if (verb.equals(NamedEntityVerb.dateEU)) {
                    LocalDate ld = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yy"));
                    LocalDateTime ldt = ld.atStartOfDay();
                    Instant instant = ldt.toInstant(ZoneOffset.UTC);
                    value = DateTimeFormatter.ISO_INSTANT.format(instant);
                } else if (verb.equals(NamedEntityVerb.dateUS)) {
                    LocalDate ld = LocalDate.parse(value, DateTimeFormatter.ofPattern("MM/dd/yy"));
                    LocalDateTime ldt = ld.atStartOfDay();
                    Instant instant = ldt.toInstant(ZoneOffset.UTC);
                    value = DateTimeFormatter.ISO_INSTANT.format(instant);
                } else {
                    // TODO: Not a valid date
                }

            }
            identities.add(new DtoNamedEntity(verb, value, m.start(), m.end()));
        }
        return identities;
    }

    public static List<DtoNamedEntity> identifyTitles(String text) {
        if(text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        String regex = "(#+)\\s*(.*)";

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(text);
        List<DtoNamedEntity> identities = new ArrayList<>();
        while (m.find()) {
            NamedEntityVerb verb = NamedEntityVerb.fromString(m.group(1));
            String value = m.group(2).trim();
            identities.add(new DtoNamedEntity(verb, value, m.start(), m.end()));
        }
        return identities;
    }
}
