package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentPassService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInAppointmentView;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

@Service
public class WalkInPassService {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

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
        BufferedImage pass = new BufferedImage(900, 1200, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = pass.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(241, 245, 249));
        graphics.fillRect(0, 0, 900, 1200);
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(60, 50, 780, 1090, 35, 35);
        graphics.setColor(new Color(8, 145, 178));
        graphics.fillRoundRect(60, 50, 780, 170, 35, 35);
        graphics.fillRect(60, 170, 780, 50);

        graphics.setColor(Color.WHITE);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 40));
        graphics.drawString("LankaID", 105, 120);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 25));
        graphics.drawString("Walk-in Service Appointment Pass", 105, 170);

        try {
            BufferedImage qr = ImageIO.read(new ByteArrayInputStream(
                    createQrCode(details.appointment().getReferenceNo(), 390)
            ));
            graphics.drawImage(qr, 255, 250, null);
        } catch (IOException exception) {
            graphics.dispose();
            throw new IllegalArgumentException("The walk-in pass could not be generated.");
        }

        graphics.setColor(new Color(15, 23, 42));
        graphics.setFont(new Font("Monospaced", Font.BOLD, 28));
        drawCentered(graphics, details.appointment().getReferenceNo(), 705, 900);

        graphics.setFont(new Font("SansSerif", Font.BOLD, 24));
        graphics.drawString("Visitor", 115, 785);
        graphics.drawString("Phone", 115, 845);
        graphics.drawString("Service", 115, 905);
        graphics.drawString("Centre", 115, 965);
        graphics.drawString("Date and time", 115, 1025);

        graphics.setFont(new Font("SansSerif", Font.PLAIN, 23));
        graphics.drawString(details.appointment().getVisitorName(), 345, 785);
        graphics.drawString(details.appointment().getVisitorPhone(), 345, 845);
        graphics.drawString(walkInService.serviceLabel(details.appointment().getVisitorService()), 345, 905);
        graphics.drawString(details.centreName(), 345, 965);
        graphics.drawString(details.appointment().getScheduledAt().format(DATE_TIME_FORMAT), 345, 1025);

        graphics.setColor(new Color(71, 85, 105));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 18));
        drawCentered(graphics, "The QR code contains only the reference printed above.", 1090, 900);
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
            throw new IllegalArgumentException("The walk-in pass could not be generated.");
        }
    }
}
