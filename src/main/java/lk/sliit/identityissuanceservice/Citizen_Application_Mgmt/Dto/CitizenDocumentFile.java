package lk.sliit.identityissuanceservice.Citizen_Application_Mgmt.Dto;

import org.springframework.core.io.Resource;

/** Safe file metadata returned only after citizen ownership has been verified. */
public record CitizenDocumentFile(Resource resource, String contentType, String fileName) {
}
