package widget.config;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;

public final class AppConfig {

	private final Set<String> processes;
	private final String openaiBaseUrl;
	private final String openaiModel;
	private final int openaiTimeoutSeconds;
	private final double openaiTemperature;
	private final int openaiMaxTokens;
	private final int maxQuestionLength;
	private final int maxHistoryTurns;
	private final int rateLimitPerUser;
	private final int rateLimitPerIp;
	private final boolean trustForwardedFor;
	private final long maxRequestSizeBytes;

	private AppConfig(Properties p) {
		this.processes = PropertiesLoader.list(p, "knowledge.processes");
		if (processes.isEmpty()) {
			throw new ConfigException("knowledge.processes deve listar ao menos um processId");
		}
		this.openaiBaseUrl = PropertiesLoader.required(p, "openai.baseUrl").replaceAll("/+$", "");
		this.openaiModel = PropertiesLoader.required(p, "openai.model");
		this.openaiTimeoutSeconds = PropertiesLoader.intValue(p, "openai.timeoutSeconds", 25);
		this.openaiTemperature = PropertiesLoader.doubleValue(p, "openai.temperature", 0.15);
		this.openaiMaxTokens = PropertiesLoader.intValue(p, "openai.maxTokens", 2048);
		this.maxQuestionLength = PropertiesLoader.intValue(p, "chat.maxQuestionLength", 2000);
		this.maxHistoryTurns = PropertiesLoader.intValue(p, "chat.maxHistoryTurns", 6);
		this.rateLimitPerUser = PropertiesLoader.intValue(p, "security.rateLimit.requestsPerMinutePerUser", 20);
		this.rateLimitPerIp = PropertiesLoader.intValue(p, "security.rateLimit.requestsPerMinutePerIp", 60);
		this.trustForwardedFor = PropertiesLoader.boolValue(p, "security.rateLimit.trustForwardedFor", true);
		int maxMb = PropertiesLoader.intValue(p, "security.maxRequestSizeMb", 1);
		this.maxRequestSizeBytes = Math.max(1, maxMb) * 1024L * 1024L;
	}

	public static AppConfig from(Properties properties) {
		return new AppConfig(properties);
	}

	/**
	 * Carrega app.properties e, se knowledge.local.properties existir no classpath,
	 * substitui apenas knowledge.processes. Usado em producao e nos testes do merge.
	 */
	public static Properties loadMergedProperties(Properties base, Properties localOverlay) {
		Properties merged = new Properties();
		merged.putAll(base);
		if (localOverlay != null) {
			String processes = PropertiesLoader.optional(localOverlay, "knowledge.processes");
			if (processes != null) {
				merged.setProperty("knowledge.processes", processes);
			}
		}
		return merged;
	}

	public static AppConfig get() {
		return Holder.INSTANCE;
	}

	public Set<String> getProcesses() {
		return Collections.unmodifiableSet(processes);
	}

	public String getOpenaiBaseUrl() {
		return openaiBaseUrl;
	}

	public String getOpenaiModel() {
		return openaiModel;
	}

	public int getOpenaiTimeoutSeconds() {
		return openaiTimeoutSeconds;
	}

	public double getOpenaiTemperature() {
		return openaiTemperature;
	}

	public int getOpenaiMaxTokens() {
		return openaiMaxTokens;
	}

	public int getMaxQuestionLength() {
		return maxQuestionLength;
	}

	public int getMaxHistoryTurns() {
		return maxHistoryTurns;
	}

	public int getRateLimitPerUser() {
		return rateLimitPerUser;
	}

	public int getRateLimitPerIp() {
		return rateLimitPerIp;
	}

	public boolean isTrustForwardedFor() {
		return trustForwardedFor;
	}

	public long getMaxRequestSizeBytes() {
		return maxRequestSizeBytes;
	}

	private static final class Holder {
		private static final AppConfig INSTANCE = from(loadMergedProperties(
				PropertiesLoader.fromClasspath("app.properties"),
				PropertiesLoader.optionalFromClasspath("knowledge.local.properties")));
	}
}
