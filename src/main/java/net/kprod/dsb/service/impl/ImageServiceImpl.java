package net.kprod.dsb.service.impl;

import net.kprod.dsb.service.ImageService;
import net.kprod.dsb.service.UtilsService;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class ImageServiceImpl implements ImageService {
    public static final int IMAGE_READ_BUFFER = 8192;
    private Logger LOG = LoggerFactory.getLogger(ImageService.class);

    @Autowired
    private UtilsService utilsService;

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
