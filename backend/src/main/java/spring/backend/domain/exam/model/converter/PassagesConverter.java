package spring.backend.domain.exam.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import spring.backend.domain.exam.model.dto.Passage;

import java.util.Collections;
import java.util.List;

@Converter
public class PassagesConverter implements AttributeConverter<List<Passage>, String> {

    private static final ObjectMapper MAPPER = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    @Override
    public String convertToDatabaseColumn(List<Passage> passages) {
        try {
            return passages == null ? null : MAPPER.writeValueAsString(passages);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Passage> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? Collections.emptyList()
                    : MAPPER.readValue(dbData, new TypeReference<List<Passage>>() {});
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
