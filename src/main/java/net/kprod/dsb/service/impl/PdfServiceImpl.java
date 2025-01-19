package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.PdfService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfServiceImpl implements PdfService {

    @Override
    public java.io.File createTranscriptPdf(String fileId, String textContent) throws IOException {
        textContent = textContent.replaceAll("[\\p{Cc}&&[^\\r\\n]]|[\\p{Cf}\\p{Co}\\p{Cn}]", "(!)");
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
            float width = mediabox.getWidth() - 2*margin;
            float startX = mediabox.getLowerLeftX() + margin;
            float startY = mediabox.getUpperRightY() - margin;

            String text = textContent;
            List<String> lines = new ArrayList<>();
            int lastSpace = -1;
            float size = 0;
            String subString;
            while (text.length() > 0)
            {
                int nlIndex = text.indexOf('\n');
                int spaceIndex = text.indexOf(' ', lastSpace + 1);

                if (spaceIndex < 0) {
                    spaceIndex = text.length();
                }

                subString = text.substring(0, spaceIndex > nlIndex ? spaceIndex : nlIndex);
                size = fontSize * pdfFont.getStringWidth(subString.replaceAll("\\p{C}", "")) / 1000;

                if (nlIndex < spaceIndex && nlIndex != -1) {
                    subString = text.substring(0, nlIndex);

                    lines.add(subString.replaceAll("\\p{C}", ""));
                    text = text.substring(nlIndex).trim();
                    //System.out.printf("> %s\n", subString);
                } else if (size > width) {
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
            }

            contentStream.beginText();
            contentStream.setFont(pdfFont, fontSize);
            contentStream.newLineAtOffset(startX, startY);
            for (String line: lines)
            {
                contentStream.showText(line);
                contentStream.newLineAtOffset(0, -leading);
            }
            contentStream.endText();
            contentStream.close();

            file = new java.io.File("/tmp/",  fileId + ".pdf");
            doc.save(file);
        }
        finally
        {
            if (doc != null)
            {
                doc.close();
            }
        }

        return file;
    }
}
