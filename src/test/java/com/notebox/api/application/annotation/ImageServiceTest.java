package com.notebox.api.application.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.UUID;

import jakarta.inject.Inject;

import com.notebox.api.domain.Image;
import com.notebox.api.domain.Tenant;
import com.notebox.api.domain.error.ImageNotFoundException;
import com.notebox.api.domain.error.ImageTooLargeException;
import com.notebox.api.domain.error.UnsupportedImageTypeException;
import com.notebox.api.infrastructure.security.TenantContext;
import com.notebox.api.testsupport.TestData;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import org.junit.jupiter.api.Test;

/** Image content-type/size validation before persistence and tenant-scoped retrieval (FR-07, NFR-04, C-07). */
@QuarkusTest
class ImageServiceTest {

    @Inject
    ImageService imageService;

    @Inject
    TestData data;

    @InjectMock
    TenantContext tenantContext;

    @Test
    @TestTransaction
    void store_validPng_persistsWithMetadata() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());

        Image stored = imageService.store("image/png", new byte[] {1, 2, 3, 4});

        assertEquals("image/png", stored.getContentType());
        assertEquals(4, stored.getSizeBytes());
        assertTrue(imageService.get(stored.getId()).getId().equals(stored.getId()));
    }

    @Test
    @TestTransaction
    void store_oversize_throwsImageTooLarge() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());

        byte[] tooBig = new byte[5_242_881];
        assertThrows(ImageTooLargeException.class, () -> imageService.store("image/png", tooBig));
    }

    @Test
    @TestTransaction
    void store_unsupportedType_throwsUnsupportedImageType() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());

        assertThrows(UnsupportedImageTypeException.class,
                () -> imageService.store("image/svg+xml", new byte[] {1, 2}));
    }

    @Test
    @TestTransaction
    void get_missing_throwsImageNotFound() {
        Tenant tenant = data.createTenant();
        when(tenantContext.tenantId()).thenReturn(tenant.getId());

        assertThrows(ImageNotFoundException.class, () -> imageService.get(UUID.randomUUID()));
    }
}
