package spring.backend.domain.chat.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.domain.chat.dto.request.GenerateQuestionRequest;
import spring.backend.domain.chat.dto.response.GenerateQuestionResponse;
import spring.backend.domain.chat.model.entity.QuestionNew;
import spring.backend.domain.chat.model.enums.Category;
import spring.backend.domain.chat.model.enums.PassageType;
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
    private final CanService canService;

    // RAG 파라미터 외부화 -> 검색 품질을 코드 수정 없이 yml 로 조정
    @Value("${app.rag.top-k:5}")
    private int topK;

    @Value("${app.rag.similarity-threshold:0.7}")
    private double similarityThreshold;

    // @Transactional -> 변형문제 저장 + Can 차감이 한 단위, 중간 실패 시 일관성 유지
    @Override
    @Transactional
    public GenerateQuestionResponse generateQuestion(
            GenerateQuestionRequest request,
            UUID userId
    ) {
        // 작업 전 선차감 -> LLM 비용 발생 전에 잔액 검증, 부족 시 즉시 중단
        canService.useUserCan(userId, 1);

        try {
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

            // 소유자 검증 -> id 만 알면 남의 시험 결과로 문제 생성 가능하므로 실제 주인과 대조
            if (!questionResult.getExamResult().getUserId().equals(userId)) {
                throw new BusinessException(ErrorCode.INVALID_QUESTION);
            }

            // 4. 원본 문제 컨텍스트 구성
            String problemContext = buildProblemContext(originalQuestion);
            log.debug("[QuestionNew] 원본 문제 컨텍스트 길이: {}", problemContext.length());

            // RAG -> 원본 문제와 유사한 기출/자료를 검색해 LLM 에 근거로 제공, 환각 줄이고 출제 형식 맞춤
            String retrievedContext = searchSimilarDocuments(problemContext);

            // CompletableFuture + orTimeout -> 동기 LLM 호출이 무한 대기하면 트랜잭션/스레드가 묶이므로 60초 강제 컷
            String llmResponse = CompletableFuture
                    .supplyAsync(() -> generateWithLLM(retrievedContext, problemContext))
                    .orTimeout(60, TimeUnit.SECONDS)
                    .join();
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

        } catch (CompletionException e) {
            // 실패 분기마다 Can 복구 -> 결과를 못 받았는데 비용만 빠지는 불공정 방지
            if (e.getCause() instanceof TimeoutException) {
                log.error("[QuestionNew] LLM 응답 타임아웃 (60초 초과) - userId: {}", userId);
                canService.recoverUserCan(userId, 1);
                throw new BusinessException(ErrorCode.LLM_TIMEOUT);
            }
            log.error("[QuestionNew] LLM 호출 중 예외 발생 - userId: {}", userId, e);
            canService.recoverUserCan(userId, 1);
            throw new BusinessException(ErrorCode.LLM_RESPONSE_FAIL);

        } catch (BusinessException e) {
            // 비즈니스 예외는 그대로 전파 (권한 없음, 리소스 없음 등)
            // Can은 이미 차감되었으므로 복구 필요
            log.error("[QuestionNew] 비즈니스 예외 발생 - userId: {}, errorCode: {}",
                    userId, e.getErrorCode().getCode(), e);
            canService.recoverUserCan(userId, 1);
            throw e;

        } catch (Exception e) {
            // 기타 예외 처리
            log.error("[QuestionNew] 변형 문제 생성 실패 - userId: {}", userId, e);
            canService.recoverUserCan(userId, 1);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
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
            // 원본 문제 텍스트를 쿼리로 임베딩 검색 -> 키워드가 아닌 의미 기반으로 유사 자료 확보
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

        // 검색 실패해도 빈 문자열 반환 -> RAG 는 보조 수단이므로 실패가 문제 생성 전체를 막지 않게 함
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
    // LLM 출력은 100% 정형 JSON 보장 안 됨 -> 파싱 전 비정상 패턴 정리해 역직렬화 실패율 낮춤
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

            // passages 파싱 (null 체크 및 단수/복수 처리)
            List<Passage> passages = parsePassages(rootNode, jsonStr);

            // options 파싱
            List<Option> options = objectMapper.convertValue(
                    rootNode.get("options"),
                    new TypeReference<List<Option>>() {
                    }
            );

            // answer 파싱 (null 체크)
            JsonNode answerNode = rootNode.get("answer");
            if (answerNode == null || answerNode.isNull()) {
                log.error("[QuestionNew] LLM 응답에 answer 필드가 없거나 null입니다. 응답: {}", jsonStr);
                throw new BusinessException(ErrorCode.LLM_GENERATE_FAIL);
            }
            Integer answer = answerNode.asInt();

            // analysis 파싱 (null 체크)
            JsonNode analysisNode = rootNode.get("analysis");
            String analysis = (analysisNode != null && !analysisNode.isNull())
                    ? analysisNode.asText()
                    : "";

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
            throw new BusinessException(ErrorCode.LLM_GENERATE_FAIL);
        }
    }

    /**
     * exam domain Category를 chat domain Category로 매핑
     */
    // 도메인 간 enum 분리 -> exam 과 chat 이 서로의 enum 에 직접 의존하지 않도록 경계에서 명시 변환
    private Category mapCategory(spring.backend.domain.exam.model.enums.Category examCategory) {
        return switch (examCategory) {
            case ALG -> Category.ALG;
            case GEO -> Category.GEO;
            case PROB -> Category.PROB;
            case CALC -> Category.CALC;
        };
    }

    /**
     * LLM 응답에서 passages 파싱 (단수/복수, 문자열/배열 모두 지원) - "passages": [...] → 그대로 파싱 - "passage": [...] → 그대로 파싱 - "passage":
     * "..." → 단일 Passage로 변환
     */
    private List<Passage> parsePassages(JsonNode rootNode, String jsonStr) {
        // 1. "passages" (복수형) 먼저 확인
        JsonNode passagesNode = rootNode.get("passages");
        if (passagesNode != null && !passagesNode.isNull() && passagesNode.isArray()) {
            List<Passage> passages = parsePassageArray(passagesNode);
            if (passages != null && !passages.isEmpty()) {
                log.debug("[QuestionNew] passages 배열 파싱 성공 - 개수: {}", passages.size());
                return passages;
            }
        }

        // 2. "passage" (단수형) 확인
        JsonNode passageNode = rootNode.get("passage");
        if (passageNode != null && !passageNode.isNull()) {
            // 2-1. 배열인 경우 (기대하는 형식)
            if (passageNode.isArray() && passageNode.size() > 0) {
                List<Passage> passages = parsePassageArray(passageNode);
                if (passages != null && !passages.isEmpty()) {
                    log.debug("[QuestionNew] passage 배열 파싱 성공 - 개수: {}", passages.size());
                    return passages;
                }
            }
            // 2-2. 문자열인 경우 → 단일 Passage로 변환
            if (passageNode.isTextual()) {
                String passageContent = passageNode.asText();
                if (passageContent != null && !passageContent.isBlank()) {
                    log.info("[QuestionNew] passage(문자열) → Passage 객체 변환 수행");
                    return List.of(new Passage(1, PassageType.TEXT, passageContent));
                }
            }
        }

        // 3. 둘 다 없는 경우
        log.error("[QuestionNew] LLM 응답에 passages/passage 필드가 없거나 null입니다. 응답: {}", jsonStr);
        throw new BusinessException(ErrorCode.LLM_GENERATE_FAIL);
    }

    /**
     * JSON 배열을 List<Passage>로 파싱 (order가 없으면 인덱스+1로 자동 설정)
     */
    private List<Passage> parsePassageArray(JsonNode arrayNode) {
        List<Passage> passages = new java.util.ArrayList<>();
        int index = 1;
        for (JsonNode node : arrayNode) {
            // order: 없으면 인덱스 사용
            Integer order = node.has("order") && !node.get("order").isNull()
                    ? node.get("order").asInt()
                    : index;

            // type: 없으면 TEXT 기본값
            PassageType type = PassageType.TEXT;
            if (node.has("type") && !node.get("type").isNull()) {
                String typeStr = node.get("type").asText();
                try {
                    type = PassageType.fromValue(typeStr);
                } catch (IllegalArgumentException e) {
                    log.warn("[QuestionNew] 알 수 없는 PassageType: {}, TEXT로 대체", typeStr);
                }
            }

            // content: 필수
            String content = node.has("content") ? node.get("content").asText() : null;
            if (content == null || content.isBlank()) {
                log.warn("[QuestionNew] passage[{}]의 content가 비어있습니다", index);
                continue;
            }

            passages.add(new Passage(order, type, content));
            index++;
        }
        return passages;
    }
}
