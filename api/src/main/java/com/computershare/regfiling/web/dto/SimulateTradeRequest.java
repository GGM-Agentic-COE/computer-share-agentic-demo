package com.computershare.regfiling.web.dto;

import jakarta.validation.constraints.NotBlank;

// pricePerShare/shares/transactionCode are intentionally boxed (Integer/Double/String), not
// primitives, so a client omitting them deserializes to null — that null is exactly what
// FormGenerationService's "required field missing" (INCOMPLETE) check looks for.
public class SimulateTradeRequest {

    @NotBlank
    private String executiveId;

    private String transactionCode;
    private Integer shares;
    private Double pricePerShare;

    public String getExecutiveId() {
        return executiveId;
    }

    public void setExecutiveId(String executiveId) {
        this.executiveId = executiveId;
    }

    public String getTransactionCode() {
        return transactionCode;
    }

    public void setTransactionCode(String transactionCode) {
        this.transactionCode = transactionCode;
    }

    public Integer getShares() {
        return shares;
    }

    public void setShares(Integer shares) {
        this.shares = shares;
    }

    public Double getPricePerShare() {
        return pricePerShare;
    }

    public void setPricePerShare(Double pricePerShare) {
        this.pricePerShare = pricePerShare;
    }
}
