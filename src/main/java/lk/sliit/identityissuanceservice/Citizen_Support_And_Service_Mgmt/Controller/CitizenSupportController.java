package lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.SupportRequestForm;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.SupportRequestView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInAppointmentView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.CitizenSupportService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInAppointmentService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Controller
public class CitizenSupportController {

    private final CitizenSupportService supportService;
    private final WalkInAppointmentService walkInService;

    public CitizenSupportController(
            CitizenSupportService supportService,
            WalkInAppointmentService walkInService
    ) {
        this.supportService = supportService;
        this.walkInService = walkInService;
    }

    @GetMapping({
            "/citizen/support/new",
            "/citizen/support/contact-us",
            "/citizen/support/complaint"
    })
    public String newSupportRequest(
            @RequestParam(required = false) String type,
            HttpServletRequest request,
            HttpSession session,
            Model model
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        SupportRequestForm form = new SupportRequestForm();
        String route = currentRouteType(type, request.getRequestURI());
        form.setRequestType(route);
        model.addAttribute("supportForm", form);
        addFormLabels(model, route);
        return "citizen_frontends/citizen_complaints";
    }

    @PostMapping("/citizen/support")
    public String submitSupportRequest(
            @Valid @ModelAttribute("supportForm") SupportRequestForm form,
            BindingResult bindingResult,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        if (bindingResult.hasErrors()) {
            addFormLabels(model, form.getRequestType());
            return "citizen_frontends/citizen_complaints";
        }

        try {
            SupportRequestView created = supportService.createRequest(citizenId, form);
            redirectAttributes.addFlashAttribute(
                    "supportMessage",
                    created.reference() + " was submitted successfully."
            );
            return "redirect:/citizen/support/" + created.reference();
        } catch (IllegalArgumentException exception) {
            bindingResult.reject("support.request.failed", exception.getMessage());
            addFormLabels(model, form.getRequestType());
            return "citizen_frontends/citizen_complaints";
        }
    }

    @GetMapping("/citizen/support")
    public String citizenSupportRequests(
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        Page<SupportRequestView> requestPage = supportService.getCitizenRequests(citizenId, page);
        addPageData(model, requestPage);
        model.addAttribute("openCount", supportService.getCitizenOpenCount(citizenId));
        model.addAttribute("respondedCount", supportService.getCitizenRespondedCount(citizenId));
        model.addAttribute("resolvedCount", supportService.getCitizenResolvedCount(citizenId));
        return "citizen_frontends/citizen_support_requests";
    }

    @GetMapping("/citizen/support/{reference}")
    public String citizenSupportDetail(
            @PathVariable String reference,
            Model model,
            HttpSession session
    ) {
        Integer citizenId = loggedInCitizenId(session);
        if (citizenId == null) return "redirect:/login";

        try {
            model.addAttribute("supportRequest", supportService.getCitizenRequest(citizenId, reference));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("supportError", exception.getMessage());
        }
        return "citizen_frontends/citizen_support_detail";
    }

    @GetMapping("/officer/csr")
    public String csrDashboard(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccessCsrPages(session)) return "redirect:/login";

        model.addAttribute("openSupportCount", supportService.getOpenCount());
        model.addAttribute("respondedSupportCount", supportService.getRespondedCount());
        model.addAttribute("resolvedSupportCount", supportService.getResolvedCount());
        model.addAttribute("averageResponseTime", supportService.getAverageResponseTimeLabel());
        model.addAttribute("recentSupportRequests", supportService.getRecentRequests());

