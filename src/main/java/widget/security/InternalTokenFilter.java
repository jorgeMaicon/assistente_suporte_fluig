package widget.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import widget.config.Credentials;
import widget.exception.ApiException;

/**
 * Exige o header X-Internal-Token nas rotas de chat. O health fica publico
 * para checagem de deploy e egress.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class InternalTokenFilter implements ContainerRequestFilter {

	public static final String HEADER = "X-Internal-Token";

	@Override
	public void filter(ContainerRequestContext request) {
		String path = request.getUriInfo().getPath();
		if (path == null || path.startsWith("health")) {
			return;
		}

		String provided = request.getHeaderString(HEADER);
		String expected = Credentials.get().getInternalToken();
		if (provided == null || provided.isEmpty() || !constantTimeEquals(provided, expected)) {
			throw ApiException.unauthorized("Token interno ausente ou invalido.");
		}
	}

	private static boolean constantTimeEquals(String a, String b) {
		byte[] left = a.getBytes(StandardCharsets.UTF_8);
		byte[] right = b.getBytes(StandardCharsets.UTF_8);
		if (left.length != right.length) {
			return false;
		}
		return MessageDigest.isEqual(left, right);
	}
}
