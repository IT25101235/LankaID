package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingChangeRequest;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto.BookingSelectionForm;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Service.BookingChangePolicy;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInAppointmentService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInBookingChangeService;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Dto.CardPaymentForm;
import lk.sliit.identityissuanceservice.Payment_Mgmt.Service.CardPaymentValidation;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WalkInBookingChangeController {
    private final WalkInAppointmentService bookings;
    private final WalkInBookingChangeService changes;

    public WalkInBookingChangeController(WalkInAppointmentService bookings, WalkInBookingChangeService changes) {
        this.bookings = bookings;
        this.changes = changes;
    }
    @ModelAttribute("cardForm") public CardPaymentForm cardForm() { return new CardPaymentForm(); }
    @ModelAttribute public void noCache(jakarta.servlet.http.HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
    }
    @ModelAttribute("selection") public BookingSelectionForm selection() { return new BookingSelectionForm(); }

    @GetMapping("/walk-in/bookings/{reference}/manage")
    public String manage(@PathVariable String reference, HttpSession session, Model model, RedirectAttributes redirect) {
        if (!bookings.isRecoveryVerified(session)) return "redirect:/walk-in/recover";
        try {
            var appointment = bookings.getVisitorPass(reference, session).appointment();
            // The change service rechecks ownership against the verified recovery number on every POST.
            if (!lk.sliit.identityissuanceservice.OTP_Mgmt.Otp.SriLankanMobileNumber.normalize(appointment.getVisitorPhone())
                    .equals(bookings.recoveryPhone(session))) throw new IllegalArgumentException("Booking was not found.");
            model.addAttribute("appointment", appointment);
            model.addAttribute("biometric", false);
            model.addAttribute("rebooking", false);
            model.addAttribute("canChange", BookingChangePolicy.canCancel(appointment));
            model.addAttribute("canPay", BookingChangePolicy.canReschedule(appointment));
            model.addAttribute("slots", bookings.getBookableSlots());
            model.addAttribute("changeToken", BookingChangeRequest.prepare(appointment, session).token());
            model.addAttribute("manageUrl", "/walk-in/bookings/" + reference + "/manage");
            model.addAttribute("backUrl", "/walk-in/recover");
            return "citizen_frontends/booking_change";
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("walkInError", exception.getMessage());
            return "redirect:/walk-in/recover";
        }
    }

    @PostMapping("/walk-in/bookings/{reference}/manage/cancel")
    public String cancel(@PathVariable String reference, @RequestParam(required = false) String token,
                         HttpSession session, RedirectAttributes redirect) {
        try {
            changes.cancel(reference, BookingChangeRequest.require(token, session), session);
            BookingChangeRequest.clear(session);
            redirect.addFlashAttribute("walkInMessage", "Booking cancelled and its place released. No cancellation fee was charged.");
        } catch (IllegalArgumentException exception) {
            redirect.addFlashAttribute("walkInError", exception.getMessage());
        } catch (DataAccessException exception) {
            redirect.addFlashAttribute("walkInError", "The cancellation could not be saved. Reload your booking and try again.");
        }
        return "redirect:/walk-in/recover";
    }

    @PostMapping("/walk-in/bookings/{reference}/manage/pay")
    public String pay(@PathVariable String reference, @RequestParam(required = false) String token,
                      @ModelAttribute("selection") BookingSelectionForm selection,
                      @ModelAttribute("cardForm") CardPaymentForm card, HttpSession session,
                      Model model, RedirectAttributes redirect) {
        if (!bookings.isRecoveryVerified(session)) return "redirect:/walk-in/recover";
        try {
            var request = BookingChangeRequest.require(token, session);
            var errors = CardPaymentValidation.errors(card);
            if (!errors.isEmpty()) {
                model.addAttribute("cardErrors", errors);
                throw new IllegalArgumentException("Correct the highlighted card details. Your booking has not changed.");
            }
            changes.payAndReschedule(reference, request, selection.readSlot(), session);
            BookingChangeRequest.clear(session);
            redirect.addFlashAttribute("walkInMessage", "LKR 300 payment recorded. Your booking was rescheduled; open the updated pass.");
            return "redirect:/walk-in/recover";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("changeError", exception.getMessage());
        } catch (DataAccessException exception) {
            model.addAttribute("changeError", "The change could not be saved. No change payment was recorded. Reload and try again.");
        }
        return manage(reference, session, model, redirect);
    }
}
