package net.kprod.mn.service.impl;

import net.kprod.mn.data.dto.DtoTranscript;
import net.kprod.mn.data.dto.DtoTranscriptPage;
import net.kprod.mn.service.PdfService;
import net.kprod.mn.service.UtilsService;
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
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfServiceImpl implements PdfService {
    public static final int RESIZE_IMAGE_MAX_WIDTH = 1200;
    public static final int RESIZE_IMAGE_MAX_HEIGHT = 1000;
    private Logger LOG = LoggerFactory.getLogger(PdfService.class);

    public static final int PDF2IMAGE_DPI = 72;
    public static final ImageType PDF2IMAGE_IMAGE_TYPE = ImageType.GRAY;

    @Autowired
    private UtilsService utilsService;

    private BufferedImage resizeImage(BufferedImage originalImage) throws Exception {
        int maxWidth = RESIZE_IMAGE_MAX_WIDTH;
        int maxHeight = RESIZE_IMAGE_MAX_HEIGHT;

        // Calculate new dimensions while preserving aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        // Resize image
        Image tmp = originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return resized;
    }

    @Override
    public List<URL> pdf2Images(String username, String fileId, File sourceFile){//, Path targetDir) {
        LOG.info("Converting {} to images", sourceFile);

        List<URL> listImages = null;
        try {
            listImages = new ArrayList<>();

            if (sourceFile.exists()) {
                PDDocument document = Loader.loadPDF(sourceFile);
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                int pageCount = document.getNumberOfPages();
                for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, PDF2IMAGE_DPI, PDF2IMAGE_IMAGE_TYPE);

                    //String md5 = imageMd5(image);

                    BufferedImage resizedImage = resizeImage(image);

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

    //seems not reliable between 2 pdf conversions..
//    private String imageMd5(BufferedImage image) throws NoSuchAlgorithmException {
//        MessageDigest md = MessageDigest.getInstance("MD5");
//
//        if (image.getRaster().getDataBuffer() instanceof DataBufferByte) {
//            byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
//            md.update(pixels);
//        } else {
//            int width = image.getWidth();
//            int height = image.getHeight();
//            int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
//            for (int pixel : pixels) {
//                md.update((byte) ((pixel >> 24) & 0xFF));
//                md.update((byte) ((pixel >> 16) & 0xFF));
//                md.update((byte) ((pixel >> 8) & 0xFF));
//                md.update((byte) (pixel & 0xFF));
//            }
//        }
//        return toHex(md.digest());
//    }
//
//    private String toHex(byte[] bytes) {
//        StringBuilder sb = new StringBuilder();
//        for (byte b : bytes) {
//            sb.append(String.format("%02x", b));
//        }
//        return sb.toString();
//    }

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
        String text = transcriptPage.getTranscript().replaceAll("[\\p{Cc}&&[^\\r\\n]]|[\\p{Cf}\\p{Co}\\p{Cn}]", "(!)");
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
