package widget.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SimpleJsonTest {

	@Test
	void montaPayloadChat() {
		List<Map<String, String>> messages = new ArrayList<>();
		Map<String, String> m = new LinkedHashMap<>();
		m.put("role", "user");
		m.put("content", "ola \"mundo\"");
		messages.add(m);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", "gpt-4o-mini");
		body.put("temperature", Double.valueOf(0.15));
		body.put("messages", messages);

		String json = SimpleJson.object(body);
		assertTrue(json.contains("\"model\":\"gpt-4o-mini\""));
		assertTrue(json.contains("ola \\\"mundo\\\""));
	}

	@Test
	void extraiContentDaOpenAi() {
		String body = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"Atividade 26.\"}}]}";
		assertEquals("Atividade 26.", SimpleJson.extractChatContent(body));
	}

	@Test
	void parseObjectSimples() {
		Map<String, Object> map = SimpleJson.parseObject("{\"processo\":\"gestao_reparaveis\",\"pergunta\":\"x\"}");
		assertEquals("gestao_reparaveis", map.get("processo"));
		assertEquals("x", map.get("pergunta"));
	}
}
