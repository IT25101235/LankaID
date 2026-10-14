package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BiometricService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BookingChangePolicy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller: officer biometric desk. Find the appointment, check the citizen in, then complete
 * collection.
 * <p>
 * Design Pattern - MVC and Post/Redirect/Get: the page is the View, and every POST redirects back to it.<br>
 * Exception handling: IllegalArgumentException (a broken rule) and DataAccessException (a database
 * problem) are caught and shown as a message on the page. An unknown service in the URL returns HTTP 404.
 */
@Controller
public class BiometricController {

    // OOP - Composition and Encapsulation: the services are private final fields handed in through the
    // constructor (Design Pattern - Dependency Injection), so this class is loosely coupled and the objects
    // are never created here.
    private final AppointmentService appointmentService;
    private final BiometricService biometricService;

    public BiometricController(AppointmentService appointmentService, BiometricService biometricService) {
        this.appointmentService = appointmentService;
        this.biometricService = biometricService;
    }

    /** READ: NIC officer queue and the selected appointment. */
    @GetMapping("/officer/nic/biometrics")
    public String nicBiometricCollection(
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "REFERENCE_NUMBER") String checkInMethod,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return biometricPage("NIC", reference, checkInMethod, model, session, redirectAttributes);
    }

    /** READ: passport officer queue and the selected appointment. */
    @GetMapping("/officer/passport/biometrics")
    public String passportBiometricCollection(
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "REFERENCE_NUMBER") String checkInMethod,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return biometricPage("PASSPORT", reference, checkInMethod, model, session, redirectAttributes);
    }

    /** UPDATE: mark the appointment CHECKED_IN. Role and form token are checked first. */
    @PostMapping("/officer/{service}/biometrics/{reference}/check-in")
    public String checkIn(
            @PathVariable String service,
            @PathVariable String reference,
            @RequestParam(defaultValue = "REFERENCE_NUMBER") String checkInMethod,
            @RequestParam(required = false) String formToken,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String serviceType = serviceType(service);
        String accessResult = checkAccess(session, serviceType);
        if (accessResult != null) return accessResult;
        try {
            BiometricFormToken.check(formToken, session);
            appointmentService.checkIn(reference, serviceType, officerId(session), checkInMethod);
            redirectAttributes.addFlashAttribute("biometricMessage", "Citizen checked in successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("biometricError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("biometricError",
                    "The change could not be saved. Reload the appointment and try again.");
        }
        return "redirect:/officer/" + serviceType.toLowerCase() + "/biometrics?reference="
                + java.net.URLEncoder.encode(reference, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** UPDATE: move a missed booking to the selected slot. Role and form token are checked first. */
    @PostMapping("/officer/{service}/biometrics/{reference}/reschedule-missed")
    public String rescheduleMissed(
            @PathVariable String service,
            @PathVariable String reference,
            @RequestParam(required = false) String slotId,
            @RequestParam(required = false) String formToken,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String serviceType = serviceType(service);
        String accessResult = checkAccess(session, serviceType);
        if (accessResult != null) return accessResult;
        try {
            BiometricFormToken.check(formToken, session);
            var details = appointmentService.rescheduleMissed(
                    reference, serviceType, officerId(session), readSlotId(slotId));
            redirectAttributes.addFlashAttribute(
                    "biometricMessage",
                    "Missed appointment moved to " + details.appointment().getScheduledAt()
                            + ". The same QR pass remains valid."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("biometricError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("biometricError",
                    "The change could not be saved. Reload the appointment and try again.");
        }
        return "redirect:/officer/" + serviceType.toLowerCase() + "/biometrics?reference="
                + java.net.URLEncoder.encode(reference, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** CREATE: save the biometric collection and finish the appointment. */
    @PostMapping("/officer/{service}/biometrics/{reference}/complete")
    public String completeBiometrics(
            @PathVariable String service,
            @PathVariable String reference,
            @RequestParam(required = false) String formToken,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String serviceType = serviceType(service);
        String accessResult = checkAccess(session, serviceType);
        if (accessResult != null) return accessResult;
        try {
            BiometricFormToken.check(formToken, session);
            biometricService.completeCollection(reference, serviceType, officerId(session));
            redirectAttributes.addFlashAttribute(
                    "biometricMessage",
                    "Biometrics attached. The application is ready for printing."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("biometricError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("biometricError",
                    "The change could not be saved. Reload the appointment and try again.");
        }
        return "redirect:/officer/" + serviceType.toLowerCase() + "/biometrics?reference="
                + java.net.URLEncoder.encode(reference, java.nio.charset.StandardCharsets.UTF_8);
    }

    private String biometricPage(
            String serviceType,
            String reference,
            String checkInMethod,
            Model model,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessResult = checkAccess(session, serviceType);
        if (accessResult != null) return accessResult;

        addBiometricPageData(model, serviceType);
        model.addAttribute("checkInMethod", "QR".equals(checkInMethod) ? "QR" : "REFERENCE_NUMBER");
        model.addAttribute("appointments", appointmentService.getOfficerQueue(serviceType));
        if (reference != null && !reference.isBlank()) {
            try {
                var selected = appointmentService.findForOfficer(reference, serviceType);
                model.addAttribute("selectedAppointment", selected);
                model.addAttribute("selectedAppointmentToday", BookingChangePolicy.isToday(selected.appointment()));
                model.addAttribute("selectedAppointmentMissed", BookingChangePolicy.isMissed(selected.appointment()));
                if (BookingChangePolicy.isMissed(selected.appointment()) && !selected.checkedIn()
                        && !selected.biometricCollected()) {
                    var slots = appointmentService.getMissedRescheduleSlots(reference, serviceType);
                    model.addAttribute("rescheduleSlots", slots);
                    model.addAttribute("rescheduleDates", slots.stream()
                            .map(slot -> slot.slotDate()).distinct().toList());
                }
            } catch (IllegalArgumentException exception) {
                redirectAttributes.addFlashAttribute("biometricError", exception.getMessage());
                return "redirect:/officer/" + serviceType.toLowerCase() + "/biometrics";
            }
        }
        return "officers_frontends/biometric_collection";
    }

    private void addBiometricPageData(Model model, String serviceType) {
        model.addAttribute("documentType", "PASSPORT".equals(serviceType) ? "Passport" : "NIC");
        model.addAttribute("servicePath", serviceType.toLowerCase());
        model.addAttribute("queueRoute", "/officer/" + serviceType.toLowerCase() + "/applications");
    }

    /** Role check (authorisation): the matching service officer or the director, otherwise back to login. */
    private String checkAccess(HttpSession session, String serviceType) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        Object role = session.getAttribute("loggedInOfficerRole");
        String requiredRole = "NIC".equals(serviceType)
                ? OfficerAccountService.NIC_OFFICER
                : OfficerAccountService.PASSPORT_OFFICER;
        return requiredRole.equals(role) || OfficerAccountService.DIRECTOR.equals(role)
                ? null : "redirect:/login";
    }

    /**
     * Input validation: turns the chosen slot text into a number. A missing value is left for the service
     * to report, and a value that is not a number gives the same friendly message instead of HTTP 400.
     */
    private Integer readSlotId(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Select an available date and time.");
        }
    }

    private Integer officerId(HttpSession session) {
        return session.getAttribute("loggedInOfficerId") instanceof Integer id ? id : null;
    }

    /** Input validation: only nic or passport is accepted. Exception handling: anything else is HTTP 404. */
    private String serviceType(String service) {
        if ("nic".equalsIgnoreCase(service)) return "NIC";
        if ("passport".equalsIgnoreCase(service)) return "PASSPORT";
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Unknown biometric service.");
    }
}
