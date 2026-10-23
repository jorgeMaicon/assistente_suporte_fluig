package widget.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

	@Override
	public Response toResponse(ApiException exception) {
		return ErrorResponses.json(exception.getStatus(), exception.getMessage());
	}
}
