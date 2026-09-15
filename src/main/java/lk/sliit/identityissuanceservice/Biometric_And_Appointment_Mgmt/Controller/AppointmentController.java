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
import java.time.LocalTime;

@Controller
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentPassService passService;


    public AppointmentController(AppointmentService appointmentService, AppointmentPassService passService) {
        this.appointmentService = appointmentService;
        this.passService = passService;
    }

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

    @PostMapping("/citizen/appointments/biometric")
    public String confirmBiometricAppointment(
            @RequestParam(required = false) String applicationId,
            @RequestParam(required = false) String centreId,
            @RequestParam(required = false) String appointmentDate,
            @RequestParam(required = false) String appointmentTime,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        Integer selectedApplicationId = null;
        try {
            selectedApplicationId = readInteger(applicationId, "Application details are missing. Please try again.");
            Integer selectedCentreId = readInteger(centreId, "Select a service centre.");
            LocalDate selectedDate = readDate(appointmentDate);
            LocalTime selectedTime = readTime(appointmentTime);
            Appointment appointment = appointmentService.bookAppointment(
                    citizenId, selectedApplicationId, selectedCentreId, selectedDate, selectedTime);
            redirectAttributes.addFlashAttribute("appointmentMessage", "Appointment confirmed successfully.");
            return "redirect:/citizen/appointments/" + appointment.getReferenceNo() + "/pass";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("appointmentError", exception.getMessage());
            if (selectedApplicationId == null) return "redirect:/citizen/applications";
            return "redirect:/citizen/appointments/biometric?applicationId=" + selectedApplicationId;
        }
    }

    @GetMapping("/citizen/appointments/biometric/availability")
    public ResponseEntity<?> availability(@RequestParam Integer applicationId, @RequestParam Integer centreId,
            @RequestParam String date, HttpSession session) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return ResponseEntity.status(401).build();
        try {
            var details = appointmentService.getBookingDetails(citizenId, applicationId);
            LocalDate selectedDate = readDate(date);
            if (selectedDate.isBefore(details.earliestDate()) || selectedDate.isAfter(details.latestDate())) {
                throw new IllegalArgumentException("Choose a date within the booking window.");
            }
            var centre = details.centres().stream().filter(c -> centreId.equals(c.getCentreId())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Select a valid centre."));
            int remaining = appointmentService.remainingCapacity(centre, selectedDate, details.appointment().getAppointmentId());
            var times = remaining == 0 ? java.util.List.<String>of() : java.util.List.of(
                    "09:00", "09:30", "10:00", "10:30", "11:00", "11:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30", "16:00")
                    .stream().filter(time -> appointmentService.remainingSlotCapacity(centre,
                            selectedDate.atTime(LocalTime.parse(time)), details.appointment().getAppointmentId()) > 0).toList();
            return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(java.util.Map.of("remaining", remaining, "times", times));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", exception.getMessage()));
        }
    }

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

    @GetMapping("/citizen/appointments/{reference}/qr")
    public ResponseEntity<byte[]> appointmentQrCode(@PathVariable String reference, HttpSession session) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return ResponseEntity.status(401).build();
        appointmentService.getCitizenPassDetails(citizenId, reference);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(passService.createQrCode(reference, 420));
    }

    @GetMapping("/citizen/appointments/{reference}/pass/download")
    public ResponseEntity<byte[]> downloadAppointmentPass(@PathVariable String reference, HttpSession session) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return ResponseEntity.status(401).build();
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
    }

    @GetMapping("/officer/nic/appointments")
    public String nicAppointments(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccess(session, OfficerAccountService.NIC_OFFICER)) return "redirect:/login";
        addAppointmentPageData(model, "NIC", "/officer/nic/applications", "/officer/nic/biometrics");
        model.addAttribute("appointments", appointmentService.getOfficerQueue("NIC"));
        return "officers_frontends/officer_appointments";
    }

    @GetMapping("/officer/passport/appointments")
    public String passportAppointments(Model model, HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccess(session, OfficerAccountService.PASSPORT_OFFICER)) return "redirect:/login";
        addAppointmentPageData(model, "Passport", "/officer/passport/applications", "/officer/passport/biometrics");
        model.addAttribute("appointments", appointmentService.getOfficerQueue("PASSPORT"));
        return "officers_frontends/officer_appointments";
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

    private Integer loggedInCitizenId(HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return userId instanceof Integer id && "CITIZEN".equals(role) ? id : null;
    }

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

    private LocalTime readTime(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Select an appointment time.");
        }
        try {
            return LocalTime.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Select a valid appointment time.");
        }
    }
}
