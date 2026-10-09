package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingChangeRequest;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingSelectionForm;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.*;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller: the citizen "Manage booking" page, where a confirmed biometric booking is cancelled or
 * moved.
 */
@Controller
public class BiometricBookingChangeController {
    // OOP - Composition and Encapsulation: the services is a private final field handed in through the
    // constructor (dependency injection), so this class is loosely coupled and the object is never created
    // here.
    private final AppointmentService bookings;
    private final BiometricBookingChangeService changes;

    public BiometricBookingChangeController(AppointmentService bookings, BiometricBookingChangeService changes) {
        this.bookings = bookings;
        this.changes = changes;
    }

    /** Stops the browser caching this page. */
    @ModelAttribute public void noCache(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }
    @ModelAttribute("selection") public BookingSelectionForm selection() { return new BookingSelectionForm(); }

    /** READ: show the booking and issue a one-time change token. */
    @GetMapping("/citizen/appointments/{reference}/manage")
    public String manage(@PathVariable String reference, HttpSession session, Model model, RedirectAttributes redirect) {
        if (citizenId(session) == null) return "redirect:/login";
        try {
            var appointment = bookings.getCitizenAppointment(citizenId(session), reference);
            if (!"BIOMETRIC".equals(appointment.getAppointmentType())) throw new IllegalArgumentException("Booking was not found.");
            var details = bookings.getBookingDetails(citizenId(session), appointment.getApplicationId());
            boolean rebooking = "CANCELLED".equals(appointment.getAppointmentStatus());
            model.addAttribute("appointment", appointment);
            model.addAttribute("biometric", true);
            model.addAttribute("rebooking", rebooking);
            model.addAttribute("canChange", BookingChangePolicy.canCancel(appointment));
            model.addAttribute("canBook", rebooking || BookingChangePolicy.canReschedule(appointment));
            model.addAttribute("centres", details.centres());
            model.addAttribute("earliestDate", details.earliestDate());
            model.addAttribute("latestDate", details.latestDate());
            model.addAttribute("changeToken", BookingChangeRequest.prepare(appointment, session).token());
            model.addAttribute("manageUrl", "/citizen/appointments/" + reference + "/manage");
            model.addAttribute("backUrl", "/citizen/applications");
            return "citizen_frontends/booking_change";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("applicationError", exception.getMessage());
            return "redirect:/citizen/applications";
        }
    }

    /** UPDATE: cancel the booking. Its status becomes CANCELLED and the payment is kept. */
    @PostMapping("/citizen/appointments/{reference}/manage/cancel")
    public String cancel(@PathVariable String reference, @RequestParam(required = false) String token,
                         HttpSession session, RedirectAttributes redirect) {
        if (citizenId(session) == null) return "redirect:/login";
        try {
            var request = BookingChangeRequest.require(token, session);
            requireReference(reference, request, session);
            changes.cancel(citizenId(session), request);
            BookingChangeRequest.clear(request.token(), session);
            redirect.addFlashAttribute("changeMessage", "Booking cancelled. Your place is released. Choose another published slot without paying again to continue your application.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("changeError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirect.addFlashAttribute("changeError", "The cancellation could not be saved. Reload your booking and try again.");
        }
        return "redirect:/citizen/appointments/" + reference + "/manage";
    }

    /** UPDATE: move to a new slot (or rebook after a cancel) with no extra payment. */
    @PostMapping({"/citizen/appointments/{reference}/manage/change", "/citizen/appointments/{reference}/manage/pay"})
    public String change(@PathVariable String reference, @RequestParam(required = false) String token,
                      @ModelAttribute("selection") BookingSelectionForm selection,
                      HttpSession session, Model model, RedirectAttributes redirect) {
        if (citizenId(session) == null) return "redirect:/login";
        try {
            var request = BookingChangeRequest.require(token, session);
            requireReference(reference, request, session);
            changes.changeBooking(citizenId(session), request, selection.readSlot());
            BookingChangeRequest.clear(request.token(), session);
            redirect.addFlashAttribute("appointmentMessage", "Your new booking is confirmed. No additional payment was required.");
            return "redirect:/citizen/appointments/" + reference + "/pass";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("changeError", exception.getMessage());
        } catch (DataAccessException exception) {
            model.addAttribute("changeError", "The change could not be saved. No change payment was recorded. Reload and try again.");
        }
        return manage(reference, session, model, redirect);
    }

    /** Checks the token belongs to the booking in the URL. */
    private void requireReference(String reference, BookingChangeRequest request, HttpSession session) {
        if (!bookings.getCitizenAppointment(citizenId(session), reference).getAppointmentId().equals(request.appointmentId())) {
            throw new IllegalArgumentException("Reload this booking before confirming a change.");
        }
    }

    private Integer citizenId(HttpSession session) {
        return session.getAttribute("loggedInUserId") instanceof Integer id
                && "CITIZEN".equals(session.getAttribute("loggedInUserRole")) ? id : null;
    }
}
