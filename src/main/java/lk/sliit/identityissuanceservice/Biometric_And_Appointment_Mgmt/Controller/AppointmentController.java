package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.AppointmentBookingDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.OfficerBiometricDetails;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentPassService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.AppointmentService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;

/**
 * Controller (MVC): citizen booking and appointment pass, plus the officer appointment history. It
 * checks the session and calls AppointmentService.
 */
@Controller
public class AppointmentController {

    // OOP - Composition and Encapsulation: the service is a private final field handed in through the
    // constructor (dependency injection), so this class is loosely coupled and the object is never created
    // here.
    private final AppointmentService appointmentService;
    private final AppointmentPassService passService;


    public AppointmentController(AppointmentService appointmentService, AppointmentPassService passService) {
        this.appointmentService = appointmentService;
        this.passService = passService;
    }

    /** READ: show the booking page for a paid application. */
    @GetMapping("/citizen/appointments/biometric")
    public String bookBiometricAppointment(
            @RequestParam(required = false) Integer applicationId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        if (applicationId == null) {
            model.addAttribute("appointmentUnavailable", true);
            return "citizen_frontends/biometric_appointment_booking";
        }

        try {
            AppointmentBookingDetails details = appointmentService.getBookingDetails(citizenId, applicationId);
            model.addAttribute("booking", details);
            model.addAttribute("appointment", details.appointment());
            // "application" is a reserved web variable in Thymeleaf, so use a clear model name.
            model.addAttribute("citizenApplication", details.application());
            model.addAttribute("centres", details.centres());
            model.addAttribute("earliestDate", details.earliestDate());
            model.addAttribute("latestDate", details.latestDate());
            return "citizen_frontends/biometric_appointment_booking";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/applications";
        }
    }

    /** CREATE: confirm the chosen slot. Errors go back to the page as messages. */
    @PostMapping("/citizen/appointments/biometric")
    public String confirmBiometricAppointment(
            @RequestParam(required = false) String applicationId,
            @RequestParam(required = false) String slotId,
            @RequestParam(required = false) String formToken,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        Integer selectedApplicationId = null;
        try {
            BiometricFormToken.check(formToken, session);
            selectedApplicationId = readInteger(applicationId, "Application details are missing. Please try again.");
            Integer selectedSlotId = readInteger(slotId, "Select an available appointment time.");
            Appointment appointment = appointmentService.bookAppointment(
                    citizenId, selectedApplicationId, selectedSlotId);
            redirectAttributes.addFlashAttribute("appointmentMessage", "Appointment confirmed successfully.");
            return "redirect:/citizen/appointments/" + appointment.getReferenceNo() + "/pass";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("appointmentError", exception.getMessage());
            if (selectedApplicationId == null) return "redirect:/citizen/applications";
            return "redirect:/citizen/appointments/biometric?applicationId=" + selectedApplicationId;
        }
    }

