package widget.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON minimo (sem javax.json / Jackson) para o classloader do widget Fluig.
 * Cobre so o que o chat e a resposta da OpenAI precisam.
 */
public final class SimpleJson {

	private SimpleJson() {
	}

	public static String quote(String value) {
		if (value == null) {
			return "null";
		}
		StringBuilder sb = new StringBuilder(value.length() + 16);
		sb.append('"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '\\':
			case '"':
				sb.append('\\').append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\r':
				sb.append("\\r");
				break;
			case '\t':
				sb.append("\\t");
				break;
			default:
				if (c < 0x20) {
					sb.append(String.format("\\u%04x", (int) c));
				} else {
					sb.append(c);
				}
			}
		}
		sb.append('"');
		return sb.toString();
	}

	public static String object(Map<String, ?> fields) {
		StringBuilder sb = new StringBuilder();
		sb.append('{');
		boolean first = true;
		for (Map.Entry<String, ?> e : fields.entrySet()) {
			if (!first) {
				sb.append(',');
			}
			first = false;
			sb.append(quote(e.getKey())).append(':').append(value(e.getValue()));
		}
		sb.append('}');
		return sb.toString();
	}

	public static String arrayOfObjects(List<Map<String, String>> items) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < items.size(); i++) {
			if (i > 0) {
				sb.append(',');
			}
			sb.append(object(items.get(i)));
		}
		sb.append(']');
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	private static String value(Object v) {
		if (v == null) {
			return "null";
		}
		if (v instanceof String) {
			return quote((String) v);
		}
		if (v instanceof Number || v instanceof Boolean) {
			return String.valueOf(v);
		}
		if (v instanceof Map) {
			return object((Map<String, ?>) v);
		}
		if (v instanceof List) {
			List<?> list = (List<?>) v;
			StringBuilder sb = new StringBuilder();
			sb.append('[');
			for (int i = 0; i < list.size(); i++) {
				if (i > 0) {
					sb.append(',');
				}
				sb.append(value(list.get(i)));
			}
			sb.append(']');
			return sb.toString();
		}
		return quote(String.valueOf(v));
	}

	/** Extrai string de uma chave no primeiro nivel: "key":"value" (com escapes). */
	public static String extractString(String json, String key) {
		if (json == null || key == null) {
			return null;
		}
		String needle = quote(key) + ":";
		int idx = json.indexOf(needle);
		if (idx < 0) {
			needle = quote(key) + " :";
			idx = json.indexOf(needle);
		}
		if (idx < 0) {
			return null;
		}
		int i = idx + needle.length();
		while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
			i++;
		}
		if (i >= json.length() || json.charAt(i) != '"') {
			return null;
		}
		i++;
		StringBuilder sb = new StringBuilder();
		while (i < json.length()) {
			char c = json.charAt(i++);
			if (c == '\\') {
				if (i >= json.length()) {
					break;
				}
				char n = json.charAt(i++);
				switch (n) {
				case '"':
				case '\\':
				case '/':
					sb.append(n);
					break;
				case 'b':
					sb.append('\b');
					break;
				case 'f':
					sb.append('\f');
					break;
				case 'n':
					sb.append('\n');
					break;
				case 'r':
					sb.append('\r');
					break;
				case 't':
					sb.append('\t');
					break;
				case 'u':
					if (i + 4 <= json.length()) {
						sb.append((char) Integer.parseInt(json.substring(i, i + 4), 16));
						i += 4;
					}
					break;
				default:
					sb.append(n);
				}
			} else if (c == '"') {
				return sb.toString();
			} else {
				sb.append(c);
			}
		}
		return null;
	}

	/**
	 * Localiza choices[0].message.content na resposta da OpenAI.
	 * Procura o primeiro bloco "message":{..."content":"..."}.
	 */
	public static String extractChatContent(String openAiBody) {
		if (openAiBody == null) {
			return null;
		}
		int messageIdx = openAiBody.indexOf("\"message\"");
		if (messageIdx < 0) {
			return null;
		}
		int contentIdx = openAiBody.indexOf("\"content\"", messageIdx);
		if (contentIdx < 0) {
			return null;
		}
		return extractString(openAiBody.substring(contentIdx), "content");
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseObject(String json) {
		Object parsed = new Parser(json).parseValue();
		if (!(parsed instanceof Map)) {
			throw new IllegalArgumentException("JSON raiz deve ser objeto");
		}
		return (Map<String, Object>) parsed;
	}

	public static List<Map<String, String>> parseHistory(Object historico) {
		List<Map<String, String>> out = new ArrayList<>();
		if (!(historico instanceof List)) {
			return out;
		}
		for (Object item : (List<?>) historico) {
			if (!(item instanceof Map)) {
				continue;
			}
			Map<?, ?> map = (Map<?, ?>) item;
			Object role = map.get("role");
			Object content = map.get("content");
			if (role == null || content == null) {
				continue;
			}
			Map<String, String> row = new LinkedHashMap<>();
			row.put("role", String.valueOf(role));
			row.put("content", String.valueOf(content));
			out.add(row);
		}
		return out;
	}

	private static final class Parser {
		private final String json;
		private int i;

		Parser(String json) {
			this.json = json == null ? "" : json.trim();
		}

		Object parseValue() {
			skipWs();
			if (i >= json.length()) {
				throw new IllegalArgumentException("JSON vazio");
			}
			char c = json.charAt(i);
			if (c == '{') {
				return parseObject();
			}
			if (c == '[') {
				return parseArray();
			}
			if (c == '"') {
				return parseString();
			}
			if (c == 't' || c == 'f') {
				return parseBoolean();
			}
			if (c == 'n') {
				return parseNull();
			}
			return parseNumber();
		}

		private Map<String, Object> parseObject() {
			Map<String, Object> map = new LinkedHashMap<>();
			i++;
			skipWs();
			if (peek('}')) {
				i++;
				return map;
			}
			while (true) {
				skipWs();
				String key = parseString();
				skipWs();
				expect(':');
				Object value = parseValue();
				map.put(key, value);
				skipWs();
				if (peek('}')) {
					i++;
					return map;
				}
				expect(',');
			}
		}

		private List<Object> parseArray() {
			List<Object> list = new ArrayList<>();
			i++;
			skipWs();
			if (peek(']')) {
				i++;
				return list;
			}
			while (true) {
				list.add(parseValue());
				skipWs();
				if (peek(']')) {
					i++;
					return list;
				}
				expect(',');
			}
		}

		private String parseString() {
			expect('"');
			StringBuilder sb = new StringBuilder();
			while (i < json.length()) {
				char c = json.charAt(i++);
				if (c == '\\') {
					char n = json.charAt(i++);
					switch (n) {
					case '"':
					case '\\':
					case '/':
						sb.append(n);
						break;
					case 'b':
						sb.append('\b');
						break;
					case 'f':
						sb.append('\f');
						break;
					case 'n':
						sb.append('\n');
						break;
					case 'r':
						sb.append('\r');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'u':
						sb.append((char) Integer.parseInt(json.substring(i, i + 4), 16));
						i += 4;
						break;
					default:
						sb.append(n);
					}
				} else if (c == '"') {
					return sb.toString();
				} else {
					sb.append(c);
				}
			}
			throw new IllegalArgumentException("string JSON nao terminada");
		}

		private Boolean parseBoolean() {
			if (json.startsWith("true", i)) {
				i += 4;
				return Boolean.TRUE;
			}
			if (json.startsWith("false", i)) {
				i += 5;
				return Boolean.FALSE;
			}
			throw new IllegalArgumentException("boolean invalido em " + i);
		}

		private Object parseNull() {
			if (json.startsWith("null", i)) {
				i += 4;
				return null;
			}
			throw new IllegalArgumentException("null invalido em " + i);
		}

		private Number parseNumber() {
			int start = i;
			if (peek('-')) {
				i++;
			}
			while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '.'
					|| json.charAt(i) == 'e' || json.charAt(i) == 'E' || json.charAt(i) == '+'
					|| json.charAt(i) == '-')) {
				i++;
			}
			String raw = json.substring(start, i);
			if (raw.indexOf('.') >= 0 || raw.indexOf('e') >= 0 || raw.indexOf('E') >= 0) {
				return Double.valueOf(raw);
			}
			return Long.valueOf(raw);
		}

		private void skipWs() {
			while (i < json.length() && Character.isWhitespace(json.charAt(i))) {
				i++;
			}
		}

		private boolean peek(char c) {
			return i < json.length() && json.charAt(i) == c;
		}

		private void expect(char c) {
			skipWs();
			if (!peek(c)) {
				throw new IllegalArgumentException("esperado '" + c + "' em " + i);
			}
			i++;
		}
	}
}
