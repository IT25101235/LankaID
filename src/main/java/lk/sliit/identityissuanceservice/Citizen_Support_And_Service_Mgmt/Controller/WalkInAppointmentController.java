package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller;

import lk.sliit.identityissuanceservice.User_Mgmt.Controller.DeletionFormToken;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BookingChangePolicy;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInAppointmentView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInAppointmentService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInPassService;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.WalkInPaymentService;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

@Controller
public class WalkInAppointmentController {

    private final WalkInAppointmentService walkInService;
    private final WalkInPassService passService;

    public WalkInAppointmentController(
            WalkInAppointmentService walkInService,
            WalkInPassService passService
    ) {
        this.walkInService = walkInService;
        this.passService = passService;
    }

    @ModelAttribute("walkInFee")
    public java.math.BigDecimal walkInFee() {
        return WalkInPaymentService.WALK_IN_FEE;
    }

    @GetMapping({"/walk-in", "/citizen/appointments/walk-in"})
    public String walkInStart(Model model) {
        model.addAttribute("step", "PHONE");
        return "citizen_frontends/walkin_appointment_booking";
    }

    @PostMapping("/walk-in/otp/send")
    public String sendOtp(
            @RequestParam String visitorName,
            @RequestParam String visitorPhone,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            walkInService.startPhoneVerification(visitorName, visitorPhone, session);
            redirectAttributes.addFlashAttribute(
                    "walkInMessage",
                    "Your SMS verification code was requested. Enter it when it arrives."
            );
            return "redirect:/walk-in/verify";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
            redirectAttributes.addFlashAttribute("retryAfterSeconds", walkInService.otpRetryAfterSeconds(visitorPhone, session));
            redirectAttributes.addFlashAttribute("visitorName", visitorName);
            redirectAttributes.addFlashAttribute("visitorPhone", visitorPhone);
            return "redirect:/walk-in";
        }
    }

    @GetMapping("/walk-in/verify")
    public String verifyPage(HttpSession session, Model model) {
        if (walkInService.sessionPhone(session).isBlank()) return "redirect:/walk-in";
        model.addAttribute("step", "OTP");
        model.addAttribute("visitorPhone", walkInService.sessionPhone(session));
        model.addAttribute("retryAfterSeconds", walkInService.otpRetryAfterSeconds(walkInService.sessionPhone(session), session));
        return "citizen_frontends/walkin_appointment_booking";
    }

    @PostMapping("/walk-in/otp/resend")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            walkInService.resendPhoneVerification(session);
            redirectAttributes.addFlashAttribute("walkInMessage", "A new SMS code was requested. Use the latest code.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        }
        return "redirect:/walk-in/verify";
    }

    @PostMapping("/walk-in/otp/verify")
    public String verifyOtp(
            @RequestParam String otp,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            walkInService.verifyPhone(otp, session);
            redirectAttributes.addFlashAttribute("walkInMessage", "Phone number verified successfully.");
            return "redirect:/walk-in/book";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
            return "redirect:/walk-in/verify";
        }
    }

    @GetMapping("/walk-in/book")
    public String bookingPage(HttpSession session, Model model) {
        if (!walkInService.isPhoneVerified(session)) return "redirect:/walk-in";
        model.addAttribute("step", "BOOK");
        model.addAttribute("visitorName", walkInService.sessionName(session));
        model.addAttribute("visitorPhone", walkInService.sessionPhone(session));
        model.addAttribute("slots", walkInService.getBookableSlots());
        return "citizen_frontends/walkin_appointment_booking";
    }

    @PostMapping("/walk-in/book")
    public String book(
            @RequestParam String visitorService,
            @RequestParam Integer slotId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        try {
            walkInService.preparePayment(visitorService, slotId, session);
            return "redirect:/walk-in/payment";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
            return "redirect:/walk-in/book";
        }
    }

    @GetMapping("/walk-in/payment")
    public String walkInPaymentPage(HttpSession session, Model model,
                                    HttpServletResponse response, RedirectAttributes redirectAttributes) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        try {
            model.addAttribute("paymentDetails", walkInService.getPaymentDetails(session));
            model.addAttribute("step", "PAYMENT");
            return "citizen_frontends/walkin_appointment_booking";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
            return "redirect:/walk-in/book";
        }
    }

    @PostMapping("/walk-in/payment")
    public String payForWalkIn(@RequestParam String token, HttpSession session,
                              RedirectAttributes redirectAttributes) {
        try {
            WalkInAppointmentView created = walkInService.payAndBook(token, session);
            redirectAttributes.addFlashAttribute("walkInMessage", "Payment recorded. Your walk-in appointment is confirmed.");
            return "redirect:/walk-in/pass/" + created.appointment().getReferenceNo();
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirectAttributes.addFlashAttribute("walkInError", "Payment could not be saved. Please try again.");
        }
        return "redirect:/walk-in/payment";
    }

    @GetMapping("/walk-in/pass/{reference}")
    public String pass(
            @org.springframework.web.bind.annotation.PathVariable String reference,
            Model model,
            HttpSession session,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes
    ) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        try {
            WalkInAppointmentView details = walkInService.getVisitorPass(reference, session);
            if ("CANCELLED".equals(details.appointment().getAppointmentStatus())) {
                redirectAttributes.addFlashAttribute("walkInError", "This booking is cancelled. Its old QR pass is no longer valid.");
                return "redirect:/walk-in/recover";
            }
            model.addAttribute("walkIn", details);
            model.addAttribute("walkInPayment", walkInService.getPaidPayment(details.appointment().getAppointmentId()));
            model.addAttribute("serviceLabel", walkInService.serviceLabel(
                    details.appointment().getVisitorService()
            ));
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", "Verify your mobile number to access this pass.");
            return "redirect:/walk-in/recover";
        }
        return "citizen_frontends/walkin_appointment_pass";
    }

    @GetMapping("/walk-in/pass/{reference}/qr")
    public ResponseEntity<byte[]> qr(
            @org.springframework.web.bind.annotation.PathVariable String reference,
            HttpSession session
    ) {
        WalkInAppointmentView details;
        try {
            details = walkInService.getVisitorPass(reference, session);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(403).header(HttpHeaders.CACHE_CONTROL, "no-store").build();
        }
        if ("CANCELLED".equals(details.appointment().getAppointmentStatus())) {
            return ResponseEntity.status(410).header(HttpHeaders.CACHE_CONTROL, "no-store").build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(passService.createQrCode(details.appointment().getReferenceNo(), 420));
    }

    @GetMapping("/walk-in/pass/{reference}/download")
    public ResponseEntity<byte[]> download(
            @org.springframework.web.bind.annotation.PathVariable String reference,
            HttpSession session
    ) {
        WalkInAppointmentView details;
        try {
            details = walkInService.getVisitorPass(reference, session);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.status(403).header(HttpHeaders.CACHE_CONTROL, "no-store").build();
        }
        if ("CANCELLED".equals(details.appointment().getAppointmentStatus())) {
            return ResponseEntity.status(410).header(HttpHeaders.CACHE_CONTROL, "no-store").build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("LankaID-WalkIn-" + details.appointment().getReferenceNo() + ".png")
                                .build().toString()
                )
                .body(passService.createPass(details));
    }

    @GetMapping("/walk-in/recover")
    public String recover(
            @RequestParam(required = false) String phone,
            Model model,
            HttpSession session,
            HttpServletResponse response
    ) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        model.addAttribute("step", "PHONE");
        if (phone != null) model.addAttribute("phone", phone);
        // A URL phone parameter can only prefill the form, never select whose data is read.
        if (phone == null && !model.containsAttribute("phone") && walkInService.isRecoveryVerified(session)) {
            try {
                model.addAttribute("walkIns", walkInService.recoverVerifiedBookings(session));
                model.addAttribute("step", "RESULTS");
                model.addAttribute("phone", walkInService.recoveryPhone(session));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("walkInError", exception.getMessage());
            }
        }
        if (!model.containsAttribute("phone")) model.addAttribute("phone", "");
        model.addAttribute("retryAfterSeconds", walkInService.recoveryRetryAfterSeconds(
                String.valueOf(model.getAttribute("phone")), session));
        return "citizen_frontends/walkin_appointment_recovery";
    }

    @PostMapping("/walk-in/recover/otp/send")
    public String sendRecoveryOtp(@RequestParam String phone, HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            walkInService.startRecoveryVerification(phone, session);
            redirectAttributes.addFlashAttribute("walkInMessage", "Enter the SMS code to retrieve your bookings.");
            return "redirect:/walk-in/recover/verify";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("phone", phone);
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
            return "redirect:/walk-in/recover";
        }
    }

    @GetMapping("/walk-in/recover/verify")
    public String recoveryVerifyPage(HttpSession session, Model model, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        if (walkInService.recoveryPhone(session).isBlank()) return "redirect:/walk-in/recover";
        if (walkInService.isRecoveryVerified(session)) return "redirect:/walk-in/recover";
        model.addAttribute("step", "OTP");
        model.addAttribute("phone", walkInService.recoveryPhone(session));
        model.addAttribute("retryAfterSeconds", walkInService.recoveryRetryAfterSeconds(
                walkInService.recoveryPhone(session), session));
        return "citizen_frontends/walkin_appointment_recovery";
    }

    @PostMapping("/walk-in/recover/otp/verify")
    public String verifyRecoveryOtp(@RequestParam String otp, HttpSession session,
                                    RedirectAttributes redirectAttributes) {
        try {
            walkInService.verifyRecovery(otp, session);
            return "redirect:/walk-in/recover";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
            return "redirect:/walk-in/recover/verify";
        }
    }

    @PostMapping("/walk-in/recover/otp/resend")
    public String resendRecoveryOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            walkInService.startRecoveryVerification(walkInService.recoveryPhone(session), session);
            redirectAttributes.addFlashAttribute("walkInMessage", "A new SMS code was requested. Use the latest code.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        }
        return "redirect:/walk-in/recover/verify";
    }

    @PostMapping("/walk-in/recover/reset")
    public String resetRecovery(HttpSession session) {
        walkInService.clearRecovery(session);
        return "redirect:/walk-in/recover";
    }

    @GetMapping("/officer/csr/walk-ins")
    public String csrWorkbench(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String reference,
            @RequestParam(name = "walkInService", required = false) String walkInServiceFilter,
            @RequestParam(required = false) Integer scheduleCentreId,
            @RequestParam(required = false) String scheduleMonth,
            HttpSession session,
            Model model
    ) {
        String accessRedirect = csrAccessRedirect(session);
        if (accessRedirect != null) return accessRedirect;
        Page<WalkInAppointmentView> queue = walkInService.getCsrQueue(page, walkInServiceFilter);
        model.addAttribute("walkIns", queue.getContent());
        model.addAttribute("currentPage", queue.getNumber());
        model.addAttribute("totalPages", queue.getTotalPages());
        model.addAttribute("totalWalkIns", queue.getTotalElements());
        model.addAttribute("hasPrevious", queue.hasPrevious());
        model.addAttribute("hasNext", queue.hasNext());
        model.addAttribute(
                "walkInService",
                walkInServiceFilter == null ? "" : walkInServiceFilter
        );
        model.addAttribute("centres", walkInService.getCentres());
        LocalDate minimumDate = LocalDate.now();
        YearMonth selectedMonth = parseScheduleMonth(scheduleMonth);
        LocalDate selectedFrom = selectedMonth.atDay(1);
        LocalDate selectedTo = selectedMonth.atEndOfMonth();
        try {
            model.addAttribute(
                    "schedule",
                    walkInService.getPublishedSchedule(
                            scheduleCentreId,
                            selectedFrom,
                            selectedTo,
                            null
                    )
            );
        } catch (IllegalArgumentException exception) {
            model.addAttribute("walkInError", exception.getMessage());
            model.addAttribute("schedule", java.util.List.of());
        }
        model.addAttribute("minimumDate", minimumDate);
        model.addAttribute("scheduleCentreId", scheduleCentreId);
        model.addAttribute("scheduleMonth", selectedMonth.toString());
        if (reference != null && !reference.isBlank()) {
            try {
                var selected = walkInService.findByReference(reference);
                model.addAttribute("selectedWalkIn", selected);
                model.addAttribute("selectedWalkInToday", BookingChangePolicy.isToday(selected.appointment()));
                model.addAttribute("selectedWalkInMissed", BookingChangePolicy.isMissed(selected.appointment()));
            } catch (IllegalArgumentException exception) {
                model.addAttribute("walkInError", exception.getMessage());
            }
        }
        addOfficer(model, session);
        return "officers_frontends/walkin_management";
    }

    @GetMapping("/officer/csr/walk-ins/register")
    public String csrRegister(HttpSession session, Model model) {
        String accessRedirect = csrAccessRedirect(session);
        if (accessRedirect != null) return accessRedirect;
        model.addAttribute("step", "PHONE");
        return "citizen_frontends/walkin_appointment_booking";
    }

    @PostMapping("/officer/csr/walk-ins/schedule/day")
    public String publishDateRange(
            @RequestParam Integer centreId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime openingTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime closingTime,
            @RequestParam Integer capacity,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessRedirect = csrAccessRedirect(session);
        if (accessRedirect != null) return accessRedirect;
        try {
            int days = walkInService.publishDateRange(
                    centreId,
                    startDate,
                    endDate,
                    openingTime,
                    closingTime,
                    capacity
            );
            redirectAttributes.addFlashAttribute(
                    "walkInMessage",
                    days + " day(s) were published with half-hour slots."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        }
        redirectAttributes.addAttribute("scheduleOpen", true);
        addScheduleFilters(redirectAttributes, centreId, startDate);
        return "redirect:/officer/csr/walk-ins";
    }

    @PostMapping("/officer/csr/walk-ins/schedule/day/close")
    public String closeDateRange(
            @RequestParam Integer centreId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessRedirect = csrAccessRedirect(session);
        if (accessRedirect != null) return accessRedirect;
        try {
            int days = walkInService.closeDateRange(centreId, startDate, endDate);
            redirectAttributes.addFlashAttribute(
                    "walkInMessage",
                    days + " published day(s) were closed and affected bookings were cancelled."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        }
        redirectAttributes.addAttribute("scheduleOpen", true);
        addScheduleFilters(redirectAttributes, centreId, startDate);
        return "redirect:/officer/csr/walk-ins";
    }

    @PostMapping("/officer/csr/walk-ins/schedule/slot")
    public String updateSlot(
            @RequestParam Integer slotId,
            @RequestParam Integer capacity,
            @RequestParam(defaultValue = "false") boolean available,
            @RequestParam(required = false) Integer scheduleCentreId,
            @RequestParam(required = false) String scheduleMonth,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessRedirect = csrAccessRedirect(session);
        if (accessRedirect != null) return accessRedirect;
        try {
            walkInService.updateSlot(slotId, capacity, available);
            redirectAttributes.addFlashAttribute("walkInMessage", "Slot capacity and availability updated.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        }
        if (scheduleCentreId != null) {
            redirectAttributes.addAttribute("scheduleCentreId", scheduleCentreId);
        }
        redirectAttributes.addAttribute("scheduleOpen", true);
        redirectAttributes.addAttribute("scheduleMonth", parseScheduleMonth(scheduleMonth).toString());
        return "redirect:/officer/csr/walk-ins";
    }

    @PostMapping("/officer/csr/walk-ins/schedule/slot/{slotId}/delete")
    public String deleteSlot(@org.springframework.web.bind.annotation.PathVariable Integer slotId,
            @RequestParam(required = false) String deleteToken,
            @RequestParam(required = false) Integer scheduleCentreId, @RequestParam(required = false) String scheduleMonth,
            HttpSession session, RedirectAttributes redirect) {
        String access = csrAccessRedirect(session);
        if (access != null) return access;
        try {
            DeletionFormToken.check(deleteToken, session);
            walkInService.deleteUnusedSlot(slotId);
            redirect.addFlashAttribute("walkInMessage", "Unused slot deleted.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("walkInError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirect.addFlashAttribute("walkInError", "The slot could not be deleted. Reload the schedule and try again.");
        }
        redirect.addAttribute("scheduleOpen", true);
        if (scheduleCentreId != null) redirect.addAttribute("scheduleCentreId", scheduleCentreId);
        redirect.addAttribute("scheduleMonth", parseScheduleMonth(scheduleMonth).toString());
        return "redirect:/officer/csr/walk-ins";
    }

    @PostMapping("/officer/csr/walk-ins/check-in")
    public String checkIn(
            @RequestParam String reference,
            @RequestParam(defaultValue = "REFERENCE_NUMBER") String method,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessRedirect = csrAccessRedirect(session);
        if (accessRedirect != null) return accessRedirect;
        try {
            walkInService.checkIn(reference, officerId(session), method);
            redirectAttributes.addFlashAttribute("walkInMessage", "Visitor checked in successfully.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        }
        return "redirect:/officer/csr/walk-ins?reference=" + reference.trim();
    }

    @PostMapping("/officer/csr/walk-ins/reschedule-missed")
    public String rescheduleMissed(
            @RequestParam String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessRedirect = csrAccessRedirect(session);
        if (accessRedirect != null) return accessRedirect;
        try {
            var updated = walkInService.rescheduleMissed(reference, officerId(session));
            redirectAttributes.addFlashAttribute(
                    "walkInMessage",
                    "Missed appointment moved to " + updated.appointment().getScheduledAt()
                            + ". The same QR pass remains valid."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        }
        return "redirect:/officer/csr/walk-ins?reference=" + reference.trim();
    }

    @PostMapping("/officer/csr/walk-ins/complete")
    public String complete(
            @RequestParam String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String accessRedirect = csrAccessRedirect(session);
        if (accessRedirect != null) return accessRedirect;
        try {
            walkInService.completeVisit(reference);
            redirectAttributes.addFlashAttribute("walkInMessage", "Walk-in visit completed.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("walkInError", exception.getMessage());
        }
        return "redirect:/officer/csr/walk-ins";
    }

    private String csrAccessRedirect(HttpSession session) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        Object role = session.getAttribute("loggedInOfficerRole");
        if (OfficerAccountService.CSR.equals(role) || OfficerAccountService.DIRECTOR.equals(role)) {
            return null;
        }
        return "redirect:/login";
    }

    private Integer officerId(HttpSession session) {
        Object id = session.getAttribute("loggedInOfficerId");
        if (id instanceof Integer officerId) return officerId;
        throw new IllegalArgumentException("Officer session was not found.");
    }

    private void addOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }

    private void addScheduleFilters(
            RedirectAttributes redirectAttributes,
            Integer centreId,
            LocalDate startDate
    ) {
        if (centreId != null) redirectAttributes.addAttribute("scheduleCentreId", centreId);
        if (startDate != null) {
            redirectAttributes.addAttribute("scheduleMonth", YearMonth.from(startDate).toString());
        }
    }

    private YearMonth parseScheduleMonth(String value) {
        if (value == null || value.isBlank()) return YearMonth.now();
        try {
            return YearMonth.parse(value);
        } catch (RuntimeException exception) {
            return YearMonth.now();
        }
    }
}
