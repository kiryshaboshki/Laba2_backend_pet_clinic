package app.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;

public final class Json {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private Json() {}

    public static <T> T read(InputStream in, Class<T> type) throws IOException {
        return MAPPER.readValue(in, type);
    }

    public static byte[] write(Object value) throws IOException {
        return MAPPER.writeValueAsBytes(value);
    }
}