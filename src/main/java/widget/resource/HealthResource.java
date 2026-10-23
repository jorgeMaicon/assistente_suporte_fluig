package widget.resource;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import widget.Services;

/**
 * Confirma que o WAR subiu, lista processos/hashes e, com ?probeOpenAi=true,
 * tenta alcancar a OpenAI (egress do Fluig Cloud).
 */
@Path("health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

	@GET
	public Map<String, Object> health(@QueryParam("probeOpenAi") Boolean probeOpenAi) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", "ok");
		body.put("environment", Services.credentials().getEnvironment());
		body.put("processes", Services.knowledge().hashesByProcess());
		body.put("model", Services.config().getOpenaiModel());

		if (Boolean.TRUE.equals(probeOpenAi)) {
			Map<String, Object> probe = Services.openAi().probeReachability();
			body.put("openai", probe);
			Object reachable = probe.get("reachable");
			if (!(reachable instanceof Boolean) || !((Boolean) reachable)) {
				body.put("status", "degraded");
				body.put("egressHint",
						"Se reachable=false, cadastre um servico Fluig OPENAI e mova o HTTP para o dataset; o WAR continua dono do prompt.");
			}
		}
		return body;
	}
}
