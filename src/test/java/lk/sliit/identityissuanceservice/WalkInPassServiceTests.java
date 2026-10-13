package lk.sliit.identityissuanceservice;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentPassService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInAppointmentView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInAppointmentService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInPassService;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WalkInPassServiceTests {

    @Test
    void downloadablePassUsesGovernmentThemeAndKeepsQrReferenceOnly() throws Exception {
        Appointment appointment = new Appointment();
        appointment.setReferenceNo("WALK-2026-0FB2DEBA");
        appointment.setVisitorName("Aakil Ahamed");
        appointment.setVisitorPhone("+94741574433");
        appointment.setVisitorService("NIC_APPLICATION_HELP");
        appointment.setScheduledAt(LocalDateTime.of(2026, 10, 30, 10, 30));

        WalkInAppointmentService appointments = mock(WalkInAppointmentService.class);
        when(appointments.serviceLabel("NIC_APPLICATION_HELP")).thenReturn("NIC application assistance");
        WalkInPassService passes = new WalkInPassService(new AppointmentPassService(), appointments);

        byte[] png = passes.createPass(new WalkInAppointmentView(
                appointment,
                "Colombo Main Centre",
                "Colombo 01",
                false
        ));
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));

        assertNotNull(image);
        assertEquals(900, image.getWidth());
        assertEquals(1200, image.getHeight());
        assertEquals(new Color(104, 16, 66).getRGB(), image.getRGB(70, 100));
        assertEquals(new Color(212, 167, 44).getRGB(), image.getRGB(70, 286));
        assertTrue(png.length > 40_000);

        BufferedImage qr = image.getSubimage(114, 339, 300, 300);
        int[] pixels = qr.getRGB(0, 0, qr.getWidth(), qr.getHeight(), null, 0, qr.getWidth());
        Result decoded = new QRCodeReader().decode(new BinaryBitmap(
                new HybridBinarizer(new RGBLuminanceSource(qr.getWidth(), qr.getHeight(), pixels))
        ));
        assertEquals(appointment.getReferenceNo(), decoded.getText());

        String previewPath = System.getProperty("walkin.pass.preview");
        if (previewPath != null && !previewPath.isBlank()) {
            Path output = Path.of(previewPath);
            Files.createDirectories(output.getParent());
            Files.write(output, png);
        }
    }
}
