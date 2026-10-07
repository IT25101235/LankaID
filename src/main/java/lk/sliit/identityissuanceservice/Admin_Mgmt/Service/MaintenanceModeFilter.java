package lk.sliit.identityissuanceservice.Admin_Mgmt.Service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class MaintenanceModeFilter extends OncePerRequestFilter {

    private final SystemMaintenanceService maintenanceService;

    public MaintenanceModeFilter(SystemMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (isAllowedDuringMaintenance(path) || !maintenanceService.isMaintenanceActive()) {
            filterChain.doFilter(request, response);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/?maintenance=true");
    }

    private boolean isAllowedDuringMaintenance(String path) {
        return path.equals("/")
                || path.equals("/login")
                || path.equals("/login/officer")
                || path.equals("/logout")
                || path.equals("/error")
                || path.equals("/favicon.ico")
                || path.equals("/officer/admin")
                || path.startsWith("/officer/admin/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/Chatbot/");
    }
}
