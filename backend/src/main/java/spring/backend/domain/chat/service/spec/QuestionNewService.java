package spring.backend.domain.chat.service.spec;

import java.util.UUID;
import spring.backend.domain.chat.dto.request.GenerateQuestionRequest;
import spring.backend.domain.chat.dto.response.GenerateQuestionResponse;

public interface QuestionNewService {

    /**
     * RAG 기반 변형 문제 생성
     */
    GenerateQuestionResponse generateQuestion(
            GenerateQuestionRequest request,
            UUID userId
    );
}
