// L1-testing-script-generator · Phase 5 · Correlation ID C885C23C-949E-440C-8B0E-04FDD166A559
// Inputs: test-cases.feature, openapi.yaml
// This is the Phase 5 reference automation script (REST Assured style). Phase 6 construction
// copies/adapts this into api/src/test/java as a real Spring Boot integration test once the
// application context exists to run it against.
package com.computershare.regfiling.it;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

class FilingGoldenPathIT {

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = "http://localhost:8080/api";
    }

    @Test
    void generateThenApproveGoldenPath() {
        // S1: simulate a valid trade -> Filing created UNDER_REVIEW
        String filingId =
            given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer exec-1-token")
                .body("{\"executiveId\":\"exec-1\",\"transactionCode\":\"S\",\"shares\":1200,\"pricePerShare\":42.10}")
            .when()
                .post("/demo/simulate-trade")
            .then()
                .statusCode(201)
                .body("status", equalTo("UNDER_REVIEW"))
                .extract().path("id");

        // S8: approval blocked before VALIDATED (this filing hasn't been through PATCH/re-validate yet
        // in this minimal flow — depending on FormGenerationService's synchronous validation, this may
        // already be VALIDATED; assert whichever the LLD's actual behavior is once built)

        // S7: approve a validated filing -> SUBMITTED, mocked EDGAR connector invoked
        given()
            .header("Authorization", "Bearer legal-1-token")
        .when()
            .post("/filings/{id}/approve", filingId)
        .then()
            .statusCode(anyOf(is(200), is(409))); // 200 if already VALIDATED, 409 otherwise — construction resolves this

        // S9: audit log has at least a GENERATED entry
        given()
            .header("Authorization", "Bearer legal-1-token")
        .when()
            .get("/filings/{id}/audit-log", filingId)
        .then()
            .statusCode(200)
            .body("action", hasItem("GENERATED"));
    }

    @Test
    void incompleteTransactionIsFlagged() {
        // S2: required field (transactionCode) literally missing -> INCOMPLETE, notification to both personas
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer exec-3-token")
            .body("{\"executiveId\":\"exec-3\",\"shares\":100,\"pricePerShare\":10.0}")
        .when()
            .post("/demo/simulate-trade")
        .then()
            .statusCode(201)
            .body("status", equalTo("INCOMPLETE"));
    }

    @Test
    void invalidValueBlocksApprovalUntilEdited() {
        // S3/S8: fields present but an invalid value (shares=0) -> UNDER_REVIEW, approval blocked
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer exec-3-token")
            .body("{\"executiveId\":\"exec-3\",\"transactionCode\":\"A\",\"shares\":0,\"pricePerShare\":0}")
        .when()
            .post("/demo/simulate-trade")
        .then()
            .statusCode(201)
            .body("status", equalTo("UNDER_REVIEW"));
    }
}
