package net.kprod.dsb.utils;

import net.kprod.dsb.data.File2Process;
import net.kprod.dsb.transcript.NamedEntity;

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

    public static List<NamedEntity> identifyCommands(String text) {
        String regex = "\\[/((?i)(XT|T|D|DU|P|@|SREF|S))(?:\\s+([^\\]]+))?\\]";
        Pattern p = Pattern.compile(regex);

        Matcher m = p.matcher(text);
        List<NamedEntity> commands = new ArrayList<>();
        while (m.find()) {

            NamedEntity.NamedEntityVerb verb = NamedEntity.NamedEntityVerb.fromString(m.group(1));
            String value = m.group(3);

            Pattern patternDate = Pattern.compile("\\d\\d/\\d\\d/\\d\\d");

            if(value != null && !value.isEmpty() && patternDate.matcher(value).matches()) {
                if (verb.equals(NamedEntity.NamedEntityVerb.dateUs)) {
                    LocalDate ld = LocalDate.parse(value, DateTimeFormatter.ofPattern("yy/MM/dd"));
                    LocalDateTime ldt = ld.atStartOfDay();
                    Instant instant = ldt.toInstant(ZoneOffset.UTC);
                    value = DateTimeFormatter.ISO_INSTANT.format(instant);
                } else if (verb.equals(NamedEntity.NamedEntityVerb.dateIntl)) {
                    LocalDate ld = LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yy"));
                    LocalDateTime ldt = ld.atStartOfDay();
                    Instant instant = ldt.toInstant(ZoneOffset.UTC);
                    value = DateTimeFormatter.ISO_INSTANT.format(instant);
                }
            }
            commands.add(new NamedEntity(verb, value, m.start(), m.end()));

        }
        return commands;
    }
}
