package widget.config;

import java.util.Properties;

public final class Credentials {

	private final String environment;
	private final String openaiApiKey;
	private final String internalToken;

	private Credentials(Properties p) {
		this.environment = value(p, "app.environment");
		this.openaiApiKey = value(p, "openai.apiKey");
		this.internalToken = value(p, "api.internalToken");
		if (internalToken.length() < 16) {
			throw new ConfigException("api.internalToken deve ter pelo menos 16 caracteres");
		}
	}

	public static Credentials from(Properties properties) {
		return new Credentials(properties);
	}

	public static Credentials get() {
		return Holder.INSTANCE;
	}

	private static String value(Properties p, String key) {
		String value = PropertiesLoader.required(p, key);
		if (value.startsWith("${")) {
			throw new ConfigException(key
					+ " nao foi preenchido no build. Gere o WAR com -Phomolog ou -Pprod (veja README).");
		}
		return value;
	}

	public String getEnvironment() {
		return environment;
	}

	public String getOpenaiApiKey() {
		return openaiApiKey;
	}

	public String getInternalToken() {
		return internalToken;
	}

	@Override
	public String toString() {
		return "Credentials[environment=" + environment + ", openaiApiKey=***, internalToken=***]";
	}

	private static final class Holder {
		private static final Credentials INSTANCE = from(PropertiesLoader.fromClasspath("credentials.properties"));
	}
}
