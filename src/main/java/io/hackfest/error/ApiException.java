package io.hackfest.error;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class ApiException extends RuntimeException {
    private final int statusCode;
    private final List<Error> errors;

    public ApiException(int statusCode, Error... errors) {
        super(Arrays.toString(errors));
        this.statusCode = statusCode;
        this.errors = Arrays.asList(errors);
    }

    public static ApiException badRequest(ErrorCode errorCode, Object... args) {
        return new ApiException(400, new Error(errorCode, args));
    }

    public static ApiException unauthorized(ErrorCode errorCode, Object... args) {
        return new ApiException(401, new Error(errorCode, args));
    }

    public static ApiException serverError(ErrorCode errorCode, Object... args) {
        return new ApiException(500, new Error(errorCode, args));
    }

    public static ApiException badRequest(Collection<Error> errors) {
        return new ApiException(400, errors.toArray(new Error[0]));
    }

    public List<Error> getErrors() {
        return errors;
    }
}
