package com.notebox.api.domain;

/**
 * The closed set of group namespaces (FR-08, OQ-04). Annotation groups and task groups are
 * distinct namespaces, so the same name may exist once in each; the value is fixed when a group is
 * created and never changes, because changing it would silently orphan every member.
 */
public enum GroupDomain {

    ANNOTATION,
    TASK
}
