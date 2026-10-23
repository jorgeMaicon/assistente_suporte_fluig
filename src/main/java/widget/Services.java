package widget;

import widget.client.OpenAiClient;
import widget.config.AppConfig;
import widget.config.Credentials;
import widget.knowledge.KnowledgeCatalog;
import widget.prompt.PromptBuilder;
import widget.service.ChatService;

/** Singletons da aplicacao, criados na primeira requisicao (o Fluig nao oferece CDI neste contexto). */
public final class Services {

	private Services() {
	}

	public static ChatService chat() {
		return Holder.CHAT;
	}

	public static KnowledgeCatalog knowledge() {
		return Holder.CATALOG;
	}

	public static OpenAiClient openAi() {
		return Holder.OPENAI;
	}

	public static AppConfig config() {
		return Holder.CONFIG;
	}

	public static Credentials credentials() {
		return Holder.CREDENTIALS;
	}

	private static final class Holder {
		private static final AppConfig CONFIG = AppConfig.get();
		private static final Credentials CREDENTIALS = Credentials.get();
		private static final KnowledgeCatalog CATALOG = KnowledgeCatalog.get();
		private static final PromptBuilder PROMPT = new PromptBuilder(CONFIG);
		private static final OpenAiClient OPENAI = new OpenAiClient(CONFIG, CREDENTIALS);
		private static final ChatService CHAT = new ChatService(CATALOG, PROMPT, OPENAI);
	}
}
