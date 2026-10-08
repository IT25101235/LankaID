package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.AppointmentCheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository pattern: Spring Data JPA gives ready-made save, find and delete for check-ins. */
@Repository
public interface AppointmentCheckInRepository extends JpaRepository<AppointmentCheckIn, Integer> {
}
