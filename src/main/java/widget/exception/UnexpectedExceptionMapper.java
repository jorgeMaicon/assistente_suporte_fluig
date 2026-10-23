package widget.exception;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import widget.config.ConfigException;

@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

	private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class.getName());

	@Override
	public Response toResponse(Throwable exception) {
		if (exception instanceof WebApplicationException) {
			int status = ((WebApplicationException) exception).getResponse().getStatus();
			if (status < 500) {
				return ErrorResponses.json(status, status == 404 ? "Recurso nao encontrado." : "Requisicao invalida.");
			}
		}

		String ref = UUID.randomUUID().toString().substring(0, 8);
		LOG.log(Level.SEVERE, "Erro inesperado [ref=" + ref + "] "
				+ exception.getClass().getName() + ": " + exception.getMessage(), exception);

		if (exception instanceof ConfigException) {
			return ErrorResponses.json(500, "Servico temporariamente indisponivel. (ref: " + ref + ")");
		}
		return ErrorResponses.json(500,
				"Erro interno. (ref: " + ref + ", tipo: " + exception.getClass().getSimpleName() + ")");
	}
}
