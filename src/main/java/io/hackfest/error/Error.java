package io.hackfest.error;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;

public class Error {
    private final ErrorCode errorCode;
    private final Object[] args;
    private final String message;

    public Error(ErrorCode errorCode, Object... args) {
        this.errorCode = errorCode;
        this.args = args;
        this.message = MessageFormat.format(errorCode.getMesssage(), args);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object[] getArgs() {
        return args;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Error{" +
                "errorCode=" + errorCode +
                ", args=" + Arrays.toString(args) +
                ", message='" + message + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Error error = (Error) o;
        return errorCode == error.errorCode && Arrays.equals(args, error.args);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(errorCode);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }
}
