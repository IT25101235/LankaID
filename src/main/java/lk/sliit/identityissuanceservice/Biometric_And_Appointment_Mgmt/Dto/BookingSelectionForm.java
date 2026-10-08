package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Keep the user's entered values when a booking change cannot be accepted. */
public class BookingSelectionForm {
    private String centreId;
    private String appointmentDate;
    private String appointmentTime;
    private String slotId;

    public String getCentreId() { return centreId; }
    public void setCentreId(String value) { centreId = value; }
    public String getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(String value) { appointmentDate = value; }
    public String getAppointmentTime() { return appointmentTime; }
    public void setAppointmentTime(String value) { appointmentTime = value; }
    public String getSlotId() { return slotId; }
    public void setSlotId(String value) { slotId = value; }

    /**
     * Input validation: these methods turn form text into numbers, dates and times, or give a friendly
     * error.
     */
    public Integer readCentre() { return readId(centreId, "Select a valid service centre."); }
    public Integer readSlot() { return readId(slotId, "Select an available appointment slot."); }
    public LocalDate readDate() {
        try { return LocalDate.parse(appointmentDate == null ? "" : appointmentDate); }
        catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Select a valid appointment date.");
        }
    }
    public LocalTime readTime() {
        try { return LocalTime.parse(appointmentTime == null ? "" : appointmentTime); }
        catch (java.time.format.DateTimeParseException exception) {
            throw new IllegalArgumentException("Select a valid appointment time.");
        }
    }
    private Integer readId(String value, String message) {
        try {
            int id = Integer.parseInt(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(message);
    }
}
