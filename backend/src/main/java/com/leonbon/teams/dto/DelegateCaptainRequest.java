package com.leonbon.teams.dto;

import jakarta.validation.constraints.NotBlank;

public record DelegateCaptainRequest(@NotBlank String newCaptainUserId) {}
