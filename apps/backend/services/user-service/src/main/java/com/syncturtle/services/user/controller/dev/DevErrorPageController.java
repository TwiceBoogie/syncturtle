package com.syncturtle.services.user.controller.dev;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.slf4j.MDC;
import org.springframework.boot.SpringBootVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.ErrorAttributes;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
@Profile("local")
@ConditionalOnBooleanProperty(prefix = "app.dev-error", name = "enabled")
public class DevErrorPageController implements ErrorController {

    private static final String DEFAULT_ERROR_TITLE = "Internal Server Error";
    private static final String DEFAULT_ERROR_MESSAGE = "An unexpected error occurred.";

    private static final List<String> SENSITIVE_HEADERS = List.of(
            "authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "x-auth-token",
            "x-csrf-token");

    private final ErrorAttributes errorAttributes;
    private final Environment environment;
    private final SourceSnippetResolver service;

    public DevErrorPageController(ErrorAttributes errorAttributes, Environment environment,
            SourceSnippetResolver service) {
        this.errorAttributes = Objects.requireNonNull(errorAttributes, "error attributes is required");
        this.environment = Objects.requireNonNull(environment, "environment is required");
        this.service = Objects.requireNonNull(service, "dev error source snippet service is required");
    }

    @RequestMapping(path = "/error", produces = MediaType.TEXT_HTML_VALUE)
    public ModelAndView errorHtml(HttpServletRequest request) {
        Assert.notNull(request, "request is required");

        HttpStatusCode status = resolveStatus(request);
        Map<String, Object> model = buildErrorModel(request, status);

        ModelAndView modelAndView = new ModelAndView("dev-error/dev-error", model);
        modelAndView.setStatus(status);

        return modelAndView;
    }

    @RequestMapping(path = "/error", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> errorJson(HttpServletRequest request) {
        Assert.notNull(request, "request is required");

        HttpStatusCode status = resolveStatus(request);
        Map<String, Object> body = buildErrorModel(request, status);

        return ResponseEntity.status(status).body(body);
    }

    private Map<String, Object> buildErrorModel(HttpServletRequest request, HttpStatusCode status) {
        ErrorAttributeOptions options = ErrorAttributeOptions.of(
                ErrorAttributeOptions.Include.EXCEPTION,
                ErrorAttributeOptions.Include.MESSAGE,
                ErrorAttributeOptions.Include.STACK_TRACE,
                ErrorAttributeOptions.Include.BINDING_ERRORS);

        ServletWebRequest webRequest = new ServletWebRequest(request);
        Map<String, Object> model = new LinkedHashMap<>(this.errorAttributes.getErrorAttributes(webRequest, options));

        Throwable throwable = resolveThrowable(this.errorAttributes.getError(webRequest), request);

        int statusCode = status.value();
        String errorTitle = resolveErrorTitle(status, request, model);
        String displayMessage = resolveDisplayMessage(request, model, throwable, errorTitle);
        String requestUri = resolveRequestUri(request);
        String exceptionType = resolveExceptionType(throwable, model);
        String basePackage = this.environment.getProperty("app.dev-error.base-package", "");

        SourceSnippet sourceSnippet = this.service.findSourceSnippet(throwable);

        List<DevErrorStackFrame> stackFrames = throwable == null
                ? List.of()
                : Arrays.stream(throwable.getStackTrace())
                        .map(frame -> DevErrorStackFrame.from(frame, basePackage))
                        .toList();

        model.put("status", statusCode);
        model.put("statusCode", statusCode);
        model.put("error", errorTitle);
        model.put("errorTitle", errorTitle);
        model.put("message", displayMessage);
        model.put("displayMessage", displayMessage);
        model.put("pageTitle", statusCode + " - " + errorTitle);
        model.put("errorKind", throwable == null ? "HTTP ERROR" : "UNHANDLED");

        model.putIfAbsent("timestamp", Instant.now());
        model.put("path", requestUri);
        model.put("requestUri", requestUri);

        model.put("method", request.getMethod());
        model.put("queryString", request.getQueryString());
        model.put("exceptionType", exceptionType);
        model.put("stackFrames", stackFrames);
        model.put("headers", collectHeaders(request));
        model.put("parameters", collectParameters(request));
        model.put("activeProfiles", String.join(", ", this.environment.getActiveProfiles()));
        model.put("springBootVersion", SpringBootVersion.getVersion());
        model.put("javaVersion", Runtime.version().toString());
        model.put("matchedPattern", request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE));
        model.put("traceId", resolveTraceId());
        model.put("servletErrorMessage", stringValue(request.getAttribute(RequestDispatcher.ERROR_MESSAGE)));

        model.put("sourceFile", sourceSnippet.getSourceFile());
        model.put("sourceLine", sourceSnippet.getSourceLine());
        model.put("sourceSnippet", sourceSnippet.getLines());

        return model;
    }

