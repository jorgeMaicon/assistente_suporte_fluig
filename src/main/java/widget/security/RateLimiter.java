package widget.security;

import java.util.concurrent.ConcurrentHashMap;

/** Janela fixa de 1 minuto por chave, em memoria (por no do cluster). */
public class RateLimiter {

	private static final long WINDOW_MS = 60_000L;
	private static final int CLEANUP_THRESHOLD = 10_000;

	private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

	public boolean tryAcquire(String key, int limit, long nowMillis) {
		if (limit <= 0) {
			return true;
		}
		if (windows.size() > CLEANUP_THRESHOLD) {
			windows.entrySet().removeIf(e -> nowMillis - e.getValue().start >= WINDOW_MS);
		}
		Window window = windows.compute(key, (k, current) -> {
			if (current == null || nowMillis - current.start >= WINDOW_MS) {
				return new Window(nowMillis);
			}
			current.count++;
			return current;
		});
		return window.count <= limit;
	}

	int size() {
		return windows.size();
	}

	private static final class Window {
		private final long start;
		private int count = 1;

		Window(long start) {
			this.start = start;
		}
	}
}
