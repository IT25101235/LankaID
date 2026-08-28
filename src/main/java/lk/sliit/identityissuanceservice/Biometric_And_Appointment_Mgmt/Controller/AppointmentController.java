package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AppointmentController {

    @GetMapping("/citizen/appointments/biometric")
    public String bookBiometricAppointment(HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        return "citizen_frontends/biometric_appointment_booking";
    }

    @GetMapping("/citizen/appointments/walk-in")
    public String bookWalkInAppointment(HttpSession session) {
        if (session.getAttribute("loggedInUserId") == null) {
            return "redirect:/login";
        }

        return "citizen_frontends/walkin_appointment_booking";
    }

    @GetMapping("/officer/nic/appointments")
    public String nicAppointments(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccess(session, OfficerAccountService.NIC_OFFICER)) {
            return "redirect:/login";
        }

        addAppointmentPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
        return "officers_frontends/officer_appointments";
    }

    @GetMapping("/officer/passport/appointments")
    public String passportAppointments(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccess(session, OfficerAccountService.PASSPORT_OFFICER)) {
            return "redirect:/login";
        }

        addAppointmentPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
        return "officers_frontends/officer_appointments";
    }

    @GetMapping("/officer/csr/walk-ins")
    public String manageWalkInAppointments(HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccess(session, OfficerAccountService.CSR)) {
            return "redirect:/login";
        }

        return "officers_frontends/walkin_management";
    }

    @GetMapping("/officer/csr/walk-ins/register")
    public String registerUnscheduledWalkIn(HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!canAccess(session, OfficerAccountService.CSR)) {
            return "redirect:/login";
        }

        return "officers_frontends/csr_walkin_booking";
    }

    private void addAppointmentPageData(Model model, String documentType, String queueRoute, String biometricRoute) {
        model.addAttribute("documentType", documentType);
        model.addAttribute("queueRoute", queueRoute);
        model.addAttribute("biometricRoute", biometricRoute);
    }

    private boolean canAccess(HttpSession session, String requiredRole) {
        Object role = session.getAttribute("loggedInOfficerRole");
        return requiredRole.equals(role) || OfficerAccountService.DIRECTOR.equals(role);
    }
}
