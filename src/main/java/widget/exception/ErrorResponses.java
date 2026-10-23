package widget.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

final class ErrorResponses {

	private ErrorResponses() {
	}

	static Response json(int status, String message) {
		Map<String, String> body = new LinkedHashMap<>();
		body.put("status", "error");
		body.put("message", message);
		return Response.status(status).entity(body).type(MediaType.APPLICATION_JSON_TYPE).build();
	}
}
