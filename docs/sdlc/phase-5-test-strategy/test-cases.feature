# L1-testing-case-writer · Phase 5 · Correlation ID C885C23C-949E-440C-8B0E-04FDD166A559
# Input: test-scenarios.md

Feature: Automated SEC Form 4 generation
  Scenario: Generate Form 4 after valid trade
    Given executive "exec-1" executes a valid trade with all required fields
    When the transaction event is processed
    Then a Filing is created within 5 minutes with status "UNDER_REVIEW"
    And an audit log entry with action "GENERATED" is written

  Scenario: Incomplete transaction data flags the form
    Given executive "exec-1" executes a trade missing a required field
    When the transaction event is processed
    Then a Filing is created with status "INCOMPLETE"
    And a notification of type "INCOMPLETE_DATA" is sent to both the executive and legal & compliance

Feature: Form field validation
  Scenario: Submission blocked on validation failure
    Given a Filing with an invalid "shares" value
    When a Legal & Compliance user calls POST /filings/{id}/approve
    Then the response is 409 Conflict
    And the Filing status remains unchanged

  Scenario: Valid submission proceeds
    Given a Filing with all fields valid and status "VALIDATED"
    When a Legal & Compliance user calls POST /filings/{id}/approve
    Then the response is 200 OK
    And the Filing status becomes "SUBMITTED"

Feature: Legal review and edit
  Scenario: Edit pre-filled form field
    Given a Filing with status "UNDER_REVIEW"
    When a Legal & Compliance user PATCHes a field with a valid value
    Then the field is updated
    And the Filing is re-validated
    And an audit log entry with action "EDITED" is written

  Scenario: Invalid edit rejected
    Given a Filing with status "UNDER_REVIEW"
    When a Legal & Compliance user PATCHes a field with an invalid value
    Then the response is 422 with field-level error messages
    And the invalid value is not persisted

Feature: Approval workflow
  Scenario: Approve validated form
    Given a Filing with status "VALIDATED"
    When a Legal & Compliance user calls POST /filings/{id}/approve
    Then the mocked EdgarSubmissionConnector is invoked
    And the Filing status becomes "SUBMITTED"
    And audit log entries with actions "APPROVED" and "SUBMITTED" are written

  Scenario: Approval blocked pre-validation
    Given a Filing with status "UNDER_REVIEW" (not yet VALIDATED)
    When a Legal & Compliance user calls POST /filings/{id}/approve
    Then the response is 409 Conflict

Feature: Audit logging
  Scenario: Audit entry on every action
    Given any of generate, edit, approve, or submit occurs on a Filing
    When the action completes
    Then a corresponding audit_log row exists with actor, action, filing_id, and timestamp

Feature: Historical filing access
  Scenario: View filing history
    Given executive "exec-1" has 3 filings in different statuses
    When exec-1 calls GET /filings?executiveId=exec-1
    Then all 3 filings are returned with their current status

Feature: Notifications
  Scenario: Notify on generation
    Given a Filing transitions to "UNDER_REVIEW"
    Then a notification of type "FORM_GENERATED" exists for the executive

  Scenario: Deadline risk alert fires at 12h against EDGAR cutoff
    Given a Filing with edgar_cutoff_at 11 hours from now and status "UNDER_REVIEW"
    When the DeadlineRiskEvaluator runs
    Then a notification of type "DEADLINE_RISK" is created for all LEGAL_COMPLIANCE users
    And deadline_risk_fired becomes true
    And running the evaluator again does not create a duplicate notification

  Scenario: Notify on submission
    Given a Filing transitions to "SUBMITTED"
    Then a notification of type "SUBMITTED" exists for both the executive and legal & compliance users

Feature: Compliance dashboard
  Scenario: Dashboard shows all filings
    Given filings exist with statuses "UNDER_REVIEW", "VALIDATED", and "SUBMITTED"
    When a Legal & Compliance user calls GET /filings
    Then all filings are returned regardless of status

  Scenario: Filter dashboard
    Given filings exist for multiple executives and statuses
    When a Legal & Compliance user calls GET /filings?status=UNDER_REVIEW
    Then only filings with status "UNDER_REVIEW" are returned
