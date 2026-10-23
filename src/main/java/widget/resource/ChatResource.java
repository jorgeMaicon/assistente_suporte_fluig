package widget.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import widget.Services;
import widget.exception.ApiException;
import widget.model.ChatRequest;
import widget.model.HistoryMessage;
import widget.util.SimpleJson;

@Path("chat")
@Produces(MediaType.APPLICATION_JSON)
public class ChatResource {

	/**
	 * Aceita o corpo como String para nao depender de JSON-B/Jackson no widget Fluig
	 * (fonte frequente de HTTP 500 no deserialize do POJO).
	 */
	@POST
	@Consumes({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN, MediaType.WILDCARD })
	public Map<String, Object> chat(String rawBody) {
		return Services.chat().chat(parse(rawBody));
	}

	private static ChatRequest parse(String rawBody) {
		if (rawBody == null || rawBody.trim().isEmpty()) {
			throw ApiException.badRequest("Corpo JSON ausente.");
		}
		try {
			Map<String, Object> map = SimpleJson.parseObject(rawBody);
			ChatRequest request = new ChatRequest();
			Object processo = map.get("processo");
			Object pergunta = map.get("pergunta");
			request.setProcesso(processo == null ? null : String.valueOf(processo));
			request.setPergunta(pergunta == null ? null : String.valueOf(pergunta));

			List<Map<String, String>> historicoMaps = SimpleJson.parseHistory(map.get("historico"));
			if (!historicoMaps.isEmpty()) {
				List<HistoryMessage> historico = new ArrayList<>();
				for (Map<String, String> item : historicoMaps) {
					historico.add(new HistoryMessage(item.get("role"), item.get("content")));
				}
				request.setHistorico(historico);
			}
			return request;
		} catch (ApiException e) {
			throw e;
		} catch (Exception e) {
			throw ApiException.badRequest("JSON invalido: " + e.getMessage());
		}
	}
}
