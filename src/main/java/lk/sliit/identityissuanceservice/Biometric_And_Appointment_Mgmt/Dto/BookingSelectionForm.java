package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Dto;

/** Keep the user's entered values when a booking change cannot be accepted. */
public class BookingSelectionForm {
    // OOP - Encapsulation: private fields with public getters and setters (the form fills them in).
    private String centreId;
    private String slotId;

    public String getCentreId() { return centreId; }
    public void setCentreId(String value) { centreId = value; }
    public String getSlotId() { return slotId; }
    public void setSlotId(String value) { slotId = value; }

    /**
     * Input validation: turns the form text for the chosen slot into a number, or gives a friendly
     * error.
     */
    public Integer readSlot() { return readId(slotId, "Select an available appointment slot."); }
    private Integer readId(String value, String message) {
        try {
            int id = Integer.parseInt(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(message);
    }
}
