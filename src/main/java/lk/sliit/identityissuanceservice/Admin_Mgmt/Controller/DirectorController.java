package lk.sliit.identityissuanceservice.Admin_Mgmt.Controller;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.DirectorDashboardService;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.PrintingExportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class DirectorController {

    private final PrintingExportService printingService;
    private final DirectorDashboardService dashboardService;

    public DirectorController(
            PrintingExportService printingService,
            DirectorDashboardService dashboardService
    ) {
        this.printingService = printingService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/officer/director")
    public String directorDashboard(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!OfficerAccountService.DIRECTOR.equals(session.getAttribute("loggedInOfficerRole"))) {
            return "redirect:/login";
        }

        model.addAttribute("stats", dashboardService.getStats());
        addLoggedInOfficer(model, session);
        return "officers_frontends/director_dashboard";
    }

    @GetMapping("/officer/director/printing")
    public String printingExport(HttpSession session, Model model) {
        if (session.getAttribute("loggedInAdminId") != null) {
            return "redirect:/officer/admin";
        }
        if (!OfficerAccountService.DIRECTOR.equals(session.getAttribute("loggedInOfficerRole"))) {
            return "redirect:/login";
        }

        model.addAttribute("readyApplications", printingService.getReadyApplications());
        model.addAttribute("printedApplications", printingService.getPrintedApplications());
        addLoggedInOfficer(model, session);
        return "officers_frontends/printing_export";
    }

    @PostMapping("/officer/director/printing/process")
    public String processPrintingSelection(
            @RequestParam(required = false) List<Integer> applicationIds,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isDirector(session)) return "redirect:/login";

        try {
            printingService.printAndCreateCsv(applicationIds, (Integer) session.getAttribute("loggedInOfficerId"));
            redirectAttributes.addFlashAttribute(
                    "printingMessage",
                    applicationIds.size() + " document(s) printed and added to the downloadable register."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("printingError", exception.getMessage());
        } catch (RuntimeException exception) {
            redirectAttributes.addFlashAttribute(
                    "printingError",
                    "Printing could not be completed. No records were changed. Please check the citizen details and try again."
            );
        }
        return "redirect:/officer/director/printing";
    }

    @GetMapping("/officer/director/printing/download")
    public ResponseEntity<byte[]> downloadPrintedRegister(HttpSession session) {
        if (!isDirector(session)) {
            return ResponseEntity.status(403).build();
        }

        byte[] csv = printingService.createPrintedRegisterCsv();
        String filename = "lankaid-printing-register-" + LocalDate.now() + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    private void addLoggedInOfficer(Model model, HttpSession session) {
        model.addAttribute("loggedInOfficerRoleLabel", session.getAttribute("loggedInOfficerRoleLabel"));
        model.addAttribute("loggedInOfficerEmployeeNo", session.getAttribute("loggedInOfficerEmployeeNo"));
        model.addAttribute("loggedInOfficerEmail", session.getAttribute("loggedInOfficerEmail"));
    }

    private boolean isDirector(HttpSession session) {
        return session.getAttribute("loggedInAdminId") == null
                && OfficerAccountService.DIRECTOR.equals(session.getAttribute("loggedInOfficerRole"));
    }
}