        LocalDate today = LocalDate.now();
        List<WalkInAppointmentView> todayWalkIns = walkInService.getAppointmentsForDate(today);
        model.addAttribute("todayWalkInCount", todayWalkIns.size());
        model.addAttribute(
                "todayCompletedWalkInCount",
                todayWalkIns.stream()
                        .filter(item -> "COMPLETED".equals(item.appointment().getAppointmentStatus()))
                        .count()
        );
        model.addAttribute("todayWalkIns", todayWalkIns.stream().limit(5).toList());
        model.addAttribute("hasMoreTodayWalkIns", todayWalkIns.size() > 5);
        model.addAttribute(
                "dashboardDate",
                today.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy", Locale.ENGLISH))
        );
        addLoggedInOfficer(model, session);
        return "officers_frontends/csr_dashboard";
    }

    @GetMapping("/officer/csr/support")
    public String csrSupportQueue(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccessCsrPages(session)) return "redirect:/login";

        Page<SupportRequestView> requestPage =
                supportService.getCsrRequests(type, status, page);
        addPageData(model, requestPage);
        model.addAttribute("selectedType", type == null ? "" : type);
        model.addAttribute("selectedStatus", status == null ? "" : status);
        model.addAttribute("openCount", supportService.getOpenCount());
        model.addAttribute("respondedCount", supportService.getRespondedCount());
        model.addAttribute("resolvedCount", supportService.getResolvedCount());
        addLoggedInOfficer(model, session);
        return "officers_frontends/csr_support_requests";
    }

    @GetMapping({
            "/officer/csr/support/{reference}",
            "/officer/csr/complaints/{reference}"
    })
    public String supportRequestDetail(
            @PathVariable String reference,
            Model model,
            HttpSession session
    ) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccessCsrPages(session)) return "redirect:/login";

        try {
            model.addAttribute("supportRequest", supportService.getCsrRequest(reference));
        } catch (IllegalArgumentException exception) {
            model.addAttribute("supportError", exception.getMessage());
        }
        addLoggedInOfficer(model, session);
        return "officers_frontends/csr_complaints_detail";
    }

    @PostMapping("/officer/csr/support/{reference}/respond")
    public String respondToCitizen(
            @PathVariable String reference,
            @RequestParam String response,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccessCsrPages(session)) return "redirect:/login";

        try {
            supportService.respond(reference, loggedInOfficerId(session), response);
            redirectAttributes.addFlashAttribute("supportMessage", "Response sent to the citizen.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("supportError", exception.getMessage());
        }
        return "redirect:/officer/csr/support/" + reference;
    }

    @PostMapping("/officer/csr/support/{reference}/resolve")
    public String resolveRequest(
            @PathVariable String reference,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (session.getAttribute("loggedInAdminId") != null) return "redirect:/officer/admin";
        if (!canAccessCsrPages(session)) return "redirect:/login";

        try {
            supportService.resolve(reference, loggedInOfficerId(session));
            redirectAttributes.addFlashAttribute("supportMessage", "Support request marked as resolved.");
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("supportError", exception.getMessage());
        }
        return "redirect:/officer/csr/support/" + reference;
    }

    private void addPageData(Model model, Page<SupportRequestView> requestPage) {
        model.addAttribute("supportRequests", requestPage.getContent());
        model.addAttribute("currentPage", requestPage.getNumber());
        model.addAttribute("totalPages", requestPage.getTotalPages());
        model.addAttribute("totalRequests", requestPage.getTotalElements());
        model.addAttribute("hasPrevious", requestPage.hasPrevious());
        model.addAttribute("hasNext", requestPage.hasNext());
    }

    private void addFormLabels(Model model, String requestType) {
        boolean complaint = CitizenSupportService.COMPLAINT.equals(requestType);
        model.addAttribute("formTitle", complaint ? "Submit a complaint" : "Contact Us");
        model.addAttribute(
                "formDescription",
                complaint
                        ? "Report a problem with an application or LankaID service."
                        : "Ask the Citizen Service team a question."
        );
    }

    private String currentRouteType(String type, String requestUri) {
        if (CitizenSupportService.COMPLAINT.equalsIgnoreCase(type)
                || requestUri.endsWith("/complaint")) {
            return CitizenSupportService.COMPLAINT;
        }
        return CitizenSupportService.CONTACT;
    }

    private boolean canAccessCsrPages(HttpSession session) {
        Object role = session.getAttribute("loggedInOfficerRole");
        return OfficerAccountService.CSR.equals(role)
                || OfficerAccountService.DIRECTOR.equals(role);
    }

    private void addLoggedInOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }

    private Integer loggedInCitizenId(HttpSession session) {
        Object userId = session.getAttribute("loggedInUserId");
        Object role = session.getAttribute("loggedInUserRole");
        return userId instanceof Integer id && "CITIZEN".equals(role) ? id : null;
    }

    private Integer loggedInOfficerId(HttpSession session) {
        Object officerId = session.getAttribute("loggedInOfficerId");
        if (officerId instanceof Integer id) return id;
        throw new IllegalArgumentException("Officer session was not found. Please sign in again.");
    }
}
