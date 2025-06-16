import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.GrayU8;
import org.junit.jupiter.api.Test;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;


public class TestCompareImage {

    @Test
    void test() {

        nu.pattern.OpenCV.loadLocally();

//        String normalizedPath1 = Paths.get("/home/marc/Desktop/B14 - NotepadTranscript/comp/1beisolEJ28DEK4VEaYkfFvYhk4onCaFk_1.jpg").normalize().toAbsolutePath().toString();
//        String normalizedPath2 = Paths.get("/home/marc/Desktop/B14 - NotepadTranscript/comp/1beisolEJ28DEK4VEaYkfFvYhk4onCaFk_2.jpg").normalize().toAbsolutePath().toString();


        try {
            BufferedImage b1 = ImageIO.read(Paths.get("/home/marc/Desktop/B14 - NotepadTranscript/comp/1beisolEJ28DEK4VEaYkfFvYhk4onCaFk_1.jpg").toFile());
            BufferedImage b2 = ImageIO.read(Paths.get("/home/marc/Desktop/B14 - NotepadTranscript/comp/1beisolEJ28DEK4VEaYkfFvYhk4onCaFk_2.jpg").toFile());
            double c = compareImages(b1, b2);
            System.out.println(c);
        } catch (Exception e) {
            e.printStackTrace();
        }



    }


//    public static double computeMSE(BufferedImage imgA, BufferedImage imgB) {
//        // Convert BufferedImage to BoofCV grayscale images
//        GrayU8 grayA = ConvertBufferedImage.convertFrom(imgA, (GrayU8) null);
//        GrayU8 grayB = ConvertBufferedImage.convertFrom(imgB, (GrayU8) null);
//
//        if (grayA.width != grayB.width || grayA.height != grayB.height) {
//            throw new IllegalArgumentException("Image dimensions must match.");
//        }
//
//        double errorSum = 0;
//        int width = grayA.width;
//        int height = grayA.height;
//
//        for (int y = 0; y < height; y++) {
//            for (int x = 0; x < width; x++) {
//                int diff = grayA.get(x, y) - grayB.get(x, y);
//                errorSum += diff * diff;
//            }
//        }
//
//        return errorSum / (width * height); // Mean Squared Error
//    }

    public double compareImages(BufferedImage imgA, BufferedImage imgB) {
        Mat matA = convert(imgA);
        Mat matB = convert(imgB);

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

