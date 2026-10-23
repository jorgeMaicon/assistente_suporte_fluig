package widget.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import widget.config.AppConfig;
import widget.exception.ApiException;

class KnowledgeCatalogTest {

	@Test
	void carregaExemploDoClasspath() {
		KnowledgeCatalog catalog = new KnowledgeCatalog(config("exemplo"));
		ProcessKnowledge knowledge = catalog.require("exemplo");
		assertEquals("exemplo", knowledge.getProcessId());
		assertTrue(knowledge.getMarkdown().contains("exemplo"));
		assertEquals(64, knowledge.getHash().length());
	}

	@Test
	void processoDesconhecidoListaPublicados() {
		KnowledgeCatalog catalog = new KnowledgeCatalog(config("exemplo"));
		ApiException ex = assertThrows(ApiException.class, () -> catalog.require("nao_existe"));
		assertEquals(404, ex.getStatus());
		assertTrue(ex.getMessage().contains("exemplo"));
	}

	private static AppConfig config(String processes) {
		Properties p = new Properties();
		p.setProperty("knowledge.processes", processes);
		p.setProperty("openai.baseUrl", "https://api.openai.com");
		p.setProperty("openai.model", "gpt-4o-mini");
		return AppConfig.from(p);
	}
}
