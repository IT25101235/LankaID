package lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Repository;

import lk.sliit.identityissuanceservice.Biometric_And_Appointment_Mgmt.Model.BiometricCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Repository pattern: ready-made CRUD for biometric collection records. */
// OOP - Inheritance, Abstraction and Polymorphism: this interface extends JpaRepository, so it inherits
// save, find and delete. We only declare it; Spring supplies the real implementation at runtime.
@Repository
public interface BiometricCollectionRepository extends JpaRepository<BiometricCollection, Integer> {
}
