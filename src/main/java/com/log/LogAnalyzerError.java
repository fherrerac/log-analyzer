package com.log;

public class LogAnalyzerError {
    private int code;
    private String message;

    LogAnalyzerError(Code code) {
        this.code = code.id;
        this.message = code.message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public enum Code {
        NOT_FOUND(1, "Not found");

        private int id;
        private String message;

        Code(int id, String message) {
            this.id = id;
            this.message = message;
        }
    }
}
