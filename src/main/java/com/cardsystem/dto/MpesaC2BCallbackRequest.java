package com.cardsystem.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MpesaC2BCallbackRequest {
    @JsonProperty("TransactionType")
    private String transactionType;
    @JsonProperty("TransID")
    private String transID;
    @JsonProperty("TransTime")
    private String transTime;
    @JsonProperty("TransAmount")
    private String transAmount;
    @JsonProperty("BusinessShortCode")
    private String businessShortCode;
    @JsonProperty("BillRefNumber")
    private String billRefNumber;
    @JsonProperty("InvoiceNumber")
    private String invoiceNumber;
    @JsonProperty("OrgAccountBalance")
    private String orgAccountBalance;
    @JsonProperty("ThirdPartyTransID")
    private String thirdPartyTransID;
    @JsonProperty("MSISDN")
    private String msisdn;
    @JsonProperty("FirstName")
    private String firstName;
    @JsonProperty("MiddleName")
    private String middleName;
    @JsonProperty("LastName")
    private String lastName;
}
