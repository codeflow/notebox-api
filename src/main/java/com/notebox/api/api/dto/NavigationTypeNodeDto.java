package com.notebox.api.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * One annotation type under the Annotations root (C28), carrying the groups its own records occupy
 * (OQ-23). Types are structural: the node is present whether or not the type has any records, so an
 * empty {@code groups} list is normal rather than exceptional.
 */
public record NavigationTypeNodeDto(UUID typeId, String name, List<NavigationGroupNodeDto> groups) {
}
