package widget.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

	@Test
	void bloqueiaAcimaDoLimiteELiberaNaProximaJanela() {
		RateLimiter limiter = new RateLimiter();
		long t = 1_000_000L;

		assertTrue(limiter.tryAcquire("ip:1", 2, t));
		assertTrue(limiter.tryAcquire("ip:1", 2, t + 1));
		assertFalse(limiter.tryAcquire("ip:1", 2, t + 2));
		assertTrue(limiter.tryAcquire("ip:2", 2, t + 2));

		assertTrue(limiter.tryAcquire("ip:1", 2, t + 60_000));
	}

	@Test
	void limiteZeroDesativa() {
		RateLimiter limiter = new RateLimiter();
		for (int i = 0; i < 100; i++) {
			assertTrue(limiter.tryAcquire("x", 0, i));
		}
	}
}
