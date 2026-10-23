package widget.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import widget.client.OpenAiClient;
import widget.knowledge.KnowledgeCatalog;
import widget.knowledge.ProcessKnowledge;
import widget.model.ChatRequest;
import widget.model.HistoryMessage;
import widget.prompt.PromptBuilder;

public final class ChatService {

	private final KnowledgeCatalog catalog;
	private final PromptBuilder promptBuilder;
	private final OpenAiClient openAiClient;

	public ChatService(KnowledgeCatalog catalog, PromptBuilder promptBuilder, OpenAiClient openAiClient) {
		this.catalog = catalog;
		this.promptBuilder = promptBuilder;
		this.openAiClient = openAiClient;
	}

	public Map<String, Object> chat(ChatRequest request) {
		ProcessKnowledge knowledge = catalog.require(request == null ? null : request.getProcesso());
		List<HistoryMessage> historico = request.getHistorico();
		List<Map<String, String>> messages = promptBuilder.build(knowledge, request.getPergunta(), historico);
		String answer = openAiClient.chat(messages);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "ok");
		body.put("processo", knowledge.getProcessId());
		body.put("contexto", knowledge.getHash());
		body.put("resposta", answer);
		if (knowledge.getLastAnalyzed() != null) {
			body.put("ultimaAnalise", knowledge.getLastAnalyzed());
		}
		return body;
	}
}