    /** READ (JSON): free future slots grouped by date for the calendar, within the booking window. */
    @GetMapping("/citizen/appointments/biometric/availability")
    public ResponseEntity<?> availability(
            @RequestParam Integer applicationId,
            @RequestParam Integer centreId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpSession session
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return ResponseEntity.status(401).build();
        try {
            var details = appointmentService.getBookingDetails(citizenId, applicationId);
            LocalDate from = readDate(startDate);
            LocalDate to = readDate(endDate);
            details.centres().stream().filter(c -> centreId.equals(c.getCentreId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Select a valid centre."));
            if (from.isBefore(details.earliestDate()) || to.isAfter(details.latestDate()) || to.isBefore(from)) {
                throw new IllegalArgumentException("Choose a valid calendar range within the booking window.");
            }
            var schedule = appointmentService.getAvailableSchedule(
                    details.application().getServiceType(), centreId, from, to);
            var grouped = new LinkedHashMap<LocalDate, java.util.List<java.util.Map<String, Object>>>();
            LocalDateTime now = LocalDateTime.now();
            for (var slot : schedule) {
                if (!slot.available() || slot.remainingPlaces() < 1
                        || !slot.slotDate().atTime(slot.slotTime()).isAfter(now)) continue;
                grouped.computeIfAbsent(slot.slotDate(), ignored -> new java.util.ArrayList<>()).add(
                        java.util.Map.of(
                                "slotId", slot.slotId(),
                                "time", slot.slotTime().toString(),
                                "remaining", slot.remainingPlaces()
                        ));
            }
            var dates = grouped.entrySet().stream().map(entry -> java.util.Map.of(
                    "date", entry.getKey().toString(),
                    "remaining", entry.getValue().stream()
                            .mapToInt(slot -> (Integer) slot.get("remaining")).sum(),
                    "slots", entry.getValue()
            )).toList();
            return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(java.util.Map.of("dates", dates));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

    /** READ: show the appointment pass (only its owner can open it). */
    @GetMapping("/citizen/appointments/{reference}/pass")
    public String appointmentPass(
            @PathVariable String reference,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";
        try {
            model.addAttribute("pass", appointmentService.getCitizenPassDetails(citizenId, reference));
            return "citizen_frontends/biometric_appointment_pass";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/applications";
        }
    }

    /** Returns the QR code image. */
    @GetMapping("/citizen/appointments/{reference}/qr")
    public ResponseEntity<byte[]> appointmentQrCode(@PathVariable String reference, HttpSession session) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return ResponseEntity.status(401).build();
        try {
            OfficerBiometricDetails details = appointmentService.getCitizenPassDetails(citizenId, reference);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(passService.createQrCode(details.appointment().getReferenceNo(), 420));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Returns the pass as a PNG download. */
    @GetMapping("/citizen/appointments/{reference}/pass/download")
    public ResponseEntity<byte[]> downloadAppointmentPass(@PathVariable String reference, HttpSession session) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return ResponseEntity.status(401).build();
        try {
            OfficerBiometricDetails details = appointmentService.getCitizenPassDetails(citizenId, reference);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment()
                                    .filename("LankaID-Appointment-" + details.appointment().getReferenceNo() + ".png")
                                    .build().toString()
                    )
                    .body(passService.createDownloadablePass(details));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    /** Officer page: appointment history for NIC applications. */
    @GetMapping("/officer/nic/appointments")
    public String nicAppointments(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccess(session, OfficerAccountService.NIC_OFFICER)) return "redirect:/login";
        addAppointmentPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
        model.addAttribute("appointments", appointmentService.getOfficerHistory("NIC"));
        return "officers_frontends/officer_appointments";
    }

    /** Officer page: appointment history for passport applications. */
    @GetMapping("/officer/passport/appointments")
    public String passportAppointments(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccess(session, OfficerAccountService.PASSPORT_OFFICER)) return "redirect:/login";
        addAppointmentPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
        model.addAttribute("appointments", appointmentService.getOfficerHistory("PASSPORT"));
        return "officers_frontends/officer_appointments";
    }

    private void addAppointmentPageData(Model model, String documentType, String queueRoute, String biometricRoute) {
        model.addAttribute("documentType", documentType);
        model.addAttribute("queueRoute", queueRoute);
        model.addAttribute("biometricRoute", biometricRoute);
        String servicePath = documentType.toLowerCase();
        model.addAttribute("historyRoute", "/officer/" + servicePath + "/appointments");
        model.addAttribute("scheduleRoute", "/officer/" + servicePath + "/appointments/schedule");
    }

    /** Role check: the matching service officer or the director. */
    private boolean canAccess(HttpSession session, String requiredRole) {
        Object role = session.getAttribute("loggedInOfficerRole");
        return requiredRole.equals(role) || OfficerAccountService.DIRECTOR.equals(role);
    }

    /** Session check: returns the citizen id, or null if nobody (or a non-citizen) is logged in. */
    private Integer loggedInCitizenId(HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return userId instanceof Integer id && "CITIZEN".equals(role) ? id : null;
    }

    /** Input validation: form text to a number, or a friendly error. */
    private Integer readInteger(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(errorMessage);
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /** Input validation: form text to a date, or a friendly error. */
    private LocalDate readDate(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Select an appointment date.");
        }
        try {
            return LocalDate.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Select a valid appointment date.");
        }
    }

}
