package fr.monkeynotes.mn.service.impl;

import fr.monkeynotes.mn.service.ImageService;
import fr.monkeynotes.mn.service.UtilsService;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ImageServiceImpl implements ImageService {
    public static final int IMAGE_READ_BUFFER = 8192;
    private Logger LOG = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private UtilsService utilsService;


    @Override
    public String imageAsBase64(URL imageURL) {
        String strUrl = imageURL.getPath();

        Pattern p = Pattern.compile("image\\/(.*)\\/(.*)\\/(\\d+)");
        Matcher m = p.matcher(strUrl);

        if (m.find()) {

            String username = m.group(1);
            String fileId = m.group(2);
            int nb = Integer.valueOf(m.group(3));

            return imageAsBase64(username, fileId, nb);
        }
        //TODO err
        return null;

    }

    @Override
    public String imageAsBase64(String username, String fileId, int imageNum) {


        byte[] bytes = null;
        try {
            bytes = Files.readAllBytes(utilsService.imagePath(username, fileId, imageNum));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return Base64.getEncoder().encodeToString(bytes);

    }

    @Override
    public void efficientStreamImage(String username, String fileId, int imageNum, OutputStream outputStream) throws IOException {
        try (InputStream inputStream = new FileInputStream(utilsService.imagePath(username, fileId, imageNum).toFile())) {
            byte[] buffer = new byte[IMAGE_READ_BUFFER];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    @Override
    public void efficientStreamImage(String username, String fileId, int imageNum, OutputStream outputStream, boolean temp) throws IOException {
        Path p = temp ? utilsService.tempImagePath(username, fileId, imageNum) : utilsService.imagePath(username, fileId, imageNum);

        try (InputStream inputStream = new FileInputStream(p.toFile())) {
            byte[] buffer = new byte[IMAGE_READ_BUFFER];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    @Override
    public double compareImages(BufferedImage image1, BufferedImage image2) {
        nu.pattern.OpenCV.loadLocally();

        Mat matA = convert(image1);
        Mat matB = convert(image2);

        Mat grayA = new Mat();
        Mat grayB = new Mat();

        Imgproc.cvtColor(matA, grayA, Imgproc.COLOR_BGR2GRAY);
        Imgproc.cvtColor(matB, grayB, Imgproc.COLOR_BGR2GRAY);

        Mat diff = new Mat();
        Core.absdiff(grayA, grayB, diff);
        diff.convertTo(diff, CvType.CV_32F);
        diff = diff.mul(diff); // square the difference

        Scalar sum = Core.sumElems(diff);
        double sse = sum.val[0];
        double mse = sse / (double)(grayA.total());

        if (mse == 0) return Double.POSITIVE_INFINITY; // images are identical

        return 10.0 * Math.log10((255 * 255) / mse); // PSNR
    }

    public static Mat convert(BufferedImage image) {
        int type = image.getType();
        if (type != BufferedImage.TYPE_3BYTE_BGR) {
            // Convert image type if not 3BYTE_BGR
            BufferedImage convertedImg = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
            convertedImg.getGraphics().drawImage(image, 0, 0, null);
            image = convertedImg;
        }

        byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC3);
        mat.put(0, 0, pixels);
        return mat;
    }

}
