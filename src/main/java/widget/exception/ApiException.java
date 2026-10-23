package widget.exception;

public class ApiException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final int status;

	public ApiException(int status, String message) {
		super(message);
		this.status = status;
	}

	public static ApiException badRequest(String message) {
		return new ApiException(400, message);
	}

	public static ApiException unauthorized(String message) {
		return new ApiException(401, message);
	}

	public static ApiException forbidden(String message) {
		return new ApiException(403, message);
	}

	public static ApiException notFound(String message) {
		return new ApiException(404, message);
	}

	public static ApiException payloadTooLarge(String message) {
		return new ApiException(413, message);
	}

	public static ApiException tooManyRequests(String message) {
		return new ApiException(429, message);
	}

	public static ApiException badGateway(String message) {
		return new ApiException(502, message);
	}

	public int getStatus() {
		return status;
	}
}
