package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.ImageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageServiceImpl implements ImageService {
    public static final int PDF2IMAGE_DPI = 150;
    public static final ImageType PDF2IMAGE_IMAGE_TYPE = ImageType.GRAY;

    private Logger LOG = LoggerFactory.getLogger(ImageService.class);

    @Override
    public List<Path> pdf2Images(String fileId, java.io.File sourceFile, Path targetDir) {
        LOG.info("Converting {} to images", sourceFile);
        List<Path> listImages = null;
        try {
            //java.io.File sourceFile = new java.io.File(sourcePath);
            listImages = new ArrayList<>();

            if (sourceFile.exists()) {
                PDDocument document = Loader.loadPDF(sourceFile);
                PDFRenderer pdfRenderer = new PDFRenderer(document);

                int pageCount = document.getNumberOfPages();
                //System.out.println("Total pages to be converted -> " + pageCount);

                //String fileName = fileId;//sourceFile.getName().replace(".pdf", "");
                for (int pageNumber = 0; pageNumber < pageCount; pageNumber++) {
                    String filename = fileId + "_" + (pageNumber + 1) + ".png";
                    BufferedImage image = pdfRenderer.renderImageWithDPI(pageNumber, PDF2IMAGE_DPI, PDF2IMAGE_IMAGE_TYPE);
                    Path pathPage = Paths.get(targetDir.toString(), filename);
                    java.io.File outputFile = pathPage.toFile();

                    //System.out.println("Image Created -> " + outputFile.getName());
                    ImageIO.write(image, "png", outputFile);
                    listImages.add(pathPage);
                    LOG.info("Page {} converted as {}", pageNumber, filename);
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
    public byte[] getImage(String fileId, String imagename) {
        // open image
        LOG.info("Qwen request fileId {} image {}", fileId, imagename);

        try {

            return Files.readAllBytes(Paths.get("/tmp/", fileId, imagename));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        return null;
    }

}
