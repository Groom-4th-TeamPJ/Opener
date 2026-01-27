package spring.backend.domain.chat.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * 프롬프트 템플릿 파일을 로드하고 변수를 치환하는 유틸리티 클래스
 */
@Slf4j
@Component
public class PromptLoader {

    private static final String PROMPTS_BASE_PATH = "prompts/";

    /**
     * 프롬프트 템플릿 로드
     */
    public String loadPromptTemplate(String templateName) {
        try {
            ClassLoader cl = PromptLoader.class.getClassLoader();
            ClassPathResource resource = new ClassPathResource("prompts/" + templateName, cl);

            String path = PROMPTS_BASE_PATH + templateName;

            ClassLoader tccl = Thread.currentThread().getContextClassLoader();
            ClassLoader mycl = PromptLoader.class.getClassLoader();

            log.info("[PromptLoader] path={}, TCCL={}, MyCL={}", path, tccl, mycl);
            log.info("[PromptLoader] TCCL.getResource={}", tccl == null ? null : tccl.getResource(path));
            log.info("[PromptLoader] MyCL.getResource={}", mycl.getResource(path));

            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("[PromptLoader] 프롬프트 템플릿 로드 실패 - templateName: {}", templateName, e);
            throw new RuntimeException("프롬프트 템플릿 로드 실패: " + templateName, e);
        }
    }

    /**
     * 프롬프트 템플릿의 변수를 치환합니다
     */
    public String replaceVariable(String template, String varName, String value) {
        return template.replace("{" + varName + "}", value);
    }

    /**
     * RAG 오프너 분석 프롬프트를 생성합니다 (검색된 문서 포함)
     */
    public String buildRagOpenerAnalysisPrompt(String retrievedContext, String problemContext) {
        String template = loadPromptTemplate("rag-opener-analysis.txt");
        template = replaceVariable(template, "retrievedContext", retrievedContext);
        template = replaceVariable(template, "problemContext", problemContext);
        return template;
    }

    /**
     * 일반 오프너 분석 프롬프트를 생성합니다
     */
    public String buildNoRagOpenerAnalysisPrompt(String problemContext) {
        String template = loadPromptTemplate("no-rag-opener-analysis.txt");
        template = replaceVariable(template, "problemContext", problemContext);
        return template;
    }

    /**
     * 채팅 RAG 시스템 프롬프트를 생성합니다
     */
    public String buildChatRagSystemPrompt(String ragContext) {
        String template = loadPromptTemplate("chat-rag-system.txt");
        template = replaceVariable(template, "ragContext", ragContext);
        return template;
    }

    /**
     * 변형 문제 생성 프롬프트를 생성합니다
     *
     * @param retrievedContext RAG로 검색된 유사 문제 컨텍스트
     * @param problemContext   원본 문제 컨텍스트
     * @return 변형 문제 생성 프롬프트
     */
    public String buildVariantQuestionPrompt(String retrievedContext, String problemContext) {
        String template = loadPromptTemplate("generate-question.txt");
        template = replaceVariable(template, "retrievedContext", retrievedContext);
        template = replaceVariable(template, "problemContext", problemContext);
        return template;
    }

    /**
     * 대화 요약 프롬프트를 생성합니다 질문/답변 형식이 아닌 서술적인 요약을 생성하도록 안내합니다
     *
     * @return 대화 요약 프롬프트
     */
    public String buildChatSummaryPrompt() {
        return loadPromptTemplate("chat-summary.txt");
    }

    /**
     * 채팅 규칙 시스템 프롬프트를 로드합니다 LLM이 코치 역할을 수행하도록 톤, 대화 규칙, 금지 사항 등을 정의합니다
     *
     * @return 채팅 규칙 시스템 프롬프트
     */
    public String loadChatRulePrompt() {
        return loadPromptTemplate("chat-rule.txt");
    }

    /**
     * 채팅 RAG 시스템 프롬프트에 규칙을 포함하여 생성합니다
     *
     * @param ragContext RAG로 검색된 참고 자료
     * @return 규칙 + RAG 컨텍스트가 포함된 시스템 프롬프트
     */
    public String buildChatRagSystemPromptWithRule(String ragContext) {
        String rule = loadChatRulePrompt();
        String ragTemplate = loadPromptTemplate("chat-rag-system.txt");
        String ragPrompt = replaceVariable(ragTemplate, "ragContext", ragContext);
        return rule + "\n\n" + ragPrompt;
    }
}
