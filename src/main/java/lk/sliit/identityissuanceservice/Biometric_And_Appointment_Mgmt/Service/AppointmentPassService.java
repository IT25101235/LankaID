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
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

/** Builds the QR code and the downloadable PNG pass (ZXing library and Java 2D). */
@Service
public class AppointmentPassService {

    private static final DateTimeFormatter APPOINTMENT_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // Same maroon and gold as the portal theme.
    private static final Color MAROON = new Color(104, 16, 66);
    private static final Color GOLD = new Color(212, 167, 44);

    private static final String THREE_LANGUAGE_LINE = "ශ්‍රී ලංකා රජය · இலங்கை அரசு · Government of Sri Lanka";
    private static final String ENGLISH_LINE = "Government of Sri Lanka";

    /** Widest a value may be, so it stays inside the card (the card ends at x = 840). */
    private static final int VALUE_X = 355;
    private static final int VALUE_MAX_WIDTH = 450;

    /** The QR holds only the appointment reference number, no personal data. */
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

    /** Draws the full pass image: logo, QR code, citizen, centre, date and status. */
    public byte[] createDownloadablePass(OfficerBiometricDetails details) {
        BufferedImage pass = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = pass.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(241, 245, 249));
        graphics.fillRect(0, 0, pass.getWidth(), pass.getHeight());

        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(60, 50, 780, 1090, 35, 35);
        graphics.setColor(MAROON);
        graphics.fillRoundRect(60, 50, 780, 170, 35, 35);
        graphics.fillRect(60, 170, 780, 50);
        graphics.setColor(GOLD);
        graphics.fillRect(60, 214, 780, 6);

        drawOfficialBrand(graphics, 105, 70, Color.WHITE);
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
        drawValue(graphics, details.applicantName(), 790);
        drawValue(graphics, details.application().getApplicationNo(), 850);
        drawValue(graphics, details.centre().getCentreName(), 910);
        drawValue(graphics, details.appointment().getScheduledAt().format(APPOINTMENT_FORMAT), 970);
        drawValue(graphics, details.appointment().getAppointmentStatus(), 1030);

        graphics.setColor(new Color(71, 85, 105));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCentered(graphics, "The QR code contains only the appointment reference shown above.", 1090, pass.getWidth());
        graphics.dispose();
        return writePng(pass);
    }

    /** Draws the same emblem and three-language LankaID mark used by the web pages. */
    public void drawOfficialBrand(Graphics2D graphics, int x, int y, Color textColor) {
        try (InputStream stream = getClass().getResourceAsStream("/static/images/sri-lanka-government-emblem.png")) {
            if (stream == null) throw new IOException("Government emblem was not found.");
            BufferedImage emblem = ImageIO.read(stream);
            graphics.drawImage(emblem, x, y, 68, 78, null);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The government emblem could not be loaded.");
        }
        graphics.setColor(textColor);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 36));
        graphics.drawString("LankaID", x + 84, y + 35);
        Font subtitleFont = new Font("SansSerif", Font.PLAIN, 13);
        graphics.setFont(subtitleFont);
        // A server without Sinhala and Tamil fonts would print boxes, so fall back to English only.
        boolean canShowAllLanguages = subtitleFont.canDisplayUpTo(THREE_LANGUAGE_LINE) == -1;
        graphics.drawString(canShowAllLanguages ? THREE_LANGUAGE_LINE : ENGLISH_LINE, x + 84, y + 57);
    }

    /** Draws a value in the right-hand column, shortened with an ellipsis if it would leave the card. */
    private void drawValue(Graphics2D graphics, String text, int y) {
        graphics.drawString(fit(graphics, text == null ? "" : text, VALUE_MAX_WIDTH), VALUE_X, y);
    }

    private String fit(Graphics2D graphics, String text, int maxWidth) {
        java.awt.FontMetrics metrics = graphics.getFontMetrics();
        if (metrics.stringWidth(text) <= maxWidth) return text;
        String shortened = text;
        while (!shortened.isEmpty() && metrics.stringWidth(shortened + "…") > maxWidth) {
            shortened = shortened.substring(0, shortened.length() - 1);
        }
        return shortened.stripTrailing() + "…";
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
