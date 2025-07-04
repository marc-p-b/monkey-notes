package net.kprod.dsb;

import net.kprod.dsb.data.File2Process;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static OffsetDateTime identifyDates(File2Process f2p) {
        // extract date from title (manually created)
        Pattern titleDatePattern = Pattern.compile("(\\d{6})");
        Matcher m1 = titleDatePattern.matcher(f2p.getFileName());

        OffsetDateTime documentTitleDate = null;
        try {
            if (m1.find()) {
                try {
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

    public static void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;

        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file); // Delete each file
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir); // Delete the directory itself after contents
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
