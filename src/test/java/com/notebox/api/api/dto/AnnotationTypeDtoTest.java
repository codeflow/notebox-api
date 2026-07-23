package com.notebox.api.api.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import com.notebox.api.domain.AnnotationType;
import com.notebox.api.domain.BadgeColour;
import com.notebox.api.domain.FieldOption;
import com.notebox.api.domain.FieldType;
import com.notebox.api.domain.TypeField;
import org.junit.jupiter.api.Test;

/** The response DTO mirrors the aggregate: declared field/option order, field type, secret flag, colour. */
class AnnotationTypeDtoTest {

    @Test
    void from_mapsAggregate_preservingOrderAndAttributes() {
        AnnotationType type = new AnnotationType(UUID.randomUUID(), "RabbitMQ");
        TypeField url = new TypeField("URL", FieldType.TEXT);
        url.setSecret(true);
        TypeField status = new TypeField("Status", FieldType.LIST);
        status.addOption(new FieldOption("open", BadgeColour.GREEN));
        status.addOption(new FieldOption("blocked", BadgeColour.RED));
        type.addField(url);
        type.addField(status);

        AnnotationTypeDto dto = AnnotationTypeDto.from(type);

        assertEquals("RabbitMQ", dto.name());
        assertEquals(2, dto.fields().size());
        assertEquals("URL", dto.fields().get(0).name());
        assertEquals("TEXT", dto.fields().get(0).fieldType());
        assertTrue(dto.fields().get(0).secret());
        assertEquals("Status", dto.fields().get(1).name());
        assertEquals(2, dto.fields().get(1).options().size());
        assertEquals("open", dto.fields().get(1).options().get(0).label());
        assertEquals("GREEN", dto.fields().get(1).options().get(0).badgeColour());
    }
}
