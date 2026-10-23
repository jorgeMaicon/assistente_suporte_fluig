package widget.prompt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import widget.config.AppConfig;
import widget.exception.ApiException;
import widget.knowledge.ProcessKnowledge;
import widget.model.HistoryMessage;

class PromptBuilderTest {

	@Test
	void montaSystemMarkdownHistoricoEPergunta() {
		PromptBuilder builder = new PromptBuilder(config());
		ProcessKnowledge knowledge = new ProcessKnowledge("exemplo", "# Doc\n## 1. Visao\ntexto",
				"abc123", "2026-10-01");

		List<HistoryMessage> historico = new ArrayList<>();
		historico.add(new HistoryMessage("user", "pergunta anterior"));
		historico.add(new HistoryMessage("assistant", "resposta anterior"));

		List<Map<String, String>> messages = builder.build(knowledge, "Qual atividade analisa a solicitacao?", historico);

		assertEquals("system", messages.get(0).get("role"));
		assertTrue(messages.get(0).get("content").contains("exemplo"));
		assertTrue(messages.get(0).get("content").contains("2026-10-01"));
		assertTrue(messages.get(0).get("content").contains("## 1. Visao"));
		assertTrue(messages.get(1).get("content").contains("## 1. Visao"));
		assertEquals("user", messages.get(2).get("role"));
		assertEquals("assistant", messages.get(3).get("role"));
		assertEquals("Qual atividade analisa a solicitacao?", messages.get(4).get("content"));
	}

	@Test
	void rejeitaPerguntaVazia() {
		PromptBuilder builder = new PromptBuilder(config());
		ProcessKnowledge knowledge = new ProcessKnowledge("exemplo", "doc", "h", null);
		assertThrows(ApiException.class, () -> builder.build(knowledge, "   ", null));
	}

	private static AppConfig config() {
		Properties p = new Properties();
		p.setProperty("knowledge.processes", "exemplo");
		p.setProperty("openai.baseUrl", "https://api.openai.com");
		p.setProperty("openai.model", "gpt-4o-mini");
		p.setProperty("openai.timeoutSeconds", "25");
		p.setProperty("openai.temperature", "0.15");
		p.setProperty("openai.maxTokens", "2048");
		p.setProperty("chat.maxQuestionLength", "2000");
		p.setProperty("chat.maxHistoryTurns", "6");
		p.setProperty("security.rateLimit.requestsPerMinutePerUser", "20");
		p.setProperty("security.rateLimit.requestsPerMinutePerIp", "60");
		p.setProperty("security.rateLimit.trustForwardedFor", "true");
		p.setProperty("security.maxRequestSizeMb", "1");
		return AppConfig.from(p);
	}
}
