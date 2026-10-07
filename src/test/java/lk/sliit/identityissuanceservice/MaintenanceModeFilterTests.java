package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.MaintenanceModeFilter;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.SystemMaintenanceService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaintenanceModeFilterTests {

    @Test
    void maintenanceModeBlocksOperationsButKeepsAdminPortalAvailable() throws Exception {
        SystemMaintenanceService service = mock(SystemMaintenanceService.class);
        when(service.isMaintenanceActive()).thenReturn(true);
        MaintenanceModeFilter filter = new MaintenanceModeFilter(service);

        MockHttpServletRequest walkInRequest = new MockHttpServletRequest("GET", "/officer/csr/walk-ins");
        MockHttpServletResponse walkInResponse = new MockHttpServletResponse();
        MockFilterChain blockedChain = new MockFilterChain();
        filter.doFilter(walkInRequest, walkInResponse, blockedChain);

        assertEquals("/?maintenance=true", walkInResponse.getRedirectedUrl());
        assertNull(blockedChain.getRequest());

        MockHttpServletRequest adminRequest = new MockHttpServletRequest("GET", "/officer/admin");
        MockHttpServletResponse adminResponse = new MockHttpServletResponse();
        MockFilterChain allowedChain = new MockFilterChain();
        filter.doFilter(adminRequest, adminResponse, allowedChain);

        assertNotNull(allowedChain.getRequest());
    }
}
