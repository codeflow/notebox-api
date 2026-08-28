package com.notebox.api.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** New wording for one key. Blank is refused — clearing an override is a DELETE, not an empty PUT. */
public record TranslationInput(
        @NotBlank(message = "translation.value.required")
        @Size(max = 2000) String value) {
}
