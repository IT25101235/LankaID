package lk.sliit.identityissuanceservice;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.OfficerBiometricDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentPassService;
import lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Model.CitizenApplication;
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

class AppointmentPassServiceTests {

    @Test
    void nicAndPassportDownloadsShareGovernmentThemeAndUseLargerQrCodes() throws Exception {
        AppointmentPassService passes = new AppointmentPassService();

        for (String serviceType : new String[]{"NIC", "PASSPORT"}) {
            OfficerBiometricDetails details = details(serviceType);
            byte[] png = passes.createDownloadablePass(details);
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));

            assertNotNull(image);
            assertEquals(900, image.getWidth());
            assertEquals(1200, image.getHeight());
            assertEquals(new Color(104, 16, 66).getRGB(), image.getRGB(70, 100));
            assertEquals(new Color(212, 167, 44).getRGB(), image.getRGB(70, 286));
            assertTrue(png.length > 40_000);

            BufferedImage qr = image.getSubimage(240, 347, 420, 420);
            int[] pixels = qr.getRGB(0, 0, qr.getWidth(), qr.getHeight(), null, 0, qr.getWidth());
            Result decoded = new QRCodeReader().decode(new BinaryBitmap(
                    new HybridBinarizer(new RGBLuminanceSource(qr.getWidth(), qr.getHeight(), pixels))
            ));
            assertEquals(details.appointment().getReferenceNo(), decoded.getText());

            String previewDirectory = System.getProperty("appointment.pass.preview.directory");
            if (previewDirectory != null && !previewDirectory.isBlank()) {
                Path directory = Path.of(previewDirectory);
                Files.createDirectories(directory);
                Files.write(directory.resolve(serviceType.toLowerCase() + "-biometric-pass.png"), png);
            }
        }
    }

    private OfficerBiometricDetails details(String serviceType) {
        Appointment appointment = new Appointment();
        appointment.setReferenceNo("BIO-2026-" + serviceType);
        appointment.setScheduledAt(LocalDateTime.of(2026, 10, 30, 10, 30));
        appointment.setAppointmentStatus("CONFIRMED");

        CitizenApplication application = new CitizenApplication();
        application.setApplicationNo("APP-2026-" + serviceType);
        application.setServiceType(serviceType);

        Centre centre = new Centre();
        centre.setCentreName("Colombo Main Centre");

        return new OfficerBiometricDetails(
                appointment,
                application,
                null,
                centre,
                "Aakil Ahamed",
                false,
                false
        );
    }
}
