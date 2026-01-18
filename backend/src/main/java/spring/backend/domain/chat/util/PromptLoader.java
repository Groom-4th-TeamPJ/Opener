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
            ClassPathResource resource = new ClassPathResource(PROMPTS_BASE_PATH + templateName);
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
}
