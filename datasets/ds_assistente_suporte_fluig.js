/**
 * Porta Fluig do assistente_suporte_fluig (chama o WAR via servico REST autorizado).
 *
 * Constraints:
 *   PROCESSO  (obrigatorio) - processId, ex.: exemplo
 *   PERGUNTA  (obrigatorio) - texto da pergunta
 *   HISTORICO (opcional)    - JSON array [{role, content}, ...]
 *
 * Pre-requisitos no Fluig:
 *   1. Widget assistente_suporte_fluig publicado
 *   2. Servico REST ASSISTENTE_SUPORTE_FLUIG apontando para
 *      https://<servidor>/assistente_suporte_fluig
 *   3. INTERNAL_TOKEN abaixo igual ao api.internalToken do build do WAR
 *
 * Publique este script como dataset no Fluig (mesmo codigo do nome do arquivo,
 * sem a extensao .js) e configure INTERNAL_TOKEN antes de usar.
 *
 * Teste no console (usuario no papel admin):
 *   var c1 = DatasetFactory.createConstraint("PROCESSO", "exemplo", "exemplo", ConstraintType.MUST);
 *   var c2 = DatasetFactory.createConstraint("PERGUNTA", "Qual atividade analisa a solicitacao?", "Qual atividade analisa a solicitacao?", ConstraintType.MUST);
 *   var ds = DatasetFactory.getDataset("ds_assistente_suporte_fluig", null, [c1, c2], null);
 *   console.log(ds.values);
 */
function createDataset(fields, constraints, sortFields) {

	var dataset = DatasetBuilder.newDataset();
	dataset.addColumn("RESPOSTA");
	dataset.addColumn("PROCESSO");
	dataset.addColumn("CONTEXTO");
	dataset.addColumn("ERRO");

	var MAX_PERGUNTA = 2000;

	// Mesmo valor de api.internalToken do build do WAR (settings.xml / -Dapi.internalToken).
	// NAO use a chave da OpenAI aqui — e so o segredo dataset <-> WAR.
	var INTERNAL_TOKEN = "CONFIGURE_MESMO_TOKEN_DO_WAR";

	try {
		var processo = "";
		var pergunta = "";
		var historicoJson = "";

		if (constraints != null) {
			for (var i = 0; i < constraints.length; i++) {
				var nome = asJs(constraints[i].getFieldName());
				var valor = asJs(constraints[i].getInitialValue());
				if (nome == "PROCESSO") {
					processo = valor;
				} else if (nome == "PERGUNTA") {
					pergunta = valor;
				} else if (nome == "HISTORICO") {
					historicoJson = valor;
				}
			}
		}

		if (!processo) {
			return linhaErro(dataset, "Informe a constraint PROCESSO.");
		}
		if (!pergunta) {
			return linhaErro(dataset, "Informe a constraint PERGUNTA.");
		}
		if (pergunta.length > MAX_PERGUNTA) {
			return linhaErro(dataset, "Pergunta excede o limite de " + MAX_PERGUNTA + " caracteres.");
		}
		if (INTERNAL_TOKEN == "CONFIGURE_MESMO_TOKEN_DO_WAR" || INTERNAL_TOKEN.length < 16) {
			return linhaErro(dataset, "INTERNAL_TOKEN do dataset nao configurado (deve bater com api.internalToken do WAR).");
		}

		var usuario = fluigAPI.getUserService().getCurrent();
		var login = asJs(usuario.getLogin());
		var codigo = asJs(usuario.getCode());

		if (!usuarioAdmin(login)) {
			return linhaErro(dataset, "Acesso permitido apenas para o papel admin.");
		}

		// Monta o JSON do body so com strings JS (evita Model#107 toJSON do Rhino/Fluig).
		var paramsJson = '{"processo":' + jsonString(processo) + ',"pergunta":' + jsonString(pergunta);
		if (historicoJson) {
			try {
				JSON.parse(historicoJson);
			} catch (parseErr) {
				return linhaErro(dataset, "HISTORICO deve ser um JSON array valido.");
			}
			paramsJson += ',"historico":' + historicoJson;
		}
		paramsJson += "}";

		var dataJson = "{"
			+ '"companyId":' + jsonString(asJs(getValue("WKCompany"))) + ","
			+ '"serviceCode":"ASSISTENTE_SUPORTE_FLUIG",'
			+ '"endpoint":"/rest/chat",'
			+ '"method":"POST",'
			+ '"timeoutService":"30000",'
			+ '"headers":{'
			+ '"Content-Type":"application/json",'
			+ '"X-Internal-Token":' + jsonString(INTERNAL_TOKEN) + ","
			+ '"X-User-Code":' + jsonString(codigo)
			+ "},"
			+ '"params":' + paramsJson
			+ "}";

		log.info("###ds_assistente_suporte_fluig processo=" + processo + " user=" + codigo);

		var clientService = fluigAPI.getAuthorizeClientService();
		var vo = clientService.invoke(dataJson);
		var httpStatus = vo.getHttpStatusResult();
		var resultText = vo.getResult() != null ? asJs(vo.getResult()) : "";

		if (httpStatus < 200 || httpStatus >= 300) {
			var msgHttp = extrairMensagemErro(resultText);
			return linhaErro(dataset, "WAR HTTP " + httpStatus + (msgHttp ? ": " + msgHttp : ""));
		}

		if (!resultText) {
			return linhaErro(dataset, "Retorno vazio do assistente_suporte_fluig.");
		}

		var retorno = JSON.parse(resultText);
		if (retorno.status == "error") {
			return linhaErro(dataset, retorno.message || "Erro retornado pelo assistente.");
		}

		dataset.addRow([
			asJs(retorno.resposta),
			asJs(retorno.processo) || processo,
			asJs(retorno.contexto),
			""
		]);
		return dataset;

	} catch (err) {
		log.error("###ds_assistente_suporte_fluig erro: " + err);
		return linhaErro(dataset, asJs(err.message || err));
	}
}

function usuarioAdmin(login) {
	var c1 = DatasetFactory.createConstraint("ROLE_CODE", "admin", "admin", ConstraintType.MUST);
	var c2 = DatasetFactory.createConstraint("LOGIN", login, login, ConstraintType.MUST);
	var dsAdmin = DatasetFactory.getDataset("fdn_userrole", null, [c1, c2], null);
	return dsAdmin != null && dsAdmin.rowsCount > 0;
}

function linhaErro(dataset, mensagem) {
	dataset.addRow(["", "", "", asJs(mensagem)]);
	return dataset;
}

function extrairMensagemErro(resultText) {
	if (!resultText) {
		return "";
	}
	try {
		var json = JSON.parse(resultText);
		return asJs(json.message || json.ERRO || "");
	} catch (e) {
		return resultText.length > 300 ? resultText.substring(0, 300) : resultText;
	}
}

/** Converte valor Java/Rhino em string JavaScript pura. */
function asJs(v) {
	if (v === null || v === undefined) {
		return "";
	}
	return ("" + v).replace(/^\s+|\s+$/g, "");
}

/** Escapa string para literal JSON (sem JSON.stringify em objetos Java). */
function jsonString(s) {
	var t = asJs(s);
	t = t.replace(/\\/g, "\\\\");
	t = t.replace(/"/g, '\\"');
	t = t.replace(/\r/g, "\\r");
	t = t.replace(/\n/g, "\\n");
	t = t.replace(/\t/g, "\\t");
	return '"' + t + '"';
}
