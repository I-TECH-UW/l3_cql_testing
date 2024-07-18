package org.uwdigi.who.l3.cqltesting;

public class APIException extends RuntimeException {

    public static final long serialVersionUID = 12121212L;
    private int statusCode;
    private String errorMessage;

    public APIException() {
        super();
    }
    public APIException(String message) {
        super(message);
        this.errorMessage = message;
    }

    public APIException(String message, Throwable cause) {
        super(message, cause);
        this.errorMessage = message;
    }

    public APIException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorMessage = message;
    }

    public APIException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorMessage = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "APIException{" +
                "statusCode=" + statusCode +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

}
