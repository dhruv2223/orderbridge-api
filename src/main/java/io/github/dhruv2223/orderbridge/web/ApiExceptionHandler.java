package io.github.dhruv2223.orderbridge.web;

import java.net.URI;
import java.util.List;

import io.github.dhruv2223.orderbridge.fx.FxUnavailableException;
import io.github.dhruv2223.orderbridge.orders.OrderRejectedException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps exceptions to RFC 9457 {@link ProblemDetail} responses. They are rendered as
 * {@code application/problem+json} or {@code application/problem+xml} depending on the
 * client's {@code Accept} header.
 */
@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	private static final String TYPE_BASE = "https://orderbridge.example.com/problems/";

	@ExceptionHandler(NotFoundException.class)
	ResponseEntity<Object> notFound(NotFoundException ex, WebRequest request) {
		return respond(problem(HttpStatus.NOT_FOUND, "not-found", "Resource not found", ex.getMessage()), request);
	}

	@ExceptionHandler(BadRequestException.class)
	ResponseEntity<Object> badRequest(BadRequestException ex, WebRequest request) {
		return respond(problem(HttpStatus.BAD_REQUEST, "bad-request", "Bad request", ex.getMessage()), request);
	}

	@ExceptionHandler(ConflictException.class)
	ResponseEntity<Object> conflict(ConflictException ex, WebRequest request) {
		return respond(problem(HttpStatus.CONFLICT, "conflict", "Conflict", ex.getMessage()), request);
	}

	@ExceptionHandler(DuplicateKeyException.class)
	ResponseEntity<Object> duplicate(DuplicateKeyException ex, WebRequest request) {
		return respond(problem(HttpStatus.CONFLICT, "conflict", "Conflict",
				"A resource with the same key already exists"), request);
	}

	@ExceptionHandler(OptimisticLockingFailureException.class)
	ResponseEntity<Object> concurrentUpdate(OptimisticLockingFailureException ex, WebRequest request) {
		return respond(problem(HttpStatus.CONFLICT, "concurrent-update", "Concurrent update",
				"The resource was modified by another request; retry with fresh data"), request);
	}

	@ExceptionHandler(OrderRejectedException.class)
	ResponseEntity<Object> orderRejected(OrderRejectedException ex, WebRequest request) {
		var problem = problem(HttpStatus.UNPROCESSABLE_CONTENT, "order-rejected", "Order rejected", ex.getMessage());
		problem.setProperty("violations", ex.getViolations());
		return respond(problem, request);
	}

	@ExceptionHandler(FxUnavailableException.class)
	ResponseEntity<Object> fxUnavailable(FxUnavailableException ex, WebRequest request) {
		return respond(problem(HttpStatus.SERVICE_UNAVAILABLE, "fx-unavailable", "Exchange rates unavailable",
				ex.getMessage()), request);
	}

	/** Bean Validation failures on request bodies: list every offending field. */
	@Override
	protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		var errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(e -> new FieldViolation(e.getField(), e.getDefaultMessage()))
			.toList();
		var problem = validationProblem(errors);
		return handleExceptionInternal(ex, problem, headers, status, request);
	}

	/** Bean Validation failures on query/path parameters. */
	@Override
	protected @Nullable ResponseEntity<Object> handleHandlerMethodValidationException(
			HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldViolation> errors = ex.getParameterValidationResults()
			.stream()
			.flatMap(r -> r.getResolvableErrors()
				.stream()
				.map(e -> new FieldViolation(r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
			.toList();
		return handleExceptionInternal(ex, validationProblem(errors), headers, HttpStatus.BAD_REQUEST, request);
	}

	/**
	 * Spring only matches {@code application/problem+xml} when a client explicitly asks for
	 * it. Clients that send {@code Accept: application/xml} (or {@code ?format=xml}) expect
	 * errors in XML too, so pick the problem media type from their preference.
	 */
	@Override
	protected ResponseEntity<Object> createResponseEntity(@Nullable Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		if (body instanceof ProblemDetail && headers.getContentType() == null) {
			var withType = new HttpHeaders();
			withType.putAll(headers);
			withType.setContentType(prefersXml(request) ? MediaType.APPLICATION_PROBLEM_XML
					: MediaType.APPLICATION_PROBLEM_JSON);
			headers = withType;
		}
		return super.createResponseEntity(body, headers, statusCode, request);
	}

	private ResponseEntity<Object> respond(ProblemDetail problem, WebRequest request) {
		return createResponseEntity(problem, new HttpHeaders(), HttpStatusCode.valueOf(problem.getStatus()), request);
	}

	static boolean prefersXml(WebRequest request) {
		var format = request.getParameter("format");
		if (format != null) {
			return format.equalsIgnoreCase("xml");
		}
		var accept = request.getHeader(HttpHeaders.ACCEPT);
		if (accept == null || accept.isBlank()) {
			return false;
		}
		try {
			var types = MediaType.parseMediaTypes(accept);
			MimeTypeUtils.sortBySpecificity(types);
			for (var type : types) {
				if (type.getQualityValue() == 0) {
					continue;
				}
				var subtype = type.getSubtype();
				if (subtype.equals("xml") || subtype.endsWith("+xml")) {
					return true;
				}
				if (subtype.equals("json") || subtype.endsWith("+json") || type.isWildcardType()) {
					return false;
				}
			}
		}
		catch (InvalidMediaTypeException ex) {
			// fall through to JSON
		}
		return false;
	}

	private ProblemDetail validationProblem(List<FieldViolation> errors) {
		var problem = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed",
				"The request contains invalid fields");
		problem.setProperty("errors", errors);
		return problem;
	}

	private static ProblemDetail problem(HttpStatus status, String type, String title, String detail) {
		var problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setType(URI.create(TYPE_BASE + type));
		problem.setTitle(title);
		return problem;
	}

	record FieldViolation(String field, String message) {
	}

}
