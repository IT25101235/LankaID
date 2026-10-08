package lk.sliit.identityissuanceservice.Notification_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.AdminNotificationRequest;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.EmailDispatchResult;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Dto.PublicAnnouncementRequest;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.AdminNotificationService;
import lk.sliit.identityissuanceservice.Notification_Mgmt.Service.PublicAnnouncementService;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminNotificationController {

    private static final String REDIRECT = "redirect:/officer/admin?section=notifications";
    private final AdminNotificationService notificationService;
    private final PublicAnnouncementService publicAnnouncementService;

    public AdminNotificationController(
            AdminNotificationService notificationService,
            PublicAnnouncementService publicAnnouncementService
    ) {
        this.notificationService = notificationService;
        this.publicAnnouncementService = publicAnnouncementService;
    }

    @PostMapping("/officer/admin/notifications/public")
    public String publishPublicAnnouncement(
            @ModelAttribute PublicAnnouncementRequest request,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer administratorId = administratorId(session);
        if (administratorId == null) return "redirect:/login";
        try {
            publicAnnouncementService.publish(request, administratorId);
            redirectAttributes.addFlashAttribute(
                    "publicAnnouncementMessage",
                    "The public announcement is now scheduled for the landing page."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("publicAnnouncementError", exception.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/officer/admin/notifications/public/{announcementId}/unpublish")
    public String unpublishPublicAnnouncement(
            @PathVariable Integer announcementId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (administratorId(session) == null) return "redirect:/login";
        try {
            publicAnnouncementService.unpublish(announcementId);
            redirectAttributes.addFlashAttribute(
                    "publicAnnouncementMessage",
                    "The public announcement was removed from the landing page."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("publicAnnouncementError", exception.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/officer/admin/notifications/in-app")
    public String sendInApp(
            @Valid @ModelAttribute AdminNotificationRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        Integer administratorId = administratorId(session);
        if (administratorId == null) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("notificationError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return REDIRECT;
        }
        try {
            int delivered = notificationService.sendInApp(request, administratorId);
            redirectAttributes.addFlashAttribute("notificationMessage", "In-app notification delivered to " + delivered + " recipient(s).");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("notificationError", exception.getMessage());
        }
        return REDIRECT;
    }

    @PostMapping("/officer/admin/notifications/email")
    public String sendEmail(
            @Valid @ModelAttribute AdminNotificationRequest request,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (administratorId(session) == null) return "redirect:/login";
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("emailNotificationError", bindingResult.getAllErrors().get(0).getDefaultMessage());
            return REDIRECT;
        }
        try {
            EmailDispatchResult result = notificationService.sendEmail(request);
            String summary = "Official email sent to " + result.sentCount() + " recipient(s).";
            if (result.failedCount() > 0) summary += " " + result.failedCount() + " delivery attempt(s) failed.";
            redirectAttributes.addFlashAttribute("emailNotificationMessage", summary);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("emailNotificationError", exception.getMessage());
        }
        return REDIRECT;
    }

    private Integer administratorId(HttpSession session) {
        Object userId = session.getAttribute("loggedInAdminId");
        return userId instanceof Integer id ? id : null;
    }
}
