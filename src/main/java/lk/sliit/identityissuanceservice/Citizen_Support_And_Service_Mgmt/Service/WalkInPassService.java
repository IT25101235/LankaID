package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentPassService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInAppointmentView;
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
import java.time.format.DateTimeFormatter;

/** Produces the QR image and downloadable pass without exposing appointment details in the QR value. */
@Service
public class WalkInPassService {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    private static final Color MAROON = new Color(104, 16, 66);
    private static final Color MAROON_DARK = new Color(75, 11, 47);
    private static final Color MAROON_SOFT = new Color(248, 237, 243);
    private static final Color NAVY = new Color(23, 53, 79);
    private static final Color NAVY_DARK = new Color(16, 40, 61);
    private static final Color GOLD = new Color(212, 167, 44);
    private static final Color GOLD_SOFT = new Color(255, 247, 221);
    private static final Color CREAM = new Color(251, 248, 241);
    private static final Color BORDER = new Color(229, 222, 209);
    private static final Color TEXT = new Color(31, 41, 55);
    private static final Color MUTED = new Color(101, 113, 126);

    private final AppointmentPassService qrService;
    private final WalkInAppointmentService walkInService;

    public WalkInPassService(
            AppointmentPassService qrService,
            WalkInAppointmentService walkInService
    ) {
        this.qrService = qrService;
        this.walkInService = walkInService;
    }

    public byte[] createQrCode(String reference, int size) {
        return qrService.createQrCode(reference, size);
    }

    public byte[] createPass(WalkInAppointmentView details) {
        // Build the pass in memory so no temporary citizen document is left on the server.
        BufferedImage pass = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = pass.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(graphics);
        drawHeader(graphics);

        try {
            // Only the reference is encoded; the protected endpoint resolves the actual booking.
            BufferedImage qr = ImageIO.read(new ByteArrayInputStream(
                    createQrCode(details.appointment().getReferenceNo(), 300)
            ));
            drawQrPanel(graphics, qr, details.appointment().getReferenceNo());
        } catch (IOException exception) {
            graphics.dispose();
            throw new IllegalArgumentException("The walk-in pass could not be generated.");
        }

        drawAppointmentDetails(graphics, details);
        drawFooter(graphics);
        graphics.dispose();
        return writePng(pass);
    }

    private void drawBackground(Graphics2D graphics) {
        graphics.setColor(new Color(242, 236, 224));
        graphics.fillRect(0, 0, 900, 1200);
        graphics.setColor(new Color(23, 53, 79, 22));
        graphics.fillRoundRect(58, 54, 800, 1110, 38, 38);
        graphics.setColor(CREAM);
        graphics.fillRoundRect(50, 40, 800, 1120, 38, 38);
    }

