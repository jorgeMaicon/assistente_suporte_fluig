package widget.knowledge;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import widget.config.AppConfig;
import widget.config.ConfigException;
import widget.exception.ApiException;

public final class KnowledgeCatalog {

	private static final Logger LOG = Logger.getLogger(KnowledgeCatalog.class.getName());
	private static final Pattern LAST_ANALYZED = Pattern.compile("(?im)^Ultima analise:\\s*(.+)$");

	private final Map<String, ProcessKnowledge> byProcess;

	public KnowledgeCatalog(AppConfig config) {
		Map<String, ProcessKnowledge> loaded = new LinkedHashMap<>();
		for (String processId : config.getProcesses()) {
			if (!processId.matches("[a-zA-Z0-9_\\-]+")) {
				throw new ConfigException("processId invalido em knowledge.processes: " + processId);
			}
			String resource = "knowledge/" + processId + ".md";
			String markdown = readClasspath(resource);
			if (markdown.trim().isEmpty()) {
				throw new ConfigException("Markdown vazio: " + resource);
			}
			String hash = sha256Hex(markdown);
			String lastAnalyzed = extractLastAnalyzed(markdown);
			loaded.put(processId, new ProcessKnowledge(processId, markdown, hash, lastAnalyzed));
			LOG.info("Conhecimento carregado: processo=" + processId + " hash=" + hash.substring(0, 12)
					+ " bytes=" + markdown.length());
		}
		this.byProcess = Collections.unmodifiableMap(loaded);
	}

	public static KnowledgeCatalog get() {
		return Holder.INSTANCE;
	}

	public ProcessKnowledge require(String processId) {
		if (processId == null || processId.trim().isEmpty()) {
			throw ApiException.badRequest("Campo processo e obrigatorio. Processos publicados: " + publishedList());
		}
		String key = processId.trim();
		ProcessKnowledge knowledge = byProcess.get(key);
		if (knowledge == null) {
			throw ApiException.notFound("Processo desconhecido: " + key + ". Processos publicados: " + publishedList());
		}
		return knowledge;
	}

	public Optional<ProcessKnowledge> find(String processId) {
		if (processId == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(byProcess.get(processId.trim()));
	}

	public Set<String> processIds() {
		return byProcess.keySet();
	}

	public Map<String, String> hashesByProcess() {
		return byProcess.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getHash(), (a, b) -> a,
						LinkedHashMap::new));
	}

	private String publishedList() {
		return String.join(", ", byProcess.keySet());
	}

	private static String readClasspath(String resource) {
		try (InputStream in = KnowledgeCatalog.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				throw new ConfigException("Arquivo de conhecimento nao encontrado: " + resource);
			}
			java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
			byte[] chunk = new byte[4096];
			int n;
			while ((n = in.read(chunk)) >= 0) {
				buf.write(chunk, 0, n);
			}
			return new String(buf.toByteArray(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new ConfigException("Falha ao ler " + resource, e);
		}
	}

	private static String extractLastAnalyzed(String markdown) {
		Matcher matcher = LAST_ANALYZED.matcher(markdown);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	private static String sha256Hex(String text) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			throw new ConfigException("SHA-256 indisponivel", e);
		}
	}

	private static final class Holder {
		private static final KnowledgeCatalog INSTANCE = new KnowledgeCatalog(AppConfig.get());
	}
}
