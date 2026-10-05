package lk.sliit.identityissuanceservice.Chatbot_Mgmt.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank @Size(max = 500) String message
) { }