    private static HttpStatusCode resolveStatus(HttpServletRequest request) {
        int statusCode = resolveStatusCode(request);

        if (statusCode < 100 || statusCode > 999) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }

        try {
            return HttpStatusCode.valueOf(statusCode);
        } catch (IllegalArgumentException exception) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private static int resolveStatusCode(HttpServletRequest request) {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (statusCode instanceof Number number) {
            return number.intValue();
        }

        if (statusCode instanceof String value && StringUtils.hasText(value)) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                return HttpStatus.INTERNAL_SERVER_ERROR.value();
            }
        }

        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    private static Throwable resolveThrowable(Throwable error, HttpServletRequest request) {
        if (error != null) {
            return error;
        }

        Object servletException = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (servletException instanceof Throwable throwable) {
            return throwable;
        }

        return null;
    }

    private static String resolveErrorTitle(HttpStatusCode status, HttpServletRequest request,
            Map<String, Object> model) {
        String modelError = stringValue(model.get("error"));

        if (hasUsefulText(modelError)) {
            return modelError;
        }

        HttpStatus httpStatus = HttpStatus.resolve(status.value());

        if (httpStatus != null) {
            return httpStatus.getReasonPhrase();
        }

        if (status.is4xxClientError()) {
            return "Client Error";
        }

        if (status.is5xxServerError()) {
            return DEFAULT_ERROR_TITLE;
        }

        return "HTTP Error";
    }

    private static String resolveDisplayMessage(HttpServletRequest request, Map<String, Object> model,
            Throwable throwable, String errorTitle) {
        String modelMessage = stringValue(model.get("message"));

        if (hasUsefulText(modelMessage)) {
            return modelMessage;
        }

        String servletMessage = stringValue(request.getAttribute(RequestDispatcher.ERROR_MESSAGE));

        if (hasUsefulText(servletMessage)) {
            return servletMessage;
        }

        if (throwable != null && StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage();
        }

        if (StringUtils.hasText(errorTitle)) {
            return errorTitle;
        }

        return DEFAULT_ERROR_MESSAGE;
    }

    private static String resolveRequestUri(HttpServletRequest request) {
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        if (uri instanceof String value && StringUtils.hasText(value)) {
            return value;
        }

        return request.getRequestURI();
    }

    private static String resolveExceptionType(Throwable error, Map<String, Object> model) {
        if (error != null) {
            return error.getClass().getName();
        }

        Object exception = model.get("exception");

        if (exception instanceof String value && StringUtils.hasText(value)) {
            return value;
        }

        return null;
    }

    private static String resolveTraceId() {
        String traceId = MDC.get("traceId");

        if (StringUtils.hasText(traceId)) {
            return traceId;
        }

        return null;
    }

    private static Map<String, String> collectHeaders(HttpServletRequest request) {
        Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames == null) {
            return headers;
        }

        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();

            if (isSensitiveHeader(name)) {
                headers.put(name, "[masked]");
                continue;
            }

            List<String> values = Collections.list(request.getHeaders(name));

            headers.put(name, String.join(", ", values));
        }

        return headers;
    }

    private static Map<String, String> collectParameters(HttpServletRequest request) {
        Map<String, String> parameters = new TreeMap<>();

        request.getParameterMap().forEach((name, values) -> {
            parameters.put(name, String.join(", ", values));
        });

        return parameters;
    }

    private static boolean isSensitiveHeader(String name) {
        if (!StringUtils.hasText(name)) {
            return false;
        }

        String normalizedName = name.toLowerCase();

        return SENSITIVE_HEADERS.contains(normalizedName);
    }

    private static boolean hasUsefulText(String value) {
        return StringUtils.hasText(value) && !"None".equalsIgnoreCase(value)
                && !"No message available".equalsIgnoreCase(value);
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }

        return value.toString();
    }

}
