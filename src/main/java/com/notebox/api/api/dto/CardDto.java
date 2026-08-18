package com.notebox.api.api.dto;

import com.notebox.api.domain.Card;

/** Wire shape of an inline card (FR-13): the code and its optional absolute link. */
public record CardDto(String code, String url) {

    /**
     * Maps the value object to its wire shape.
     *
     * @param card the inline card, or null when the owner carries none
     * @return the wire representation, or null for no card
     */
    public static CardDto from(Card card) {
        return card == null ? null : new CardDto(card.getCode(), card.getUrl());
    }
}
