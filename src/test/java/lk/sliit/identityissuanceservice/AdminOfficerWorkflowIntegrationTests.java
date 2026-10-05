package lk.sliit.identityissuanceservice;

import jakarta.servlet.http.HttpSession;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.AppointmentRepository;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class AdminOfficerWorkflowIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private StaffAccountRepository staffAccountRepository;

    @Autowired
    private CentreRepository centreRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    void onlyAdminCanAddAServiceCentre() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        String centreName = "Kandy Service Centre " + suffix;

        mockMvc.perform(post("/officer/admin/centres")
                        .param("centreName", centreName)
                        .param("street", "Peradeniya Road")
                        .param("city", "Kandy")
                        .param("district", "Kandy")
                        .param("dailyCapacity", "120"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInAdminId", 1);
        adminSession.setAttribute("loggedInAdminEmail", "admin@gov.lk");

        mockMvc.perform(post("/officer/admin/centres")
                        .session(adminSession)
                        .param("centreName", centreName)
                        .param("street", "Peradeniya Road")
                        .param("city", "Kandy")
                        .param("district", "Kandy")
                        .param("dailyCapacity", "120"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=centres"));

        assertTrue(centreRepository.existsByCentreNameIgnoreCaseAndCityIgnoreCase(centreName, "Kandy"));

        mockMvc.perform(get("/officer/admin")
                        .param("section", "centres")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(centreName)))
                .andExpect(content().string(containsString("booking totals are for today")));
    }

    @Test
    void adminCanManageCentreAndCannotDeleteItWithFutureAppointments() throws Exception {
        String suffix = String.valueOf(System.nanoTime());
        Centre centre = new Centre();
        centre.setCentreName("Galle Centre " + suffix);
        centre.setStreet("Main Street");
        centre.setCity("Galle");
        centre.setDistrict("Galle");
        centre.setDailyCapacity(80);
        centre = centreRepository.saveAndFlush(centre);

        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInAdminId", 1);
        adminSession.setAttribute("loggedInAdminEmail", "admin@gov.lk");

        mockMvc.perform(post("/officer/admin/centres/{centreId}/edit", centre.getCentreId())
                        .session(adminSession)
                        .param("centreName", "Galle Main Centre " + suffix)
                        .param("street", "New Main Street")
                        .param("city", "Galle")
                        .param("district", "Galle")
                        .param("dailyCapacity", "150"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("centreMessage", "Service centre updated successfully."));

        Centre updated = centreRepository.findById(centre.getCentreId()).orElseThrow();
        assertEquals("Galle Main Centre " + suffix, updated.getCentreName());
        assertEquals(150, updated.getDailyCapacity());

        mockMvc.perform(post("/officer/admin/centres/{centreId}/status", centre.getCentreId())
                        .session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("centreMessage", "Service centre deactivated successfully."));
        assertEquals("INACTIVE", centreRepository.findById(centre.getCentreId()).orElseThrow().getCentreStatus());

        Appointment appointment = new Appointment();
        appointment.setCentreId(centre.getCentreId());
        appointment.setAppointmentType("WALK_IN");
        appointment.setScheduledAt(LocalDateTime.now().plusDays(2));
        appointment.setAppointmentStatus("CONFIRMED");
        appointment.setReferenceNo("CENTRE-TEST-" + suffix);
        appointmentRepository.saveAndFlush(appointment);

        mockMvc.perform(post("/officer/admin/centres/{centreId}/delete", centre.getCentreId())
                        .session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("centreError",
                        "This centre has future appointments. Deactivate it instead of deleting it."));
        assertTrue(centreRepository.existsById(centre.getCentreId()));

        appointmentRepository.delete(appointment);
        appointmentRepository.flush();
        mockMvc.perform(post("/officer/admin/centres/{centreId}/delete", centre.getCentreId())
                        .session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("centreMessage", "Service centre deleted successfully."));
        assertFalse(centreRepository.existsById(centre.getCentreId()));
    }

    @Test
    void adminCreatesOfficerWhoAppearsAndCanLogin() throws Exception {
        String suffix = String.format("%08d", Math.abs(System.nanoTime() % 100_000_000));
        String email = "test." + suffix + "@gov.lk";
        String phone = "+947" + suffix;

        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInAdminId", 1);
        adminSession.setAttribute("loggedInAdminEmail", "admin@gov.lk");

        mockMvc.perform(post("/officer/admin/officers")
                        .session(adminSession)
                        .param("email", email)
                        .param("phone", phone)
                        .param("role", "nic-officer")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=users"));

        mockMvc.perform(get("/officer/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(email)))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMP-NIC-")));

        HttpSession officerSession = mockMvc.perform(post("/login/officer")
                        .param("role", "nic-officer")
                        .param("email", email)
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/nic/applications"))
                .andReturn()
                .getRequest()
                .getSession(false);

        assertNotNull(officerSession);
        assertEquals("NIC_OFFICER", officerSession.getAttribute("loggedInOfficerRole"));

        mockMvc.perform(get("/officer/nic/applications")
                        .session((MockHttpSession) officerSession))
                .andExpect(status().isOk());

        mockMvc.perform(get("/officer/passport/applications")
                        .session((MockHttpSession) officerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void adminCanCreateAllOfficerRolesAndDirectorCanUseAllDashboards() throws Exception {
        String suffix = String.format("%07d", Math.abs(System.nanoTime() % 10_000_000));
        String[] roles = {"nic-officer", "passport-officer", "csr", "director"};
        String[] emailPrefixes = {"nic.", "passport.", "csr.", "director."};

        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInAdminId", 1);
        adminSession.setAttribute("loggedInAdminEmail", "admin@gov.lk");

        for (int index = 0; index < roles.length; index++) {
            mockMvc.perform(post("/officer/admin/officers")
                            .session(adminSession)
                            .param("email", emailPrefixes[index] + suffix + "@gov.lk")
                            .param("phone", "+947" + suffix + index)
                            .param("role", roles[index])
                            .param("password", "Password123!"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/officer/admin?section=users"));
        }

        mockMvc.perform(get("/officer/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMP-NIC-")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMP-PASS-")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMP-CSR-")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("EMP-DIR-")));

        HttpSession directorSession = mockMvc.perform(post("/login/officer")
                        .param("role", "csr")
                        .param("email", "director." + suffix + "@gov.lk")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/csr"))
                .andReturn()
                .getRequest()
                .getSession(false);

        assertNotNull(directorSession);
        assertEquals("DIRECTOR", directorSession.getAttribute("loggedInOfficerRole"));

        String[] directorRoutes = {
                "/officer/director",
                "/officer/nic/applications",
                "/officer/passport/applications",
                "/officer/csr"
        };
        for (String route : directorRoutes) {
            mockMvc.perform(get(route).session((MockHttpSession) directorSession))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void adminCanEditDisableReactivateAndDeleteOfficer() throws Exception {
        String suffix = String.format("%08d", Math.abs(System.nanoTime() % 100_000_000));
        String originalEmail = "actions." + suffix + "@gov.lk";
        String updatedEmail = "edited." + suffix + "@gov.lk";
        String phone = "+947" + suffix;
        String updatedPhone = "+947" + new StringBuilder(suffix).reverse();

        MockHttpSession adminSession = new MockHttpSession();
        adminSession.setAttribute("loggedInAdminId", 1);
        adminSession.setAttribute("loggedInAdminEmail", "admin@gov.lk");

        mockMvc.perform(post("/officer/admin/officers")
                        .session(adminSession)
                        .param("email", originalEmail)
                        .param("phone", phone)
                        .param("role", "nic-officer")
                        .param("password", "Password123!"))
                .andExpect(status().is3xxRedirection());

        UserAccount user = userAccountRepository.findByEmailIgnoreCase(originalEmail).orElseThrow();

        mockMvc.perform(post("/officer/admin/officers/{userId}/edit", user.getUserId())
                        .session(adminSession)
                        .param("email", updatedEmail)
                        .param("phone", updatedPhone)
                        .param("role", "passport-officer")
                        .param("newPassword", "Changed123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/admin?section=users"));

        mockMvc.perform(get("/officer/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(updatedEmail)))
                .andExpect(content().string(containsString("EMP-PASS-")));

        mockMvc.perform(post("/officer/admin/officers/{userId}/status", user.getUserId())
                        .session(adminSession))
                .andExpect(status().is3xxRedirection());

        UserAccount disabledUser = userAccountRepository.findById(user.getUserId()).orElseThrow();
        StaffAccount disabledStaff = staffAccountRepository.findById(user.getUserId()).orElseThrow();
        assertEquals("SUSPENDED", disabledUser.getAccountStatus());
        assertEquals("SUSPENDED", disabledStaff.getStaffStatus());

        mockMvc.perform(post("/login/officer")
                        .param("role", "passport-officer")
                        .param("email", updatedEmail)
                        .param("password", "Changed123!"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Invalid officer email, password or selected role.")));

        mockMvc.perform(post("/officer/admin/officers/{userId}/status", user.getUserId())
                        .session(adminSession))
                .andExpect(status().is3xxRedirection());

        HttpSession officerSession = mockMvc.perform(post("/login/officer")
                        .param("role", "passport-officer")
                        .param("email", updatedEmail)
                        .param("password", "Changed123!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/officer/passport/applications"))
                .andReturn()
                .getRequest()
                .getSession(false);
        assertNotNull(officerSession);

        mockMvc.perform(post("/officer/admin/officers/{userId}/delete", user.getUserId())
                        .session(adminSession))
                .andExpect(status().is3xxRedirection());

        UserAccount removedUser = userAccountRepository.findById(user.getUserId()).orElseThrow();
        StaffAccount removedStaff = staffAccountRepository.findById(user.getUserId()).orElseThrow();
        assertEquals("REMOVED", removedUser.getAccountStatus());
        assertEquals("REMOVED", removedStaff.getStaffStatus());

        mockMvc.perform(get("/officer/admin").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(updatedEmail))));

        assertTrue(userAccountRepository.findByEmailIgnoreCaseAndAccountStatus(updatedEmail, "ACTIVE").isEmpty());
    }
}
