package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BiometricScheduleService;
import lk.sliit.identityissuanceservice.User_Mgmt.Controller.DeletionFormToken;
import org.springframework.dao.DataAccessException;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

/** Controller: officers manage the biometric calendar (publish, close, edit and delete slots). */
@Controller
public class BiometricScheduleController {

    // OOP - Composition and Encapsulation: the service is a private final field handed in through the
    // constructor (dependency injection), so this class is loosely coupled and the object is never created
    // here.
    private final BiometricScheduleService schedules;

    public BiometricScheduleController(BiometricScheduleService schedules) {
        this.schedules = schedules;
    }

    /** READ: monthly calendar of slots for one centre. */
    @GetMapping("/officer/{service}/appointments/schedule")
    public String schedule(
            @PathVariable String service,
            @RequestParam(required = false) Integer centreId,
            @RequestParam(required = false) String month,
            HttpSession session,
            Model model
    ) {
        String serviceType = serviceType(service);
        if (!canAccess(session, serviceType)) return "redirect:/login";
        YearMonth selectedMonth = readMonth(month);
        var centres = schedules.getCentres();
        Integer selectedCentre = centreId;
        if (selectedCentre == null && !centres.isEmpty()) selectedCentre = centres.get(0).getCentreId();

        model.addAttribute("servicePath", serviceType.toLowerCase());
        model.addAttribute("serviceType", serviceType);
        model.addAttribute("centres", centres);
        model.addAttribute("selectedCentreId", selectedCentre);
        model.addAttribute("selectedMonth", selectedMonth.toString());
        model.addAttribute("minimumDate", LocalDate.now().plusDays(1));
        try {
            model.addAttribute("schedule", selectedCentre == null ? java.util.List.of() : schedules.getSchedule(
                    serviceType, selectedCentre, selectedMonth.atDay(1), selectedMonth.atEndOfMonth()));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("schedule", java.util.List.of());
            model.addAttribute("scheduleError", exception.getMessage());
        }
        return "officers_frontends/biometric_schedule";
    }

    /** CREATE: publish half-hour slots for a date range. */
    @PostMapping("/officer/{service}/appointments/schedule/publish")
    public String publish(
            @PathVariable String service,
            @RequestParam Integer centreId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime openingTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime closingTime,
            @RequestParam Integer capacity,
            @RequestParam(required = false) String formToken,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        String serviceType = serviceType(service);
        if (!canAccess(session, serviceType)) return "redirect:/login";
        try {
            BiometricFormToken.check(formToken, session);
            int days = schedules.publishDateRange(serviceType, centreId, startDate, endDate,
                    openingTime, closingTime, capacity, officerId(session));
            redirect.addFlashAttribute("scheduleMessage", days + " day(s) published for " + serviceType + " biometrics.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("scheduleError", exception.getMessage());
        }
        return redirect(service, centreId, startDate, redirect);
    }

    /** UPDATE: close a date range. Confirmed bookings are cancelled and citizens notified. */
    @PostMapping("/officer/{service}/appointments/schedule/close")
    public String close(
            @PathVariable String service,
            @RequestParam Integer centreId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String formToken,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        String serviceType = serviceType(service);
        if (!canAccess(session, serviceType)) return "redirect:/login";
        try {
            BiometricFormToken.check(formToken, session);
            int days = schedules.closeDateRange(serviceType, centreId, startDate, endDate);
            redirect.addFlashAttribute("scheduleMessage", days
                    + " day(s) closed. Confirmed citizens were notified and can rebook without paying again.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("scheduleError", exception.getMessage());
        }
        return redirect(service, centreId, startDate, redirect);
    }

    /** UPDATE: change one slot's capacity, or open/close it. */
    @PostMapping("/officer/{service}/appointments/schedule/slot")
    public String updateSlot(
            @PathVariable String service,
            @RequestParam Integer slotId,
            @RequestParam Integer capacity,
            @RequestParam(defaultValue = "false") boolean available,
            @RequestParam Integer centreId,
            @RequestParam String month,
            @RequestParam(required = false) String formToken,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        String serviceType = serviceType(service);
        if (!canAccess(session, serviceType)) return "redirect:/login";
        try {
            BiometricFormToken.check(formToken, session);
            schedules.updateSlot(serviceType, slotId, capacity, available);
            redirect.addFlashAttribute("scheduleMessage", "Slot capacity and availability updated.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("scheduleError", exception.getMessage());
        }
        redirect.addAttribute("centreId", centreId);
        redirect.addAttribute("month", readMonth(month));
        return "redirect:/officer/" + service + "/appointments/schedule";
    }

    /** DELETE: remove a future slot that has no bookings (needs the delete token too). */
    @PostMapping("/officer/{service}/appointments/schedule/slot/{slotId}/delete")
    public String deleteSlot(
            @PathVariable String service,
            @PathVariable Integer slotId,
            @RequestParam String deleteToken,
            @RequestParam Integer centreId,
            @RequestParam String month,
            HttpSession session,
            RedirectAttributes redirect
    ) {
        String serviceType = serviceType(service);
        if (!canAccess(session, serviceType)) return "redirect:/login";
        try {
            DeletionFormToken.check(deleteToken, session);
            schedules.deleteUnusedSlot(serviceType, slotId);
            redirect.addFlashAttribute("scheduleMessage", "Unused slot deleted.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("scheduleError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirect.addFlashAttribute("scheduleError", "The slot could not be deleted. Reload and try again.");
        }
        redirect.addAttribute("centreId", centreId);
        redirect.addAttribute("month", readMonth(month));
        return "redirect:/officer/" + service + "/appointments/schedule";
    }

    private String redirect(String service, Integer centreId, LocalDate date, RedirectAttributes redirect) {
        redirect.addAttribute("centreId", centreId);
        redirect.addAttribute("month", YearMonth.from(date));
        return "redirect:/officer/" + service + "/appointments/schedule";
    }

    /** Role check: the director or the officer for this service. */
    private boolean canAccess(HttpSession session, String serviceType) {
        Object role = session.getAttribute("loggedInOfficerRole");
        return OfficerAccountService.DIRECTOR.equals(role)
                || ("NIC".equals(serviceType) && OfficerAccountService.NIC_OFFICER.equals(role))
                || ("PASSPORT".equals(serviceType) && OfficerAccountService.PASSPORT_OFFICER.equals(role));
    }

    private Integer officerId(HttpSession session) {
        return session.getAttribute("loggedInOfficerId") instanceof Integer id ? id : null;
    }

    private String serviceType(String service) {
        if ("nic".equalsIgnoreCase(service)) return "NIC";
        if ("passport".equalsIgnoreCase(service)) return "PASSPORT";
        throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Unknown biometric service.");
    }

    /** Falls back to the current month if the value is invalid. */
    private YearMonth readMonth(String value) {
        try {
            return value == null || value.isBlank() ? YearMonth.now() : YearMonth.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            return YearMonth.now();
        }
    }
}
