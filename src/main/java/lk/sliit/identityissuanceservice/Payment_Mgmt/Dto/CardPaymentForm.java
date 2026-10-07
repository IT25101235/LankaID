package lk.sliit.identityissuanceservice.Payment_Mgmt.Dto;

/** Request-only simulation details. Never stored in the database or session. */
public class CardPaymentForm {
    private String cardholderName;
    private String cardNumber;
    private String expiry;
    private String cvv;

    public String getCardholderName() { return cardholderName; }
    public void setCardholderName(String cardholderName) { this.cardholderName = cardholderName; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }
    public String getCvv() { return cvv; }
    public void setCvv(String cvv) { this.cvv = cvv; }
}
