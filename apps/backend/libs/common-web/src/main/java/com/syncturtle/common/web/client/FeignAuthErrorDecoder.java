package com.syncturtle.common.web.client;

import java.io.InputStream;

import com.syncturtle.common.contracts.auth.error.AuthErrorCode;
import com.syncturtle.common.contracts.auth.exception.AuthException;
import com.syncturtle.common.web.dto.response.AuthExceptionResponse;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@RequiredArgsConstructor
public class FeignAuthErrorDecoder implements ErrorDecoder {

    private final JsonMapper mapper;
    private final ErrorDecoder defaultDecoder = new Default();

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();

        if (status == 401 || status == 403) {
            AuthExceptionResponse auth = tryReadAuthException(response);

            if (auth != null) {
                AuthErrorCode code = AuthErrorCode.fromCodeOrDefault(auth.getErrorCode());
                AuthException exception = AuthException.of(code);

                if (auth.getPayload() != null) {
                    auth.getPayload().forEach(exception::with);
                }

                return exception;
            }
        }
        return defaultDecoder.decode(methodKey, response);
    }

    private AuthExceptionResponse tryReadAuthException(Response response) {
        if (response.body() == null) {
            return null;
        }

        try (InputStream is = response.body().asInputStream()) {
            byte[] bytes = is.readAllBytes();
            if (bytes.length == 0) {
                return null;
            }

            return mapper.readValue(bytes, AuthExceptionResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

}
