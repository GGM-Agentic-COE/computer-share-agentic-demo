package com.computershare.regfiling.web.dto;

public final class ApiExceptions {

    private ApiExceptions() {
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) {
            super(message);
        }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }

    public static class ValidationFailedException extends RuntimeException {
        private final String filingId;
        private final java.util.List<String> errors;

        public ValidationFailedException(String filingId, java.util.List<String> errors) {
            super("Validation failed for filing " + filingId);
            this.filingId = filingId;
            this.errors = errors;
        }

        public String getFilingId() {
            return filingId;
        }

        public java.util.List<String> getErrors() {
            return errors;
        }
    }
}
