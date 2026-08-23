package com.notebox.api.api.dto;

import java.util.List;

/**
 * The Navigator's data (FR-09, OQ-23): two roots, and nothing below group level. Individual records
 * and tasks are never enumerated — a node is resolved by calling the paginated listing with the
 * matching group filter, which keeps this payload bounded by types x groups (NFR-08).
 */
public record NavigationTreeDto(
        List<NavigationTypeNodeDto> annotations,
        List<NavigationGroupNodeDto> tasks) {
}
