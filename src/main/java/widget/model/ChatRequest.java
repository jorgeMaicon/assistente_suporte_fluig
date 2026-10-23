package widget.model;

import java.util.List;

public final class ChatRequest {

	private String processo;
	private String pergunta;
	private List<HistoryMessage> historico;

	public String getProcesso() {
		return processo;
	}

	public void setProcesso(String processo) {
		this.processo = processo;
	}

	public String getPergunta() {
		return pergunta;
	}

	public void setPergunta(String pergunta) {
		this.pergunta = pergunta;
	}

	public List<HistoryMessage> getHistorico() {
		return historico;
	}

	public void setHistorico(List<HistoryMessage> historico) {
		this.historico = historico;
	}
}
