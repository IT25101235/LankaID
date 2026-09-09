package lk.sliit.identityissuanceservice;

import lk.sliit.identityissuanceservice.Admin_Mgmt.Model.StaffAccount;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Repository.StaffAccountRepository;
import lk.sliit.identityissuanceservice.Admin_Mgmt.Service.OfficerAccountService;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Appointment;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.Centre;
import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository.CentreRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInAppointmentView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Dto.WalkInSlotView;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Repository.WalkInAppointmentRepository;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInAppointmentService;
import lk.sliit.identityissuanceservice.Citizen_Support_And_Service_Mgmt.Service.WalkInPassService;
import lk.sliit.identityissuanceservice.User_Mgmt.Model.UserAccount;
import lk.sliit.identityissuanceservice.User_Mgmt.Repository.UserAccountRepository;
import lk.sliit.identityissuanceservice.Shared.Sms.SmsSender;
import lk.sliit.identityissuanceservice.Shared.Otp.SriLankanMobileNumber;
import org.mockito.ArgumentCaptor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Rollback
class WalkInAppointmentWorkflowIntegrationTests {

    @Autowired MockMvc mockMvc;
    @Autowired WalkInAppointmentService walkInService;
    @Autowired WalkInPassService passService;
    @Autowired WalkInAppointmentRepository appointmentRepository;
    @Autowired CentreRepository centreRepository;
    @Autowired UserAccountRepository userRepository;
    @Autowired StaffAccountRepository staffRepository;
    @MockitoBean SmsSender smsSender;

    @Test
    void anonymousVisitorVerifiesBooksRecoversAndCsrChecksIn() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Centre centre = createCentre(unique);
        LocalDate bookingDate = LocalDate.now().plusDays(150);
        walkInService.publishDay(centre.getCentreId(), bookingDate, 1);
        WalkInSlotView slot = walkInService.getBookableSlots().stream()
                .filter(item -> item.centreId().equals(centre.getCentreId()))
                .filter(item -> item.slotDate().equals(bookingDate))
                .findFirst()
                .orElseThrow();

        String phone = "077" + String.format("%07d", Math.abs(unique.hashCode()) % 10_000_000);
        MockHttpSession visitorSession = new MockHttpSession();
        mockMvc.perform(post("/walk-in/otp/send")
                        .session(visitorSession)
                        .param("visitorName", "Anonymous Visitor")
                        .param("visitorPhone", phone))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/walk-in/verify"));

