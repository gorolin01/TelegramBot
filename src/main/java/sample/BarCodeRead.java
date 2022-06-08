package sample;

import com.google.zxing.*;
import com.google.zxing.Reader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

public class BarCodeRead {
    public String getBarCode(BufferedImage barCodeBufferedImage){

        try{

            //InputStream barCodeInputStream = new FileInputStream("C:\\Users\\Admin\\Desktop\\barcode_PNG60.png");
            //BufferedImage barCodeBufferedImage = ImageIO.read(barCodeInputStream);

            LuminanceSource source = new BufferedImageLuminanceSource(barCodeBufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Reader reader = new MultiFormatReader();
            Result result = reader.decode(bitmap);

            System.out.println("Barcode text is " + result.getText());
            return result.getText();

        } catch (ReaderException e) {
            e.printStackTrace();
            return e.getMessage();
        }

    }
}
