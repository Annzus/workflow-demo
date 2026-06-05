package com.workflowdemo.backend.application;

import java.time.Instant;
import java.util.UUID;

import com.workflowdemo.backend.formdefinition.ApplicationFormField;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "application_field_values")
class ApplicationFieldValue {

    @Id
    private UUID id;

    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    @Column(name = "field_key", nullable = false)
    private String fieldKey;

    @Column(nullable = false)
    private String label;

    @Column(name = "data_type", nullable = false)
    private String dataType;

    @Column(name = "value_text")
    private String valueText;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApplicationFieldValue() {
    }

    ApplicationFieldValue(UUID applicationId, ApplicationFormField field, String valueText) {
        this.id = UUID.randomUUID();
        this.applicationId = applicationId;
        this.fieldKey = field.getFieldKey();
        this.label = field.getLabel();
        this.dataType = field.getDataType();
        this.valueText = valueText;
        this.displayOrder = field.getDisplayOrder();
        this.createdAt = Instant.now();
    }

    String getFieldKey() {
        return fieldKey;
    }

    String getLabel() {
        return label;
    }

    String getDataType() {
        return dataType;
    }

    String getValueText() {
        return valueText;
    }

    int getDisplayOrder() {
        return displayOrder;
    }
}