        mockMvc.perform(get("/walk-in/verify").session(visitorSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("111111"))))
                .andExpect(content().string(not(containsString("simulation"))));

        mockMvc.perform(post("/walk-in/otp/verify")
                        .session(visitorSession)
                        .param("otp", lastSentCode()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/walk-in/book"));

        mockMvc.perform(post("/walk-in/book")
                        .session(visitorSession)
                        .param("visitorService", "NIC_APPLICATION_HELP")
                        .param("slotId", slot.slotId().toString()))
                .andExpect(status().is3xxRedirection());

        Appointment appointment = appointmentRepository
                .findAllByVisitorPhoneAndAppointmentTypeOrderByScheduledAtDesc(
                        SriLankanMobileNumber.normalize(phone), "WALK_IN")
                .get(0);
        assertNull(appointment.getCitizenId());
        assertNull(appointment.getApplicationId());
        assertEquals("Anonymous Visitor", appointment.getVisitorName());
        assertEquals("NIC_APPLICATION_HELP", appointment.getVisitorService());
        assertTrue(appointment.getReferenceNo().startsWith("WALK-"));
        assertTrue(passService.createQrCode(appointment.getReferenceNo(), 200).length > 100);
        assertTrue(passService.createPass(walkInService.findByReference(appointment.getReferenceNo())).length > 100);

        mockMvc.perform(get("/walk-in/pass/{reference}", appointment.getReferenceNo()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(appointment.getReferenceNo())));
        mockMvc.perform(get("/walk-in/pass/{reference}/qr", appointment.getReferenceNo()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));

        mockMvc.perform(get("/walk-in/recover").param("phone", phone))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(appointment.getReferenceNo())));

        Integer csrId = createCsr(unique);
        MockHttpSession csrSession = csrSession(csrId);
        mockMvc.perform(post("/officer/csr/walk-ins/check-in")
                        .session(csrSession)
                        .param("reference", appointment.getReferenceNo())
                        .param("method", "QR"))
                .andExpect(status().is3xxRedirection());
        assertEquals(
                "CHECKED_IN",
                appointmentRepository.findById(appointment.getAppointmentId()).orElseThrow().getAppointmentStatus()
        );

        mockMvc.perform(post("/officer/csr/walk-ins/complete")
                        .session(csrSession)
                        .param("reference", appointment.getReferenceNo()))
                .andExpect(status().is3xxRedirection());
        assertEquals(
                "COMPLETED",
                appointmentRepository.findById(appointment.getAppointmentId()).orElseThrow().getAppointmentStatus()
        );
    }

    @Test
    void capacityAndClosedDaysAreEnforced() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Centre centre = createCentre(unique);
        LocalDate date = LocalDate.now().plusDays(180);
        walkInService.publishDay(centre.getCentreId(), date, 1);
        WalkInSlotView slot = walkInService.getBookableSlots().stream()
                .filter(item -> item.centreId().equals(centre.getCentreId()) && item.slotDate().equals(date))
                .findFirst().orElseThrow();

        MockHttpSession first = verifiedSession("First Visitor", "0771111111");
        WalkInAppointmentView booked = walkInService.book("NIC_APPLICATION_HELP", slot.slotId(), first);

        MockHttpSession second = verifiedSession("Second Visitor", "0772222222");
        assertThrows(
                IllegalArgumentException.class,
                () -> walkInService.book("PASSPORT_APPLICATION_HELP", slot.slotId(), second)
        );
        MockHttpSession invalidService = verifiedSession("Third Visitor", "0773333333");
        assertThrows(
                IllegalArgumentException.class,
                () -> walkInService.book("GENERAL_INQUIRY", slot.slotId(), invalidService)
        );

        walkInService.closeDay(centre.getCentreId(), date);
        assertEquals(
                "CANCELLED",
                appointmentRepository.findById(booked.appointment().getAppointmentId())
                        .orElseThrow().getAppointmentStatus()
        );
        assertFalse(walkInService.getBookableSlots().stream()
                .anyMatch(item -> item.centreId().equals(centre.getCentreId()) && item.slotDate().equals(date)));
    }

    @Test
    void csrPublishesFiltersAndClosesCustomDateRanges() {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Centre centre = createCentre(unique);
        LocalDate startDate = LocalDate.now().plusDays(210);
        LocalDate endDate = startDate.plusDays(1);

        int publishedDays = walkInService.publishDateRange(
                centre.getCentreId(),
                startDate,
                endDate,
                LocalTime.of(9, 30),
                LocalTime.of(11, 0),
                4
        );

        assertEquals(2, publishedDays);
        assertEquals(
                6,
                walkInService.getPublishedSchedule(
                        centre.getCentreId(), startDate, endDate, null
                ).size()
        );
        assertEquals(
                2,
                walkInService.getPublishedSchedule(
                        centre.getCentreId(), startDate, endDate, LocalTime.of(10, 0)
                ).size()
        );

        assertEquals(2, walkInService.closeDateRange(centre.getCentreId(), startDate, endDate));
        assertTrue(walkInService.getPublishedSchedule(
                        centre.getCentreId(), startDate, endDate, null
                ).stream()
                .noneMatch(WalkInSlotView::available));
    }

    @Test
    void csrPublishAndCloseFormsRedirectWithoutServerErrors() throws Exception {
        String unique = UUID.randomUUID().toString().substring(0, 8);
        Centre centre = createCentre(unique);
        LocalDate startDate = LocalDate.now().plusDays(240);
        LocalDate endDate = startDate.plusDays(2);
        MockHttpSession session = csrSession(createCsr(unique));

        MvcResult publishResult = mockMvc.perform(post("/officer/csr/walk-ins/schedule/day")
                        .session(session)
                        .param("centreId", centre.getCentreId().toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString())
                        .param("openingTime", "08:30")
                        .param("closingTime", "12:00")
                        .param("capacity", "6"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        mockMvc.perform(get(publishResult.getResponse().getRedirectedUrl()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"walkInCalendar\"")))
                .andExpect(content().string(containsString("data-month=\"" +
                        java.time.YearMonth.from(startDate) + "\"")));

        MvcResult closeResult = mockMvc.perform(post("/officer/csr/walk-ins/schedule/day/close")
                        .session(session)
                        .param("centreId", centre.getCentreId().toString())
                        .param("startDate", startDate.toString())
                        .param("endDate", endDate.toString()))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        mockMvc.perform(get(closeResult.getResponse().getRedirectedUrl()).session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/officer/csr/walk-ins")
                        .session(session)
                        .param("scheduleMonth", "invalid-month"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"walkInCalendar\"")));
    }

    private MockHttpSession verifiedSession(String name, String phone) {
        MockHttpSession session = new MockHttpSession();
        walkInService.startPhoneVerification(name, phone, session);
        walkInService.verifyPhone(lastSentCode(), session);
        return session;
    }

    private String lastSentCode() {
        ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(smsSender, atLeastOnce()).send(anyString(), message.capture(), anyInt());
        return message.getValue().substring("LankaID (OTP) - ".length());
    }

    private Centre createCentre(String unique) {
        Centre centre = new Centre();
        centre.setCentreName("Walk-in Centre " + unique);
        centre.setStreet("1 Test Road");
        centre.setCity("Colombo");
        centre.setDistrict("Colombo");
        centre.setDailyCapacity(100);
        return centreRepository.saveAndFlush(centre);
    }

    private Integer createCsr(String unique) {
        UserAccount user = new UserAccount();
        user.setEmail("walkin-csr-" + unique + "@gov.lk");
        user.setPhone("+947" + String.format("%08d", Math.abs((unique + "csr").hashCode()) % 100_000_000));
        user.setPasswordHash("test-hash");
        user = userRepository.saveAndFlush(user);
        StaffAccount staff = new StaffAccount(user.getUserId(), "EMP-WALK-" + unique);
        staff.setStaffRole(OfficerAccountService.CSR);
        staffRepository.saveAndFlush(staff);
        return user.getUserId();
    }

    private MockHttpSession csrSession(Integer csrId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loggedInOfficerId", csrId);
        session.setAttribute("loggedInOfficerRole", OfficerAccountService.CSR);
        session.setAttribute("loggedInOfficerRoleLabel", "Citizen Service Representative");
        session.setAttribute("loggedInOfficerEmployeeNo", "EMP-WALK-TEST");
        session.setAttribute("loggedInOfficerEmail", "csr@gov.lk");
        return session;
    }
}
