package com.workflowdemo.backend.formdefinition;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_form_fields")
public class ApplicationFormField {

    @Id
    private UUID id;

    @Column(name = "form_definition_id", nullable = false)
    private UUID formDefinitionId;

    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(nullable = false)
    private String label;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(nullable = false)
    private boolean required;

    private String placeholder;

    @Column(name = "initial_value_type", nullable = false)
    private String initialValueType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    UUID getId() {
        return id;
    }

    public UUID getFormDefinitionId() {
        return formDefinitionId;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getLabel() {
        return label;
    }

    public String getDataType() {
        return dataType;
    }

    public boolean isRequired() {
        return required;
    }

    String getPlaceholder() {
        return placeholder;
    }

    String getInitialValueType() {
        return initialValueType;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    boolean isActive() {
        return active;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
