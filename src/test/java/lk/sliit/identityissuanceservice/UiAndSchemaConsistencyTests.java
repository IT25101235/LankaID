package lk.sliit.identityissuanceservice;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiAndSchemaConsistencyTests {

    private static final Path TEMPLATE_ROOT = Path.of("src", "main", "resources", "templates");

    @Test
    void everyLiveCitizenAndOfficerTemplateLoadsTheSharedPortalShell() throws IOException {
        List<String> liveTemplates = List.of(
                "citizen_frontends/citizen_dashboard.html",
                "citizen_frontends/nic_application_form.html",
                "citizen_frontends/passport_application_form.html",
                "citizen_frontends/application_document_upload.html",
                "citizen_frontends/citizen_application_review.html",
                "citizen_frontends/application_tracking.html",
                "citizen_frontends/payment_gateway.html",
                "citizen_frontends/biometric_appointment_booking.html",
                "citizen_frontends/biometric_appointment_pass.html",
                "citizen_frontends/walkin_appointment_booking.html",
                "citizen_frontends/walkin_appointment_pass.html",
                "citizen_frontends/walkin_appointment_recovery.html",
                "citizen_frontends/citizen_support_requests.html",
                "citizen_frontends/citizen_support_detail.html",
                "citizen_frontends/citizen_complaints.html",
                "citizen_frontends/citizen_notifications.html",
                "officers_frontends/nic_verification_queue.html",
                "officers_frontends/passport_verification_queue.html",
                "officers_frontends/processed_applications.html",
                "officers_frontends/application_review.html",
                "officers_frontends/officer_application_tracking.html",
                "officers_frontends/officer_appointments.html",
                "officers_frontends/biometric_schedule.html",
                "officers_frontends/biometric_collection.html",
                "officers_frontends/csr_dashboard.html",
                "officers_frontends/csr_support_requests.html",
                "officers_frontends/csr_complaints_detail.html",
                "officers_frontends/walkin_management.html",
                "officers_frontends/document_collection.html",
                "officers_frontends/director_dashboard.html",
                "officers_frontends/printing_export.html",
                "officers_frontends/admin_dashboard.html"
        );

        for (String template : liveTemplates) {
            String html = Files.readString(TEMPLATE_ROOT.resolve(template));
            assertTrue(html.contains("/js/portal-shell.js"), template + " must load the shared portal shell");
        }
    }

    @Test
    void removedLegacyTemplatesDoNotReturn() {
        List<String> legacyTemplates = List.of(
                "citizen_frontends/nic_application_form_new.html",
                "citizen_frontends/nic_application_form_renewal.html",
                "citizen_frontends/nic_application_form_replacement.html",
                "citizen_frontends/nic_document_upload.html",
                "citizen_frontends/nic_application_review.html",
                "citizen_frontends/passport_application_form_new.html",
                "citizen_frontends/passport_application_form_renewal.html",
                "citizen_frontends/passport_application_form_replacement.html",
                "citizen_frontends/passport_document_upload.html",
                "citizen_frontends/passport_application_review.html",
                "officers_frontends/csr_walkin_booking.html",
                "officers_frontends/director_command_center.html",
                "officers_frontends/final_review_submission.html"
        );

        for (String template : legacyTemplates) {
            assertFalse(Files.exists(TEMPLATE_ROOT.resolve(template)), template + " is disconnected legacy UI");
        }
    }

    @Test
    void everyOfficerTemplateExposesAuthenticatedIdentityToTheSharedSidebar() throws IOException {
        List<String> officerTemplates = List.of(
                "application_review.html", "admin_dashboard.html", "biometric_collection.html",
                "biometric_schedule.html",
                "csr_dashboard.html", "csr_support_requests.html", "csr_complaints_detail.html",
                "director_dashboard.html", "document_collection.html", "nic_verification_queue.html",
                "officer_appointments.html", "officer_application_tracking.html",
                "passport_verification_queue.html", "printing_export.html",
                "processed_applications.html", "walkin_management.html"
        );

        for (String template : officerTemplates) {
            String html = Files.readString(TEMPLATE_ROOT.resolve("officers_frontends").resolve(template));
            assertTrue(html.contains("data-officer-email=${loggedInOfficerEmail}"),
                    template + " must expose the authenticated email");
            assertTrue(html.contains("data-officer-rank=${loggedInOfficerRoleLabel}"),
                    template + " must expose the authenticated rank");
            assertTrue(html.contains("data-officer-login-time=${loggedInOfficerLoginTime}"),
                    template + " must expose the officer login time");
        }
    }

    @Test
    void csrReviewAndDirectorDashboardKeepTheRequestedUsabilityFixes() throws IOException {
        String supportReview = Files.readString(
                TEMPLATE_ROOT.resolve("officers_frontends/csr_complaints_detail.html"));
        String director = Files.readString(
                TEMPLATE_ROOT.resolve("officers_frontends/director_dashboard.html"));
        String governmentTheme = Files.readString(Path.of(
                "src", "main", "resources", "static", "css", "government-theme.css"));
        String portalShell = Files.readString(Path.of(
                "src", "main", "resources", "static", "js", "portal-shell.js"));

        assertTrue(supportReview.contains("support-back-button"));
        assertTrue(supportReview.contains("Back to support queue"));
        assertTrue(governmentTheme.contains(".metric-card {"));
        assertTrue(director.contains("legend:{labels:{color:'#334155'}}"));
        assertTrue(portalShell.contains("/officer/admin?section=centres"));
    }

    @Test
    void citizenAndPublicWalkInPagesUseTheSimpleTopNavigation() throws IOException {
        String portalShell = Files.readString(Path.of(
                "src", "main", "resources", "static", "js", "portal-shell.js"));
        String portalStyles = Files.readString(Path.of(
                "src", "main", "resources", "static", "css", "government-theme.css"));
        String landingPage = Files.readString(TEMPLATE_ROOT.resolve("index.html"));
        String citizenNavigation = portalShell.substring(
                portalShell.indexOf("function citizenConfig()"),
                portalShell.indexOf("function publicWalkInConfig()"));
        String publicWalkInNavigation = portalShell.substring(
                portalShell.indexOf("function publicWalkInConfig()"),
                portalShell.indexOf("function officerConfig("));

        assertTrue(portalShell.contains("if (isOfficer) mountOfficerSidebar()"));
        assertTrue(portalShell.contains("else mountCitizenTopbar(publicWalkIn)"));
        assertTrue(portalShell.contains("replace(/;[^/]+/g, '')"));
        assertTrue(portalShell.contains("function publicWalkInConfig()"));
        assertTrue(portalShell.contains("Contact / Complaint"));
        assertFalse(citizenNavigation.contains("Walk-in"));
        assertFalse(publicWalkInNavigation.contains("Help"));
        assertFalse(publicWalkInNavigation.contains("/login"));
        assertTrue(portalStyles.contains("body.portal-citizen"));
        assertTrue(portalStyles.contains(".citizen-topbar"));
        assertTrue(portalStyles.contains("background: var(--portal-secondary) !important"));
        assertTrue(portalStyles.contains("position: sticky"));
        assertTrue(landingPage.contains("gov-site-header gov-official-strip"));
        assertTrue(landingPage.contains("Access Dashboard"));
        assertTrue(landingPage.contains("th:if=\"${!landingSignedIn}\""));
    }

    @Test
    void citizenSupportRenewalAndHomeDeliveryControlsRemainUsable() throws IOException {
        String support = Files.readString(
                TEMPLATE_ROOT.resolve("citizen_frontends/citizen_support_requests.html"));
        String nicForm = Files.readString(
                TEMPLATE_ROOT.resolve("citizen_frontends/nic_application_form.html"));
        String passportForm = Files.readString(
                TEMPLATE_ROOT.resolve("citizen_frontends/passport_application_form.html"));
        String payment = Files.readString(
                TEMPLATE_ROOT.resolve("citizen_frontends/payment_gateway.html"));

        assertTrue(support.contains("Contact and complaints"));
        assertTrue(support.contains("Make a Complaint"));
        assertFalse(support.contains("<header class="));
        assertTrue(nicForm.contains("th:field=\"*{lastNicNo}\" required readonly"));
        assertTrue(nicForm.contains("th:readonly=\"${applicationForm.lastIssuedDate != null}\""));
        assertTrue(passportForm.contains("cannot be changed"));
        assertTrue(payment.contains("centreSelection.classList.toggle('hidden', home)"));
        assertTrue(payment.contains("centre.disabled = home"));
        assertTrue(payment.contains("if (home) centre.value = ''"));
    }

    @Test
    void biometricAppointmentPagesExposeHistoryAndScheduleTabs() throws IOException {
        String history = Files.readString(
                TEMPLATE_ROOT.resolve("officers_frontends/officer_appointments.html"));
        String schedule = Files.readString(
                TEMPLATE_ROOT.resolve("officers_frontends/biometric_schedule.html"));

        assertTrue(history.contains("Appointment History"));
        assertTrue(history.contains("Schedule Settings"));
        assertTrue(history.contains("${scheduleRoute}"));
        assertTrue(schedule.contains("Appointment History"));
        assertTrue(schedule.contains("Schedule Settings"));
        assertTrue(schedule.contains("/appointments/schedule"));
    }

    @Test
    void teammateSetupScriptContainsEveryMappedPhysicalTable() throws IOException {
        String sql = Files.readString(Path.of("DATABASE_SETUP.sql"));
        List<String> tables = List.of(
                "GovernmentIdentityRecord", "UserAccount", "CitizenAccount", "StaffAccount", "Centre",
                "Application", "NICApplication", "PassportApplication", "ApplicationDocument",
                "ApplicationReview", "Appointment", "AppointmentCheckIn", "Payment", "BiometricCollection",
                "Notification", "UserNotification", "SupportRequest", "DocumentDelivery", "WalkInSlot"
        );

        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE dbo." + table), table + " must be present in DATABASE_SETUP.sql");
        }
    }
}
