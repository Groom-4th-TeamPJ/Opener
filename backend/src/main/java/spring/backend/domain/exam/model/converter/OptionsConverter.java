package spring.backend.domain.exam.model.converter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import spring.backend.domain.exam.model.dto.Option;

import java.util.Collections;
import java.util.List;

@Converter
public class OptionsConverter implements AttributeConverter<List<Option>, String> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(List<Option> optionList) {
        try {
            return optionList == null ? null : objectMapper.writeValueAsString(optionList);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public List<Option> convertToEntityAttribute(String s) {
        try {
            return s == null ? Collections.emptyList()
                    : objectMapper.readValue(s, new TypeReference<List<Option>>() {});
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
