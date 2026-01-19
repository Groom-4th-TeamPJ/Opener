package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.backend.domain.chat.dto.request.GenerateQuestionRequest;
import spring.backend.domain.chat.dto.response.GenerateQuestionResponse;
import spring.backend.domain.chat.model.entity.QuestionNew;
import spring.backend.domain.chat.model.enums.Category;
import spring.backend.domain.chat.model.enums.QuestionType;
import spring.backend.domain.chat.model.vo.Option;
import spring.backend.domain.chat.model.vo.Passage;
import spring.backend.domain.chat.repository.spec.QuestionNewRepository;
import spring.backend.domain.chat.service.spec.QuestionNewService;
import spring.backend.domain.chat.util.PromptLoader;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.jpa.JpaQuestionRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.user.model.entity.User;
import spring.backend.domain.user.repository.spec.UserRepository;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

/**
 * 변형 문제 생성 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionNewServiceImpl implements QuestionNewService {

    private final QuestionNewRepository questionNewRepository;
    private final JpaQuestionRepository questionRepository;
    private final QuestionResultRepository questionResultRepository;
    private final UserRepository userRepository;
    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;

    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    @Override
    @Transactional
    public GenerateQuestionResponse generateQuestion(
            GenerateQuestionRequest request,
            UUID userId
    ) {
        log.info("[QuestionNew] 변형 문제 생성 시작 - questionId: {}, questionResultId: {}, userId: {}",
                request.questionId(), request.questionResultId(), userId);

        // 1. 사용자 조회
        User user = userRepository.findUserById(userId);

        // 2. 원본 문제 조회
        Question originalQuestion = questionRepository.findById(request.questionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 3. QuestionResult 조회 및 권한 검증
        QuestionResult questionResult = questionResultRepository.findById(request.questionResultId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESULT_NOT_FOUND));

        // 권한 검증: QuestionResult의 소유자가 요청한 사용자인지 확인
        if (!questionResult.getExamResult().getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.INVALID_QUESTION);
        }

        // 4. 원본 문제 컨텍스트 구성
        String problemContext = buildProblemContext(originalQuestion);
        log.debug("[QuestionNew] 원본 문제 컨텍스트 길이: {}", problemContext.length());

        // 5. RAG: 유사 문서 검색
        String retrievedContext = searchSimilarDocuments(problemContext);

        // 6. LLM으로 변형 문제 생성
        String llmResponse = generateWithLLM(retrievedContext, problemContext);
        log.debug("[QuestionNew] LLM 응답 길이: {}", llmResponse.length());

        // 7. JSON 파싱 및 엔티티 생성
        QuestionNew questionNew = parseAndCreateEntity(
                llmResponse,
                user,
                questionResult,
                originalQuestion.getCategory()
        );

        // 8. DB 저장
        QuestionNew savedQuestion = questionNewRepository.save(questionNew);
        log.info("[QuestionNew] 변형 문제 저장 완료 - questionNewId: {}", savedQuestion.getId());

        // 9. Response 생성 및 반환
        return GenerateQuestionResponse.builder()
                .passages(savedQuestion.getPassages())
                .options(savedQuestion.getOptions())
                .answer(savedQuestion.getAnswer())
                .analysis(savedQuestion.getAnalysis())
                .build();
    }

    /**
     * 원본 문제를 하나의 문자열 컨텍스트로 변환
     */
    private String buildProblemContext(Question question) {
        StringBuilder context = new StringBuilder();
        context.append("카테고리: ").append(question.getCategory()).append("\n");
        context.append("배점: ").append(question.getPoint()).append("점\n\n");

        // Passages (지문)
        context.append("=== 문제 지문 ===\n");
        for (spring.backend.domain.exam.model.dto.Passage passage : question.getPassages()) {
            context.append(passage.content()).append("\n");
        }
        context.append("\n");

        // Options (선택지)
        if (question.getOptions() != null && !question.getOptions().isEmpty()) {
            context.append("=== 선택지 ===\n");
            for (var option : question.getOptions()) {
                context.append(option.order()).append(". ").append(option.content()).append("\n");
            }
            context.append("\n");
        }

        // Answer (정답)
        if (question.getAnswer() != null) {
            context.append("=== 정답 ===\n");
            context.append("정답: ").append(question.getAnswer()).append("번\n");
        }

        return context.toString();
    }

    /**
     * VectorStore에서 유사 문서 검색
     */
    private String searchSimilarDocuments(String problemContext) {
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(problemContext)
                    .topK(topK)
                    .similarityThreshold(similarityThreshold)
                    .build();

            List<Document> similarDocuments = vectorStore.similaritySearch(searchRequest);
            log.info("[QuestionNew] 유사 문서 검색 완료 - 검색된 문서 수: {}", similarDocuments.size());

            if (similarDocuments.isEmpty()) {
                log.warn("[QuestionNew] 유사 문서를 찾지 못했습니다.");
                return "";
            }

            return similarDocuments.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n=== 참고 자료 구분 ===\n\n"));

        } catch (Exception e) {
            log.error("[QuestionNew] 유사 문서 검색 실패", e);
            return "";
        }
    }

    /**
     * LLM으로 변형 문제 생성
     */
    private String generateWithLLM(String retrievedContext, String problemContext) {
        try {
            String prompt = promptLoader.buildVariantQuestionPrompt(retrievedContext, problemContext);
            log.debug("[QuestionNew] 프롬프트 구성 완료 - 전체 길이: {}", prompt.length());

            ChatClient chatClient = chatClientBuilder.build();

            String response = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            if (response == null || response.trim().isEmpty()) {
                throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
            }

            return response;

        } catch (Exception e) {
            log.error("[QuestionNew] LLM 생성 실패", e);
            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);
        }
    }

    /**
     * JSON 응답 전처리: LaTeX 백슬래시 및 비정상적인 구조 제거
     */
    private String preprocessJson(String jsonStr) {
        String preprocessed = jsonStr;

        // 1. 비정상적인 JSON 구조 제거 ([null], null: null 패턴)
        preprocessed = preprocessed.replaceAll("\\[null\\]\\s*null:\\s*null\\s*", "");
        preprocessed = preprocessed.replaceAll("null:\\s*null\\s*,?", "");

        // 2. 연속된 쉼표 제거 (제거 후 남은 잘못된 구조)
        preprocessed = preprocessed.replaceAll(",\\s*,", ",");
        preprocessed = preprocessed.replaceAll(",\\s*\\]", "]");
        preprocessed = preprocessed.replaceAll(",\\s*}", "}");

        // 3. 로깅: 전처리로 변경사항이 있었는지 확인
        if (!jsonStr.equals(preprocessed)) {
            log.warn("[QuestionNew] JSON 전처리 수행됨 - 비정상적인 구조 제거");
            log.debug("[QuestionNew] 전처리 전: {}", jsonStr);
            log.debug("[QuestionNew] 전처리 후: {}", preprocessed);
        }

        return preprocessed;
    }

    /**
     * LLM 응답 JSON 파싱 및 QuestionNew 엔티티 생성
     */
    private QuestionNew parseAndCreateEntity(
            String llmResponse,
            User user,
            QuestionResult questionResult,
            spring.backend.domain.exam.model.enums.Category originalCategory
    ) {
        try {
            // JSON 추출 (마크다운 코드 블록 제거)
            String jsonStr = llmResponse.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            // JSON 전처리: LaTeX 백슬래시 및 비정상적인 구조 처리
            jsonStr = preprocessJson(jsonStr);

            // JSON 파싱
            JsonNode rootNode = objectMapper.readTree(jsonStr);

            // passages 파싱
            List<Passage> passages = objectMapper.convertValue(
                    rootNode.get("passages"),
                    new TypeReference<List<Passage>>() {
                    }
            );

            // options 파싱
            List<Option> options = objectMapper.convertValue(
                    rootNode.get("options"),
                    new TypeReference<List<Option>>() {
                    }
            );

            // answer 파싱
            Integer answer = rootNode.get("answer").asInt();

            // analysis 파싱
            String analysis = rootNode.get("analysis").asText();

            // Category 매핑 (exam domain → chat domain)
            Category category = mapCategory(originalCategory);

            // QuestionType은 객관식으로 고정 (옵션이 있으므로)
            QuestionType questionType = QuestionType.MCQ;

            // 엔티티 생성
            return QuestionNew.create(
                    user,
                    questionResult,
                    passages,
                    options,
                    answer,
                    category,
                    questionType,
                    analysis
            );

        } catch (com.fasterxml.jackson.core.JsonParseException e) {
            log.error("[QuestionNew] JSON 파싱 실패 - 유효하지 않은 JSON 형식", e);
            log.error("[QuestionNew] 파싱 실패한 JSON (처음 500자): {}",
                    llmResponse.substring(0, Math.min(500, llmResponse.length())));
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        } catch (Exception e) {
            log.error("[QuestionNew] 엔티티 생성 실패 - LLM 응답 처리 중 예외 발생", e);
            log.error("[QuestionNew] LLM 응답 (처음 500자): {}",
                    llmResponse.substring(0, Math.min(500, llmResponse.length())));
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    /**
     * exam domain Category를 chat domain Category로 매핑
     */
    private Category mapCategory(spring.backend.domain.exam.model.enums.Category examCategory) {
        return switch (examCategory) {
            case ALG -> Category.ALG;
            case GEO -> Category.GEO;
            case PROB -> Category.PROB;
            case CALC -> Category.CALC;
        };
    }
}
