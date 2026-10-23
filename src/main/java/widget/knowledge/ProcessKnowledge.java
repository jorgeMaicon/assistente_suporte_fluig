package widget.knowledge;

public final class ProcessKnowledge {

	private final String processId;
	private final String markdown;
	private final String hash;
	private final String lastAnalyzed;

	public ProcessKnowledge(String processId, String markdown, String hash, String lastAnalyzed) {
		this.processId = processId;
		this.markdown = markdown;
		this.hash = hash;
		this.lastAnalyzed = lastAnalyzed;
	}

	public String getProcessId() {
		return processId;
	}

	public String getMarkdown() {
		return markdown;
	}

	public String getHash() {
		return hash;
	}

	public String getLastAnalyzed() {
		return lastAnalyzed;
	}
}
