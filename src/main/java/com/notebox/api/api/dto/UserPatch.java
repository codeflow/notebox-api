package com.notebox.api.api.dto;

import jakarta.validation.constraints.Size;

/** Mutable user fields (AD-07). Null fields are left unchanged. */
public record UserPatch(
        @Size(max = 120) String displayName,
        @Size(max = 5) String locale) {
}
