package com.notebox.api.api.dto;

import java.util.List;

/** One page of a paginated listing plus the pager facts (NFR-08; feat-008 contracts/listing.md). */
public record PageDto<T>(List<T> items, int page, int size, long total) {
}
