package widget.security;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter("/*")
public class SecurityHeadersFilter implements Filter {

	private static final String CSP = "default-src 'self'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'";

	@Override
	public void init(FilterConfig filterConfig) {
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;

		response.setHeader("X-Content-Type-Options", "nosniff");
		response.setHeader("X-Frame-Options", "DENY");
		response.setHeader("Referrer-Policy", "no-referrer");
		response.setHeader("Content-Security-Policy", CSP);

		String path = request.getRequestURI().substring(request.getContextPath().length());
		if (path.startsWith("/rest/")) {
			response.setHeader("Cache-Control", "no-store");
		}
		chain.doFilter(req, res);
	}

	@Override
	public void destroy() {
	}
}
