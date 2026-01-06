package spring.backend.domain.exam.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import spring.backend.domain.exam.model.dto.Passage;

import java.util.Collections;
import java.util.List;

@Converter
public class PassagesConverter implements AttributeConverter<List<Passage>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Passage> passages) {
        try {
            return passages == null ? null : objectMapper.writeValueAsString(passages);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public List<Passage> convertToEntityAttribute(String dbData) {
        try {
            return dbData == null ? Collections.emptyList()
                    : objectMapper.readValue(dbData, new TypeReference<List<Passage>>() {});
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
