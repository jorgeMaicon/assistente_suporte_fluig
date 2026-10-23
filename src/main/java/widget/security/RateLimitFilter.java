package widget.security;

import javax.annotation.Priority;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import widget.config.AppConfig;
import widget.exception.ApiException;

@Provider
@Priority(Priorities.AUTHENTICATION + 10)
public class RateLimitFilter implements ContainerRequestFilter {

	public static final String USER_HEADER = "X-User-Code";

	private static final RateLimiter LIMITER = new RateLimiter();

	@Context
	private HttpServletRequest servletRequest;

	@Override
	public void filter(ContainerRequestContext request) {
		String path = request.getUriInfo().getPath();
		if (path != null && path.startsWith("health")) {
			return;
		}

		AppConfig config = AppConfig.get();
		long now = System.currentTimeMillis();

		if (!LIMITER.tryAcquire("ip:" + clientIp(config), config.getRateLimitPerIp(), now)) {
			throw ApiException.tooManyRequests("Muitas requisicoes. Aguarde um minuto e tente novamente.");
		}

		String user = request.getHeaderString(USER_HEADER);
		if (user != null && !user.trim().isEmpty()
				&& !LIMITER.tryAcquire("user:" + user.trim().toLowerCase(), config.getRateLimitPerUser(), now)) {
			throw ApiException.tooManyRequests("Muitas requisicoes para este usuario. Aguarde um minuto.");
		}
	}

	private String clientIp(AppConfig config) {
		if (config.isTrustForwardedFor()) {
			String forwarded = servletRequest.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.trim().isEmpty()) {
				return forwarded.split(",")[0].trim();
			}
		}
		return servletRequest.getRemoteAddr();
	}
}
