package com.igrowker.miniproject.Collaborator.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollaboratorCreateRequestDTO {
    @NotBlank(message = "Name is required")
    public String name;
    @NotBlank(message = "Service is required")
    public String service;
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    public Double amount;
    @NotNull(message = "User id is required")
    @Positive(message = "User id must be positive")
    public Long user_id;
}