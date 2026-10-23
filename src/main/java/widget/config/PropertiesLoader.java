package widget.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

final class PropertiesLoader {

	private PropertiesLoader() {
	}

	static Properties fromClasspath(String resource) {
		Properties properties = optionalFromClasspath(resource);
		if (properties == null) {
			throw new ConfigException("Arquivo de configuracao nao encontrado no classpath: " + resource);
		}
		return properties;
	}

	/**
	 * Carrega um resource do classpath. Retorna null se o arquivo nao existir.
	 */
	static Properties optionalFromClasspath(String resource) {
		try (InputStream in = PropertiesLoader.class.getClassLoader().getResourceAsStream(resource)) {
			if (in == null) {
				return null;
			}
			Properties properties = new Properties();
			properties.load(new InputStreamReader(in, StandardCharsets.UTF_8));
			return properties;
		} catch (IOException e) {
			throw new ConfigException("Falha ao ler " + resource, e);
		}
	}

	static String optional(Properties p, String key) {
		String value = p.getProperty(key);
		if (value == null) {
			return null;
		}
		value = value.trim();
		return value.isEmpty() ? null : value;
	}

	static String optional(Properties p, String key, String defaultValue) {
		String value = optional(p, key);
		return value == null ? defaultValue : value;
	}

	static String required(Properties p, String key) {
		String value = optional(p, key);
		if (value == null) {
			throw new ConfigException("Propriedade obrigatoria ausente: " + key);
		}
		return value;
	}

	static int intValue(Properties p, String key, int defaultValue) {
		String value = optional(p, key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new ConfigException("Propriedade " + key + " deve ser numerica: " + value);
		}
	}

	static double doubleValue(Properties p, String key, double defaultValue) {
		String value = optional(p, key);
		if (value == null) {
			return defaultValue;
		}
		try {
			return Double.parseDouble(value);
		} catch (NumberFormatException e) {
			throw new ConfigException("Propriedade " + key + " deve ser numerica: " + value);
		}
	}

	static boolean boolValue(Properties p, String key, boolean defaultValue) {
		String value = optional(p, key);
		return value == null ? defaultValue : Boolean.parseBoolean(value);
	}

	static Set<String> list(Properties p, String key) {
		String value = optional(p, key);
		if (value == null) {
			return Collections.emptySet();
		}
		Set<String> items = Arrays.stream(value.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.collect(Collectors.toCollection(LinkedHashSet::new));
		return Collections.unmodifiableSet(items);
	}
}
