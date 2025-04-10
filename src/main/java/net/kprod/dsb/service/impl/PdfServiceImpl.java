package net.kprod.dsb.service.impl;

import net.kprod.dsb.data.dto.DtoTranscript;
import net.kprod.dsb.data.dto.DtoTranscriptPage;
import net.kprod.dsb.service.ImageService;
import net.kprod.dsb.service.PdfService;
import net.kprod.dsb.service.UtilsService;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PdfServiceImpl implements PdfService {
    private Logger LOG = LoggerFactory.getLogger(PdfService.class);

    public static final int PDF2IMAGE_DPI = 150;
    public static final ImageType PDF2IMAGE_IMAGE_TYPE = ImageType.GRAY;

    @Autowired
    private UtilsService utilsService;

    @Override
    public List<URL> pdf2Images(String fileId, java.io.File sourceFile, Path targetDir) {
        LOG.info("Converting {} to images", sourceFile);
        List<URL> listImages = null;
        try {
            //java.io.File sourceFile = new java.io.File(sourcePath);
            listImages = new ArrayList<>();

            if (sourceFile.exists()) {
                PDDocument document = Loader.loadPDF(sourceFile);
                PDFRenderer pdfRenderer = new PDFRenderer(document);

                int pageCount = document.getNumberOfPages();
                //System.out.println("Total pages to be converted -> " + pageCount);

                for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, PDF2IMAGE_DPI, PDF2IMAGE_IMAGE_TYPE);

                    Path pathPage = utilsService.imagePath(fileId, (pageNumber + 1));
                    java.io.File outputFile = pathPage.toFile();

                    ImageIO.write(image, "png", outputFile);
                    listImages.add(utilsService.imageURL(fileId, (pageNumber + 1)));
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
    public java.io.File createTranscriptPdf(String fileId, DtoTranscript dtoTranscript) throws IOException {




        //textContent = textContent.replaceAll("[\\p{Cc}&&[^\\r\\n]]|[\\p{Cf}\\p{Co}\\p{Cn}]", "(!)");
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

            for(DtoTranscriptPage transcriptPage : dtoTranscript.getPages()) {


                String text = transcriptPage.getTranscript().replaceAll("[\\p{Cc}&&[^\\r\\n]]|[\\p{Cf}\\p{Co}\\p{Cn}]", "(!)");


                while (text.length() > 0) {
                    int nlIndex = text.indexOf('\n');
                    int spaceIndex = text.indexOf(' ', lastSpace + 1);

                    if (spaceIndex < 0) {
                        spaceIndex = text.length();
                    }

                    subString = text.substring(0, spaceIndex > nlIndex ? spaceIndex : nlIndex);
                    lineWidth = fontSize * pdfFont.getStringWidth(subString.replaceAll("\\p{C}", "")) / 1000;

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
                    }
                    else if (spaceIndex == text.length()) {
                        lines.add(text.replaceAll("\\p{C}", ""));
                        //System.out.printf(">%s\n", text);
                        text = "";
                    }
                    else {
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


            pages.put(pageNumber, new ArrayList<>(lines));

            contentStream.beginText();
            contentStream.setFont(pdfFont, fontSize);
            contentStream.newLineAtOffset(startX, startY);

            for(Map.Entry<Integer, List<String>> entry : pages.entrySet()) {

                for (String line : entry.getValue()) {
                    contentStream.showText(line);
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
}
