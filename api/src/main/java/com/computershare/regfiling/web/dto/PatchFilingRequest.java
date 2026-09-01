package com.computershare.regfiling.web.dto;

import java.util.Map;

// Generic field-map PATCH per openapi.yaml's FilingController.updateFiling — mirrors the
// documented Filing.fields JSONB shape even though Filing itself uses direct columns in MVP
// (see domain/Filing.java note); this DTO is what maps request JSON onto those columns.
public class PatchFilingRequest {

    private Map<String, Object> fields;

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
}
