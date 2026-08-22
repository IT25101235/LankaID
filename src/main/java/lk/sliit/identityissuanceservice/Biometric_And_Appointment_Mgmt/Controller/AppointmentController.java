package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppointmentController {

    @GetMapping("/citizen/appointments/biometric")
    public String bookBiometricAppointment() {
        return "citizen_frontends/biometric_appointment_booking";
    }

    @GetMapping("/citizen/appointments/walk-in")
    public String bookWalkInAppointment() {
        return "citizen_frontends/walkin_appointment_booking";
    }

    @GetMapping("/officer/nic/appointments")
    public String nicAppointments(Model model) {
        addAppointmentPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
        return "officers_frontends/officer_appointments";
    }

    @GetMapping("/officer/passport/appointments")
    public String passportAppointments(Model model) {
        addAppointmentPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
        return "officers_frontends/officer_appointments";
    }

    @GetMapping("/officer/csr/walk-ins")
    public String manageWalkInAppointments() {
        return "officers_frontends/walkin_management";
    }

    @GetMapping("/officer/csr/walk-ins/register")
    public String registerUnscheduledWalkIn() {
        return "officers_frontends/csr_walkin_booking";
    }

    private void addAppointmentPageData(Model model, String documentType, String queueRoute, String biometricRoute) {
        model.addAttribute("documentType", documentType);
        model.addAttribute("queueRoute", queueRoute);
        model.addAttribute("biometricRoute", biometricRoute);
    }
}
