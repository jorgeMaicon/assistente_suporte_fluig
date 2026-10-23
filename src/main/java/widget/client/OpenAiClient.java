package widget.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import widget.config.AppConfig;
import widget.config.Credentials;
import widget.exception.ApiException;
import widget.util.SimpleJson;

public final class OpenAiClient {

	private static final Logger LOG = Logger.getLogger(OpenAiClient.class.getName());

	private final AppConfig config;
	private final Credentials credentials;

	public OpenAiClient(AppConfig config, Credentials credentials) {
		this.config = config;
		this.credentials = credentials;
	}

	public String chat(List<Map<String, String>> messages) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", config.getOpenaiModel());
		body.put("temperature", config.getOpenaiTemperature());
		body.put("max_tokens", Integer.valueOf(config.getOpenaiMaxTokens()));
		body.put("messages", messages);

		String payload = SimpleJson.object(body);
		HttpResult result = request("POST", config.getOpenaiBaseUrl() + "/v1/chat/completions", payload,
				config.getOpenaiTimeoutSeconds() * 1000);

		if (result.status < 200 || result.status >= 300) {
			LOG.warning("OpenAI chat HTTP " + result.status + ": " + truncate(result.body, 500));
			throw ApiException.badGateway("Falha ao consultar a OpenAI (HTTP " + result.status + ").");
		}

		String content = SimpleJson.extractChatContent(result.body);
		if (content == null || content.trim().isEmpty()) {
			throw ApiException.badGateway("Resposta da OpenAI sem content.");
		}
		return content.trim();
	}

	/** Probe leve (GET /v1/models) para o /rest/health. Nao expoe a chave. */
	public Map<String, Object> probeReachability() {
		Map<String, Object> result = new LinkedHashMap<>();
		try {
			HttpResult http = request("GET", config.getOpenaiBaseUrl() + "/v1/models", null,
					Math.min(10_000, config.getOpenaiTimeoutSeconds() * 1000));
			result.put("reachable", Boolean.valueOf(http.status >= 200 && http.status < 300));
			result.put("httpStatus", Integer.valueOf(http.status));
			if (http.status < 200 || http.status >= 300) {
				result.put("detail", "OpenAI respondeu HTTP " + http.status);
			}
		} catch (ApiException e) {
			result.put("reachable", Boolean.FALSE);
			result.put("detail", e.getMessage());
		} catch (Exception e) {
			LOG.log(Level.INFO, "Probe OpenAI falhou: " + e.getMessage());
			result.put("reachable", Boolean.FALSE);
			result.put("detail", "network_error: " + e.getClass().getSimpleName());
		}
		return result;
	}

	private HttpResult request(String method, String url, String jsonBody, int timeoutMs) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestMethod(method);
			conn.setConnectTimeout(Math.min(10_000, timeoutMs));
			conn.setReadTimeout(timeoutMs);
			conn.setRequestProperty("Authorization", "Bearer " + credentials.getOpenaiApiKey());
			conn.setRequestProperty("Accept", "application/json");
			if (jsonBody != null) {
				byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
				conn.setDoOutput(true);
				conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
				conn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
				try (OutputStream out = conn.getOutputStream()) {
					out.write(bytes);
				}
			}

			int status = conn.getResponseCode();
			InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
			String body = stream == null ? "" : readUtf8(stream);
			return new HttpResult(status, body);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Erro de rede ao chamar OpenAI", e);
			throw ApiException.badGateway(
					"Nao foi possivel alcancar a OpenAI a partir deste servidor. Verifique egress no Fluig Cloud.");
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	private static String readUtf8(InputStream in) throws IOException {
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		byte[] chunk = new byte[4096];
		int n;
		while ((n = in.read(chunk)) >= 0) {
			buf.write(chunk, 0, n);
		}
		return new String(buf.toByteArray(), StandardCharsets.UTF_8);
	}

	private static String truncate(String value, int max) {
		if (value == null) {
			return "";
		}
		return value.length() <= max ? value : value.substring(0, max);
	}

	private static final class HttpResult {
		final int status;
		final String body;

		HttpResult(int status, String body) {
			this.status = status;
			this.body = body;
		}
	}
}
