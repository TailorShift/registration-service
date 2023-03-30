package io.hackfest.error;

import org.jboss.resteasy.reactive.RestResponse;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.UUID;

public class ExceptionMappers {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionMappers.class);

    @ServerExceptionMapper
    public RestResponse<ErrorResponse> mapException(ApiException apiException) {
        String errorId = UUID.randomUUID().toString();

        logger.error("API Exception occured (error id {})", errorId);

        return RestResponse.status(
                Response.Status.BAD_REQUEST,
                new ErrorResponse(errorId, apiException.getErrors())
        );
    }
}
