package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.data.dto.DtoTranscript;
import fr.monkeynotes.mn.data.dto.DtoTranscriptPage;
import fr.monkeynotes.mn.data.enums.PreferenceKey;
import fr.monkeynotes.mn.service.ImageService;
import fr.monkeynotes.mn.service.PdfService;
import fr.monkeynotes.mn.service.PreferencesService;
import fr.monkeynotes.mn.service.UtilsService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class PdfServiceImpl implements PdfService {
    private Logger LOG = LoggerFactory.getLogger(PdfService.class);

    public static final int PDF2IMAGE_DPI = 72;
    public static final ImageType PDF2IMAGE_IMAGE_TYPE = ImageType.GRAY;

    @Value("${app.defaults.image.resize.max.width}")
    private int resizeMaxWidth;

    @Value("${app.defaults.image.resize.max.height}")
    private int resizeMaxHeight;

    @Value("${app.defaults.image.crop.padding}")
    private int cropPadding;

    @Autowired
    private UtilsService utilsService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private PreferencesService preferencesService;

    @Override
    public List<URL> pdf2Images(String username, String fileId, File sourceFile){
        LOG.info("Converting {} to images", sourceFile);

        List<URL> listImages = new ArrayList<>();
        try {
            //listImages = new ArrayList<>();

            if (sourceFile.exists()) {
                PDDocument document = Loader.loadPDF(sourceFile);
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();
                for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, PDF2IMAGE_DPI, PDF2IMAGE_IMAGE_TYPE);

                    //String md5 = imageMd5(image);

                    LOG.info("original image {}x{}", image.getWidth(), image.getHeight());
                    BufferedImage resizedImage = imageService.resizeImage(resizeMaxWidth, resizeMaxHeight, image);
                    LOG.info("resized image {}x{}", resizedImage.getWidth(), resizedImage.getHeight());

                    if(preferencesService.getPreferenceAsBoolean(PreferenceKey.cropImage)) {
                        resizedImage = imageService.cropToContent(resizedImage, cropPadding);
                        LOG.info("cropped image {}x{}", resizedImage.getWidth(), resizedImage.getHeight());
                    }

                    Path pathPage = utilsService.tempImagePath(fileId, pageNumber);
                    java.io.File outputFile = pathPage.toFile();

                    ImageIO.write(resizedImage, "png", outputFile);
                    listImages.add(utilsService.tempImageURL(username, fileId, pageNumber));
                    LOG.info("Page {} converted to image {}", pageNumber, pathPage);
                }
                document.close();
            } else {
                LOG.error("PDF not found: {}", sourceFile);
            }
            return listImages;

        } catch (Exception e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

    @Override
    public java.io.File createTranscriptPdf(String fileId, List<DtoTranscript> listDtoTranscript) throws IOException {
        Map<String, String> replacements = new HashMap<>();
        replacements.put("\u2713", "[V]");
        replacements.put("\u2717", "[X]");

        PDDocument doc = null;
        java.io.File file = null;
        try
        {
            doc = new PDDocument();
            PDPage page = new PDPage();
            doc.addPage(page);
            PDPageContentStream contentStream = new PDPageContentStream(doc, page);

            PDFont pdfFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);;
            float fontSize = 12;
            float leading = 1.5f * fontSize;

            PDRectangle mediabox = page.getMediaBox();
            float margin = 20;
            float pageWidth = mediabox.getWidth() - 2 * margin;
            //float pageHeight = mediabox.getHeight() - 2 * margin;
            float startX = mediabox.getLowerLeftX() + margin;
            float startY = mediabox.getUpperRightY() - margin;


            List<String> lines = new ArrayList<>();
            Map<Integer, List<String>> pages = new HashMap<>();

            int lastSpace = -1;
            float lineWidth = 0;
            //float lineHeight = (pdfFont.getFontDescriptor().getCapHeight()) / 1000 * fontSize;

            String subString;
            int pageNumber = 1;


            int docNumber = 0;
            for(DtoTranscript dtoTranscript : listDtoTranscript) {

                //Doc title
                StringBuilder sbDocTitle = new StringBuilder()
                        .append(dtoTranscript.getTitle())
                        .append(" (")
                        .append(dtoTranscript.getPages().size())
                        .append(" pages)")
                        .append(" date ").append(dtoTranscript.getDocumented_at());

                lines.add(sbDocTitle.toString());

                for (DtoTranscriptPage transcriptPage : dtoTranscript.getPages()) {
                    //document pages
                    String text = replaceCars4Pdf(transcriptPage, replacements);

                    while (text.length() > 0) {
                        int nlIndex = text.indexOf('\n');
                        int spaceIndex = text.indexOf(' ', lastSpace + 1);

                        if (spaceIndex < 0) {
                            spaceIndex = text.length();
                        }

                        subString = text.substring(0, spaceIndex > nlIndex ? spaceIndex : nlIndex);
                        lineWidth = fontSize * getStringWidth(pdfFont, subString) / 1000;


                        if (nlIndex < spaceIndex && nlIndex != -1) {
                            subString = text.substring(0, nlIndex);

                            lines.add(subString.replaceAll("\\p{C}", ""));
                            text = text.substring(nlIndex).trim();
                            //System.out.printf("> %s\n", subString);
                        } else if (lineWidth > pageWidth) {
                            if (lastSpace < 0) {
                                lastSpace = spaceIndex;
                            }
                            subString = text.substring(0, lastSpace);

                            lines.add(subString.replaceAll("\\p{C}", ""));
                            text = text.substring(lastSpace).trim();
                            //System.out.printf("> %s\n", subString);
                            lastSpace = -1;
                        } else if (spaceIndex == text.length()) {
                            lines.add(text.replaceAll("\\p{C}", ""));
                            //System.out.printf(">%s\n", text);
                            text = "";
                        } else {
                            lastSpace = spaceIndex;
                        }

                        //if(lineHeight * lines.size() >= pageHeight) {
                        if (lines.size() > 42) {
                            pages.put(pageNumber, new ArrayList<>(lines));
                            pageNumber++;
                            lines.clear();
                        }
                    }
                }
                if(docNumber++ < listDtoTranscript.size()-1) {
                    //add page break
                    pages.put(pageNumber, new ArrayList<>(lines));
                    pageNumber++;
                    lines.clear();
                }
            }
            pages.put(pageNumber, new ArrayList<>(lines));

            contentStream.beginText();
            contentStream.setFont(pdfFont, fontSize);
            contentStream.newLineAtOffset(startX, startY);

            for(Map.Entry<Integer, List<String>> entry : pages.entrySet()) {

                for (String line : entry.getValue()) {
                    addContentToStream(line, contentStream);
                    contentStream.newLineAtOffset(0, -leading);
                }
                contentStream.endText();
                contentStream.close();

                if(pages.containsKey(entry.getKey() + 1)) {

                    page = new PDPage();
                    contentStream = new PDPageContentStream(doc, page);
                    doc.addPage(page);

                    contentStream.beginText();
                    contentStream.setFont(pdfFont, fontSize);
                    contentStream.newLineAtOffset(startX, startY);
                }

            }
            //contentStream.endText();
            //contentStream.close();

            //todo here use a stream
            file = new java.io.File("/tmp/",  fileId + ".pdf");
            doc.save(file);
        }
        finally {
            if (doc != null) {
                doc.close();
            }
        }
        return file;
    }

    @NotNull
    private static String replaceCars4Pdf(DtoTranscriptPage transcriptPage, Map<String, String> replacements) {

        Pattern replaceUnsafePattern = Pattern.compile(
                "[\\p{Cc}&&[^\\r\\n\\t]]" +   // control chars (sauf CR/LF/TAB)
                        "|[\\p{Cf}\\p{Co}\\p{Cn}\\p{Cs}]" + // format, PUA, unassigned, surrogates
                        "|[\\uFFFD\\uFFFE\\uFFFF]" +   // replacement char, BOM, non-chars
                        "|[\\uE000-\\uF8FF]"            // PUA BMP
        );

        String text = transcriptPage.getTranscript();

        if (text != null) {
            text = replaceUnsafePattern.matcher(text).replaceAll("(!)");
        }


//        String text =
//                .replaceAll("[\\p{Cc}&&[^\\r\\n]]|[\\p{Cf}\\p{Co}\\p{Cn}]", "(!)");
        for(Map.Entry<String, String> replacement : replacements.entrySet()) {
            text = text.replaceAll(replacement.getKey(), replacement.getValue());
        }
        return text;
    }

    private static void addContentToStream(String line, PDPageContentStream contentStream) throws IOException {
        try {
            contentStream.showText(line);
        } catch (IllegalArgumentException e) {
            contentStream.showText("ERROR");
        }
    }

    private static float getStringWidth(PDFont pdfFont, String subString) throws IOException {
        try {
            return pdfFont.getStringWidth(subString.replaceAll("\\p{C}", ""));
        } catch (IllegalArgumentException e) {
            return 20;
        }
    }
}
