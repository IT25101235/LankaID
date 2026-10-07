package lk.sliit.identityissuanceservice.User_Mgmt.Dto;

import java.io.Serializable;

public record PreparedCitizenRegistration(
        String email,
        String phone,
        String passwordHash
) implements Serializable {
}
