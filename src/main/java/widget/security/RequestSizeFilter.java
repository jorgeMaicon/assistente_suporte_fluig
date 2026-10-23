package widget.security;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import widget.config.AppConfig;
import widget.exception.ApiException;

@Provider
@Priority(Priorities.AUTHENTICATION - 10)
public class RequestSizeFilter implements ContainerRequestFilter {

	@Override
	public void filter(ContainerRequestContext request) {
		long max = AppConfig.get().getMaxRequestSizeBytes();
		if (request.getLength() > max) {
			throw ApiException.payloadTooLarge("Requisicao excede o tamanho maximo permitido.");
		}
		if (request.hasEntity()) {
			request.setEntityStream(new LimitedInputStream(request.getEntityStream(), max));
		}
	}

	static final class LimitedInputStream extends FilterInputStream {
		private final long max;
		private long read;

		LimitedInputStream(InputStream in, long max) {
			super(in);
			this.max = max;
		}

		@Override
		public int read() throws IOException {
			int b = super.read();
			if (b >= 0) {
				count(1);
			}
			return b;
		}

		@Override
		public int read(byte[] buffer, int off, int len) throws IOException {
			int n = super.read(buffer, off, len);
			if (n > 0) {
				count(n);
			}
			return n;
		}

		private void count(long n) throws IOException {
			read += n;
			if (read > max) {
				throw new IOException("Requisicao excede o tamanho maximo permitido");
			}
		}
	}
}
