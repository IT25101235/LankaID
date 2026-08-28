package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.OfficerBiometricDetails;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class AppointmentPassService {

    private static final DateTimeFormatter APPOINTMENT_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    public byte[] createQrCode(String reference, int size) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(reference, BarcodeFormat.QR_CODE, size, size);
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    image.setRGB(x, y, matrix.get(x, y) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
                }
            }
            return writePng(image);
        } catch (WriterException exception) {
            throw new IllegalArgumentException("The QR code could not be generated.");
        }
    }

    public byte[] createDownloadablePass(OfficerBiometricDetails details) {
        BufferedImage pass = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = pass.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(241, 245, 249));
        graphics.fillRect(0, 0, pass.getWidth(), pass.getHeight());

        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(60, 50, 780, 1090, 35, 35);
        graphics.setColor(new Color(2, 132, 199));
        graphics.fillRoundRect(60, 50, 780, 170, 35, 35);
        graphics.fillRect(60, 170, 780, 50);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 40));
        graphics.drawString("LankaID", 105, 120);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 25));
        graphics.drawString("Biometric Collection Appointment Pass", 105, 170);

        byte[] qrBytes = createQrCode(details.appointment().getReferenceNo(), 390);
        try {
            BufferedImage qr = ImageIO.read(new java.io.ByteArrayInputStream(qrBytes));
            graphics.drawImage(qr, 255, 255, null);
        } catch (IOException exception) {
            graphics.dispose();
            throw new IllegalArgumentException("The appointment pass could not be generated.");
        }

        graphics.setColor(new Color(15, 23, 42));
        graphics.setFont(new Font("Monospaced", Font.BOLD, 28));
        drawCentered(graphics, details.appointment().getReferenceNo(), 710, pass.getWidth());

        graphics.setFont(new Font("SansSerif", Font.BOLD, 25));
        graphics.drawString("Citizen", 115, 790);
        graphics.drawString("Application", 115, 850);
        graphics.drawString("Service centre", 115, 910);
        graphics.drawString("Date and time", 115, 970);
        graphics.drawString("Status", 115, 1030);

        graphics.setFont(new Font("SansSerif", Font.PLAIN, 24));
        graphics.drawString(details.applicantName(), 355, 790);
        graphics.drawString(details.application().getApplicationNo(), 355, 850);
        graphics.drawString(details.centre().getCentreName(), 355, 910);
        graphics.drawString(details.appointment().getScheduledAt().format(APPOINTMENT_FORMAT), 355, 970);
        graphics.drawString(details.appointment().getAppointmentStatus(), 355, 1030);

        graphics.setColor(new Color(71, 85, 105));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCentered(graphics, "The QR code contains only the appointment reference shown above.", 1090, pass.getWidth());
        graphics.dispose();
        return writePng(pass);
    }

    private void drawCentered(Graphics2D graphics, String text, int y, int width) {
        int x = (width - graphics.getFontMetrics().stringWidth(text)) / 2;
        graphics.drawString(text, Math.max(x, 20), y);
    }

    private byte[] writePng(BufferedImage image) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("The image could not be generated.");
        }
    }
}
