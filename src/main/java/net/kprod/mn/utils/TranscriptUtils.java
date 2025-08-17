package net.kprod.mn.utils;

import net.kprod.mn.data.File2Process;
import net.kprod.mn.data.dto.DtoNamedEntity;
import net.kprod.mn.transcript.NamedEntityVerb;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranscriptUtils {
    public static OffsetDateTime identifyDates(File2Process f2p) {
        // extract date from title (manually created)
        Pattern titleDatePattern = Pattern.compile("(\\d{6})");
        Matcher m1 = titleDatePattern.matcher(f2p.getFileName());

        OffsetDateTime documentTitleDate = null;
        try {
            if (m1.find()) {
                try {
                    //TODO Date must be neutral (GMT) and then adapted to user according its setting
                    LocalDate ld = LocalDate.parse(m1.group(1), DateTimeFormatter.ofPattern("yyMMdd"));
                    ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("GMT+1"));
                    documentTitleDate = zdt.withZoneSameInstant(ZoneId.of("GMT+1")).toOffsetDateTime();
                } catch (DateTimeParseException e) {
                    //  todo
                    //LOG.warn("Could not parse date in file title {}", m1.group(1), e);
                }
            } else if (f2p.getParentFolderName() != null) {
                Matcher m2 = titleDatePattern.matcher(f2p.getParentFolderName());

                if (m2.find()) {
                    try {
                        LocalDate ld = LocalDate.parse(m2.group(1), DateTimeFormatter.ofPattern("yyMMdd"));
                        ZonedDateTime zdt = ld.atStartOfDay(ZoneId.of("GMT+1"));
                        documentTitleDate = zdt.withZoneSameInstant(ZoneId.of("GMT+1")).toOffsetDateTime();
                    } catch (DateTimeParseException e) {
                        //  todo
                        //LOG.warn("Could not parse date in parent title {}", m1.group(1), e);
                    }
                }
            }
        } catch (Exception e) {
            //todo why ??
            //LOG.error("Could not identify date in file {}", f2p.getFileId(), e);
        }
        return documentTitleDate;
    }

    public static List<DtoNamedEntity> identifyNamedIdentities(String text) {
        if(text == null || text.isEmpty()) {
            return new ArrayList<>();
        }

        //String regex = "\\[\\s*\\/\\s*((?i)(XT|T|D|DU|P|@|SREF|S))(?:\\s+([^\\]]+))?\\]";

        String regex = "\\[\\s*((?i)(XT|T|D|DU|P|@|SREF|S|L))(?:\\s*:\\s*([^\\]]+))?\\]";
        //format [VERB:value] where VERB is XT, T, D, DU, P, @, SREF, S, L (upper or lower)
        //with spaces or not :
        // [ t: myTAG]
        // [T : myTAG ]

        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(text);
        List<DtoNamedEntity> identities = new ArrayList<>();
        while (m.find()) {

            NamedEntityVerb verb = NamedEntityVerb.fromString(m.group(1));
            String value = m.group(3).trim();

            Pattern patternDate = Pattern.compile("\\d\\d/\\d\\d/\\d\\d");

            if(value != null && !value.isEmpty() && patternDate.matcher(value).matches()) {
                if (verb.equals(NamedEntityVerb.dateUs)) {
                    LocalDate ld = LocalDate.parse(value, DateTimeFormatter.ofPattern("yy/MM/dd"));
                    LocalDateTime ldt = ld.atStartOfDay();
                    Instant instant = ldt.toInstant(ZoneOffset.UTC);
                    value = DateTimeFormatter.ISO_INSTANT.format(instant);
                } else if (verb.equals(NamedEntityVerb.dateIntl)) {
                    LocalDate ld = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yy"));
                    LocalDateTime ldt = ld.atStartOfDay();
                    Instant instant = ldt.toInstant(ZoneOffset.UTC);
                    value = DateTimeFormatter.ISO_INSTANT.format(instant);
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
