package widget.prompt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import widget.config.AppConfig;
import widget.exception.ApiException;
import widget.knowledge.ProcessKnowledge;
import widget.model.HistoryMessage;

public final class PromptBuilder {

	private final AppConfig config;

	public PromptBuilder(AppConfig config) {
		this.config = config;
	}

	public List<Map<String, String>> build(ProcessKnowledge knowledge, String pergunta,
			List<HistoryMessage> historico) {
		String question = normalizeQuestion(pergunta);
		List<Map<String, String>> messages = new ArrayList<>();
		messages.add(message("system", systemPrompt(knowledge)));
		messages.add(message("system", "DOCUMENTACAO DO PROCESSO (" + knowledge.getProcessId() + "):\n\n"
				+ knowledge.getMarkdown()));

		List<HistoryMessage> turns = trimHistory(historico);
		for (HistoryMessage turn : turns) {
			String role = normalizeRole(turn.getRole());
			String content = trimTo(turn.getContent(), config.getMaxQuestionLength());
			if (content == null || content.isEmpty()) {
				continue;
			}
			messages.add(message(role, content));
		}

		messages.add(message("user", question));
		return messages;
	}

	private String systemPrompt(ProcessKnowledge knowledge) {
		StringBuilder sb = new StringBuilder();
		sb.append("Voce e um assistente de suporte tecnico do Fluig/TOTVS para o processo ")
				.append(knowledge.getProcessId()).append(".\n");
		sb.append("Responda em portugues do Brasil, de forma objetiva e util para quem diagnostica problemas.\n");
		sb.append("Use APENAS o documento de contexto fornecido. Se a resposta nao estiver no documento, diga claramente que o documento nao cobre o assunto.\n");
		sb.append("Ao citar regras, mencione o titulo da secao (ex.: \"## 1. Visao\").\n");
		sb.append("Nao invente IDs de atividade, nomes de campo, datasets ou scripts.\n");
		sb.append("Se o codigo atual divergir do documento, o codigo e a fonte da verdade; avise essa limitacao.\n");
		if (knowledge.getLastAnalyzed() != null) {
			sb.append("Data da ultima analise do documento: ").append(knowledge.getLastAnalyzed()).append(".\n");
		}
		sb.append("Hash do contexto carregado: ").append(knowledge.getHash()).append(".\n");
		return sb.toString();
	}

	private String normalizeQuestion(String pergunta) {
		if (pergunta == null || pergunta.trim().isEmpty()) {
			throw ApiException.badRequest("Campo pergunta e obrigatorio.");
		}
		String trimmed = pergunta.trim();
		if (trimmed.length() > config.getMaxQuestionLength()) {
			throw ApiException.badRequest(
					"Pergunta excede o limite de " + config.getMaxQuestionLength() + " caracteres.");
		}
		return trimmed;
	}

	private List<HistoryMessage> trimHistory(List<HistoryMessage> historico) {
		if (historico == null || historico.isEmpty()) {
			return Collections.emptyList();
		}
		int max = config.getMaxHistoryTurns() * 2;
		if (historico.size() <= max) {
			return historico;
		}
		return historico.subList(historico.size() - max, historico.size());
	}

	private static String normalizeRole(String role) {
		if (role == null) {
			return "user";
		}
		String normalized = role.trim().toLowerCase(Locale.ROOT);
		if ("assistant".equals(normalized) || "system".equals(normalized)) {
			return normalized;
		}
		return "user";
	}

	private static String trimTo(String value, int max) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() <= max) {
			return trimmed;
		}
		return trimmed.substring(0, max);
	}

	private static Map<String, String> message(String role, String content) {
		Map<String, String> map = new LinkedHashMap<>();
		map.put("role", role);
		map.put("content", content);
		return map;
	}
}
