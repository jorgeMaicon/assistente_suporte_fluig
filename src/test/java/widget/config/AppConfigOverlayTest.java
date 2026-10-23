package widget.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Properties;

import org.junit.jupiter.api.Test;

class AppConfigOverlayTest {

	@Test
	void overlaySubstituiSomenteKnowledgeProcesses() {
		Properties base = new Properties();
		base.setProperty("knowledge.processes", "exemplo");
		base.setProperty("openai.baseUrl", "https://api.openai.com");
		base.setProperty("openai.model", "gpt-4o-mini");

		Properties overlay = new Properties();
		overlay.setProperty("knowledge.processes", "meu_processo");
		overlay.setProperty("openai.model", "nao-deve-passar");

		Properties merged = AppConfig.loadMergedProperties(base, overlay);
		assertEquals("meu_processo", merged.getProperty("knowledge.processes"));
		assertEquals("gpt-4o-mini", merged.getProperty("openai.model"));
		assertEquals("https://api.openai.com", merged.getProperty("openai.baseUrl"));
	}

	@Test
	void semOverlayMantemBase() {
		Properties base = new Properties();
		base.setProperty("knowledge.processes", "exemplo");
		base.setProperty("openai.baseUrl", "https://api.openai.com");
		base.setProperty("openai.model", "gpt-4o-mini");

		Properties merged = AppConfig.loadMergedProperties(base, null);
		assertEquals("exemplo", merged.getProperty("knowledge.processes"));

		AppConfig config = AppConfig.from(merged);
		assertTrue(config.getProcesses().contains("exemplo"));
	}
}
