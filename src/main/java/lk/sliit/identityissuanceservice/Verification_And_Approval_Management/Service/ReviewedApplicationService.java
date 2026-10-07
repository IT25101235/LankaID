package lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Service;

import java.time.format.DateTimeParseException;

import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Dto.ReviewedApplicationRow;
import lk.sliit.identityissuanceservice.Verification_And_Approval_Management.Repository.ApplicationReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ReviewedApplicationService {
    private final ApplicationReviewRepository reviews;
    public ReviewedApplicationService(ApplicationReviewRepository reviews) { this.reviews = reviews; }

    @Transactional(readOnly = true)
    public Page<ReviewedApplicationRow> find(String service, String search, String decision, String date, int page) {
        if (!List.of("NIC", "PASSPORT").contains(service)) throw new IllegalArgumentException("Select a valid service.");
        if (!List.of("", "APPROVED", "REJECTED", "CORRECTION_REQUIRED").contains(decision)) {
            throw new IllegalArgumentException("Select a valid review decision.");
        }
        if (search.length() > 100) throw new IllegalArgumentException("Search must be 100 characters or fewer.");
        LocalDate reviewedOn = null;
        if (!date.isBlank()) {
            try { reviewedOn = LocalDate.parse(date); }
            catch (DateTimeParseException exception) {
                throw new IllegalArgumentException("Enter a valid review date.");
            }
        }
        return reviews.findReviewed(service, decision, search.trim().toLowerCase(Locale.ROOT),
                reviewedOn == null ? null : reviewedOn.atStartOfDay(),
                reviewedOn == null ? null : reviewedOn.plusDays(1).atStartOfDay(),
                PageRequest.of(Math.max(0, page), 15));
    }
}
