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
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
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
    private static final Color MAROON_DARK = new Color(75, 11, 47);
    private static final Color NAVY = new Color(23, 53, 79);
    private static final Color NAVY_DARK = new Color(16, 40, 61);
    private static final Color GOLD = new Color(212, 167, 44);
    private static final Color GOLD_SOFT = new Color(255, 247, 221);
    private static final Color CREAM = new Color(251, 248, 241);
    private static final Color BORDER = new Color(229, 222, 209);
    private static final Color TEXT = new Color(31, 41, 55);

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
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawPassBackground(graphics);
        drawPassHeader(graphics, details);

        byte[] qrBytes = createQrCode(details.appointment().getReferenceNo(), 420);
        try {
            BufferedImage qr = ImageIO.read(new ByteArrayInputStream(qrBytes));
            drawQrPanel(graphics, qr, details.appointment().getReferenceNo());
        } catch (IOException exception) {
            graphics.dispose();
            throw new IllegalArgumentException("The appointment pass could not be generated.");
        }

        drawAppointmentDetails(graphics, details);
        drawPassFooter(graphics);
        graphics.dispose();
        return writePng(pass);
    }

    private void drawPassBackground(Graphics2D graphics) {
        graphics.setColor(new Color(242, 236, 224));
        graphics.fillRect(0, 0, 900, 1200);
        graphics.setColor(new Color(23, 53, 79, 22));
        graphics.fillRoundRect(58, 54, 800, 1110, 38, 38);
        graphics.setColor(CREAM);
        graphics.fillRoundRect(50, 40, 800, 1120, 38, 38);
    }

    private void drawPassHeader(Graphics2D graphics, OfficerBiometricDetails details) {
        graphics.setColor(MAROON);
        graphics.fillRoundRect(50, 40, 800, 250, 38, 38);
        graphics.fillRect(50, 245, 800, 45);
        graphics.setColor(GOLD);
        graphics.fillRect(50, 284, 800, 6);

        drawOfficialBrand(graphics, 90, 67, Color.WHITE, new Color(243, 217, 135));
        graphics.setColor(new Color(243, 217, 135));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 14));
        graphics.drawString("OFFICIAL APPOINTMENT PASS", 90, 190);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 31));
        String service = safeText(details.application().getServiceType()).toUpperCase();
        drawFittedText(graphics, service + " BIOMETRIC COLLECTION", 90, 231, 570, 23);
        graphics.setColor(new Color(245, 233, 214));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 17));
        graphics.drawString("Present this pass at your biometric appointment.", 90, 260);

        graphics.setColor(GOLD);
        graphics.fillRoundRect(680, 192, 120, 38, 19, 19);
        graphics.setColor(MAROON_DARK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 13));
        drawCenteredInBox(graphics, statusLabel(details.appointment().getAppointmentStatus()), 680, 192, 120, 38);
    }

    private void drawQrPanel(Graphics2D graphics, BufferedImage qr, String reference) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(82, 310, 736, 510, 26, 26);
        graphics.setColor(BORDER);
        graphics.drawRoundRect(82, 310, 736, 510, 26, 26);

        graphics.setColor(GOLD_SOFT);
        graphics.fillRoundRect(218, 325, 464, 454, 22, 22);
        graphics.setColor(new Color(236, 213, 138));
        graphics.drawRoundRect(218, 325, 464, 454, 22, 22);
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(230, 337, 440, 440, 14, 14);
        graphics.drawImage(qr, 240, 347, null);

        graphics.setColor(MAROON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 11));
        drawCenteredInBox(graphics, "APPOINTMENT REFERENCE", 82, 779, 736, 15);
        graphics.setColor(NAVY_DARK);
        graphics.setFont(new Font("Monospaced", Font.BOLD, 20));
        drawCenteredInBox(graphics, safeText(reference), 82, 794, 736, 24);
    }

    private void drawAppointmentDetails(Graphics2D graphics, OfficerBiometricDetails details) {
        graphics.setColor(MAROON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 14));
        graphics.drawString("APPOINTMENT DETAILS", 82, 846);
        graphics.setColor(GOLD);
        graphics.fillRoundRect(82, 856, 86, 4, 2, 2);

        graphics.setColor(NAVY);
        graphics.fillRoundRect(82, 870, 736, 62, 16, 16);
        graphics.setColor(new Color(243, 217, 135));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 11));
        graphics.drawString("DATE AND TIME", 105, 894);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 21));
        drawFittedText(graphics, details.appointment().getScheduledAt().format(APPOINTMENT_FORMAT), 105, 920, 680, 16);

        drawDetailCard(graphics, 82, 944, 350, 68, "CITIZEN", details.applicantName());
        drawDetailCard(graphics, 450, 944, 368, 68, "APPLICATION", details.application().getApplicationNo());
        drawDetailCard(graphics, 82, 1024, 350, 68, "DOCUMENT SERVICE", details.application().getServiceType());
        drawDetailCard(graphics, 450, 1024, 368, 68, "SERVICE CENTRE", details.centre().getCentreName());
    }

    private void drawDetailCard(Graphics2D graphics, int x, int y, int width, int height, String label, String value) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(x, y, width, height, 14, 14);
        graphics.setColor(BORDER);
        graphics.drawRoundRect(x, y, width, height, 14, 14);
        graphics.setColor(GOLD);
        graphics.fillRoundRect(x, y, 6, height, 6, 6);
        graphics.setColor(MAROON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 10));
        graphics.drawString(label, x + 20, y + 22);
        graphics.setColor(TEXT);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 17));
        drawFittedText(graphics, safeText(value), x + 20, y + 50, width - 38, 13);
    }

    private void drawPassFooter(Graphics2D graphics) {
        graphics.setColor(NAVY_DARK);
        graphics.fillRoundRect(50, 1106, 800, 54, 38, 38);
        graphics.fillRect(50, 1106, 800, 20);
        graphics.setColor(GOLD);
        graphics.fillRect(50, 1106, 800, 5);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 13));
        drawCenteredInBox(graphics, "QR contains the appointment reference only · Issued by LankaID", 50, 1114, 800, 37);
    }

    private void drawFittedText(Graphics2D graphics, String text, int x, int y, int maxWidth, int minimumSize) {
        Font font = graphics.getFont();
        FontMetrics metrics = graphics.getFontMetrics(font);
        while (metrics.stringWidth(text) > maxWidth && font.getSize() > minimumSize) {
            font = font.deriveFont((float) font.getSize() - 1);
            graphics.setFont(font);
            metrics = graphics.getFontMetrics(font);
        }
        String fitted = text;
        while (metrics.stringWidth(fitted) > maxWidth && fitted.length() > 3) {
            fitted = fitted.substring(0, fitted.length() - 2).stripTrailing() + "…";
        }
        graphics.drawString(fitted, x, y);
    }

    private void drawCenteredInBox(Graphics2D graphics, String text, int x, int y, int width, int height) {
        FontMetrics metrics = graphics.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(text)) / 2;
        int textY = y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
        graphics.drawString(text, textX, textY);
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "Not provided" : value.strip();
    }

    private String statusLabel(String status) {
        return safeText(status).replace('_', ' ').toUpperCase();
    }

    /** Draws the same emblem and three-language LankaID mark used by the web pages. */
    public void drawOfficialBrand(Graphics2D graphics, int x, int y, Color textColor) {
        drawOfficialBrand(graphics, x, y, textColor, textColor);
    }

    /** Draws the official brand with an optional accent colour for the ID mark. */
    public void drawOfficialBrand(Graphics2D graphics, int x, int y, Color textColor, Color accentColor) {
        try (InputStream stream = getClass().getResourceAsStream("/static/images/sri-lanka-government-emblem.png")) {
            if (stream == null) throw new IOException("Government emblem was not found.");
            BufferedImage emblem = ImageIO.read(stream);
            graphics.drawImage(emblem, x, y, 68, 78, null);
        } catch (IOException exception) {
            throw new IllegalArgumentException("The government emblem could not be loaded.");
        }
        graphics.setColor(textColor);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 36));
        graphics.drawString("Lanka", x + 84, y + 35);
        int idX = x + 84 + graphics.getFontMetrics().stringWidth("Lanka");
        graphics.setColor(accentColor);
        graphics.drawString("ID", idX, y + 35);

        Font subtitleFont = new Font("SansSerif", Font.PLAIN, 13);
        graphics.setFont(subtitleFont);
        graphics.setColor(textColor);
        String multilingual = "ශ්‍රී ලංකා රජය · இலங்கை அரசு · Government of Sri Lanka";
        String subtitle = subtitleFont.canDisplayUpTo(multilingual) < 0
                ? multilingual
                : "Government of Sri Lanka · Official Digital Service";
        graphics.drawString(subtitle, x + 84, y + 57);
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
