package com.notebox.api.domain;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * An inline card reference (FR-13, OQ-06): a required code plus an optional absolute link, owned by
 * exactly one task or subtask and stored in that owner's own row — never a shared entity. A card
 * whose columns are all null materializes as a null {@code Card}, which is the "no card" state.
 * Value semantics: two cards with the same code and url are equal.
 */
@Embeddable
public class Card {

    @Column(name = "card_code", length = 60)
    private String code;

    @Column(name = "card_url", length = 2048)
    private String url;

    protected Card() {
    }

    public Card(String code, String url) {
        this.code = code;
        this.url = url;
    }

    public String getCode() {
        return code;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Card that)) {
            return false;
        }
        return Objects.equals(code, that.code) && Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, url);
    }
}
