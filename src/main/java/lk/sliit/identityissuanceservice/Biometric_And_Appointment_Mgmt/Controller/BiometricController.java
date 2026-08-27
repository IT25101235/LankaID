package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BiometricController {

    @GetMapping("/officer/nic/biometrics")
    public String nicBiometricCollection(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }

        addBiometricPageData(model, "NIC", "LK-NIC-2026-00183", "/officer/nic/applications");
        return "officers_frontends/biometric_collection";
    }

    @GetMapping("/officer/passport/biometrics")
    public String passportBiometricCollection(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }

        addBiometricPageData(model, "Passport", "LK-PASS-2026-00041", "/officer/passport/applications");
        return "officers_frontends/biometric_collection";
    }

    private void addBiometricPageData(Model model, String documentType, String sampleReference, String queueRoute) {
        /* Mock values make QR/manual lookup demonstrable without storing biometric data yet. */
        model.addAttribute("documentType", documentType);
        model.addAttribute("sampleReference", sampleReference);
        model.addAttribute("queueRoute", queueRoute);
    }
}