    private void drawHeader(Graphics2D graphics) {
        graphics.setColor(MAROON);
        graphics.fillRoundRect(50, 40, 800, 250, 38, 38);
        graphics.fillRect(50, 245, 800, 45);
        graphics.setColor(GOLD);
        graphics.fillRect(50, 284, 800, 6);

        qrService.drawOfficialBrand(graphics, 90, 67, Color.WHITE, new Color(243, 217, 135));
        graphics.setColor(new Color(243, 217, 135));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 14));
        graphics.drawString("OFFICIAL APPOINTMENT PASS", 90, 190);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 31));
        graphics.drawString("Walk-in service", 90, 231);
        graphics.setColor(new Color(245, 233, 214));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 17));
        graphics.drawString("Present this pass when you arrive at the service centre.", 90, 260);

        graphics.setColor(GOLD);
        graphics.fillRoundRect(688, 192, 112, 38, 19, 19);
        graphics.setColor(MAROON_DARK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 14));
        drawCenteredInBox(graphics, "CONFIRMED", 688, 192, 112, 38);
    }

    private void drawQrPanel(Graphics2D graphics, BufferedImage qr, String reference) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(82, 310, 736, 370, 26, 26);
        graphics.setColor(BORDER);
        graphics.drawRoundRect(82, 310, 736, 370, 26, 26);

        graphics.setColor(GOLD_SOFT);
        graphics.fillRoundRect(94, 327, 340, 336, 20, 20);
        graphics.setColor(new Color(236, 213, 138));
        graphics.drawRoundRect(94, 327, 340, 336, 20, 20);
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(106, 331, 316, 316, 12, 12);
        graphics.drawImage(qr, 114, 339, null);

        graphics.setColor(MAROON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 13));
        drawCenteredInBox(graphics, "SCAN AT THE SERVICE CENTRE", 94, 639, 340, 24);

        graphics.setColor(MAROON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 14));
        graphics.drawString("APPOINTMENT REFERENCE", 448, 371);
        graphics.setColor(NAVY_DARK);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 28));
        drawFittedText(graphics, safeText(reference), 448, 420, 322, 18);

        graphics.setColor(BORDER);
        graphics.drawLine(448, 443, 777, 443);
        graphics.setColor(TEXT);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 22));
        graphics.drawString("Ready for check-in", 448, 487);
        graphics.setColor(MUTED);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 17));
        graphics.drawString("Keep this pass available on your phone", 448, 520);
        graphics.drawString("or bring a printed copy.", 448, 547);

        graphics.setColor(MAROON_SOFT);
        graphics.fillRoundRect(448, 574, 329, 55, 14, 14);
        graphics.setColor(MAROON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 15));
        graphics.drawString("SECURE QR", 466, 598);
        graphics.setColor(MUTED);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 13));
        graphics.drawString("Reference only — no personal details", 466, 618);
    }

    private void drawAppointmentDetails(Graphics2D graphics, WalkInAppointmentView details) {
        graphics.setColor(MAROON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 15));
        graphics.drawString("APPOINTMENT DETAILS", 82, 709);
        graphics.setColor(GOLD);
        graphics.fillRoundRect(82, 721, 86, 4, 2, 2);

        drawDateCard(
                graphics,
                details.appointment().getScheduledAt().format(DATE_TIME_FORMAT)
        );
        drawDetailCard(graphics, 82, 822, 350, 88, "VISITOR", details.appointment().getVisitorName());
        drawDetailCard(graphics, 450, 822, 368, 88, "PHONE", details.appointment().getVisitorPhone());
        drawDetailCard(
                graphics,
                82,
                928,
                350,
                88,
                "SERVICE",
                walkInService.serviceLabel(details.appointment().getVisitorService())
        );
        drawDetailCard(graphics, 450, 928, 368, 88, "SERVICE CENTRE", details.centreName());
    }

    private void drawDateCard(Graphics2D graphics, String dateAndTime) {
        graphics.setColor(NAVY);
        graphics.fillRoundRect(82, 738, 736, 74, 18, 18);
        graphics.setColor(new Color(243, 217, 135));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 13));
        graphics.drawString("DATE AND TIME", 105, 767);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 25));
        drawFittedText(graphics, safeText(dateAndTime), 105, 798, 680, 18);
    }

    private void drawDetailCard(
            Graphics2D graphics,
            int x,
            int y,
            int width,
            int height,
            String label,
            String value
    ) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(x, y, width, height, 16, 16);
        graphics.setColor(BORDER);
        graphics.drawRoundRect(x, y, width, height, 16, 16);
        graphics.setColor(GOLD);
        graphics.fillRoundRect(x, y, 6, height, 6, 6);
        graphics.setColor(MAROON);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 12));
        graphics.drawString(label, x + 22, y + 29);
        graphics.setColor(TEXT);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 19));
        drawFittedText(graphics, safeText(value), x + 22, y + 62, width - 42, 14);
    }

    private void drawFooter(Graphics2D graphics) {
        graphics.setColor(NAVY_DARK);
        graphics.fillRoundRect(50, 1042, 800, 118, 38, 38);
        graphics.fillRect(50, 1042, 800, 38);
        graphics.setColor(GOLD);
        graphics.fillRect(50, 1042, 800, 5);
        graphics.setColor(new Color(243, 217, 135));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 14));
        graphics.drawString("BEFORE YOUR VISIT", 82, 1081);
        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 16));
        graphics.drawString("Bring this pass and a valid identity document.", 82, 1109);
        graphics.setColor(new Color(198, 212, 223));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 13));
        graphics.drawString("Issued by LankaID · Government of Sri Lanka", 82, 1137);
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

    private byte[] writePng(BufferedImage image) {
        // Returning bytes lets the controller choose inline display or download headers.
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("The walk-in pass could not be generated.");
        }
    }
}
