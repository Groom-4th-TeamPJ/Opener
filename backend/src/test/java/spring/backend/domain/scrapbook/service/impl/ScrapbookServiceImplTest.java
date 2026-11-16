package spring.backend.domain.scrapbook.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import spring.backend.domain.chat.dto.enums.ChatRole;
import spring.backend.domain.chat.dto.response.ChatHistoryResponse;
import spring.backend.domain.chat.dto.response.ChatMessageDto;
import spring.backend.domain.chat.service.spec.ChatService;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.entity.ExamResult;
import spring.backend.domain.exam.model.entity.Question;
import spring.backend.domain.exam.model.entity.QuestionResult;
import spring.backend.domain.exam.repository.spec.ExamRepository;
import spring.backend.domain.exam.repository.spec.ExamResultRepository;
import spring.backend.domain.exam.repository.spec.QuestionResultRepository;
import spring.backend.domain.scrapbook.dto.response.detail.ScrapbookDetailResponse;

// 요약은 저장 뒤에 따로 채워지므로 방금 끝난 대화의 상세보기는 요약이 null 일 수 있다
// 이때도 대화와 문제 정보는 그대로 내려가야 한다
@ExtendWith(MockitoExtension.class)
class ScrapbookServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final Long QUESTION_RESULT_ID = 11L;

    @Mock private ExamResultRepository examResultRepository;
    @Mock private ExamRepository examRepository;
    @Mock private QuestionResultRepository questionResultRepository;
    @Mock private ChatService chatService;
    @Mock private QuestionResult questionResult;
    @Mock private ExamResult examResult;
    @Mock private Exam exam;
    @Mock private Question question;

    private ScrapbookServiceImpl service() {
        return new ScrapbookServiceImpl(examResultRepository, examRepository, questionResultRepository, chatService);
    }

    // 소유자 검증과 오프너 사용 이력 검증을 통과하는 문제 결과를 만든다
    private void givenOpenerQuestionResultOwnedByUser() {
        when(questionResultRepository.findById(QUESTION_RESULT_ID)).thenReturn(Optional.of(questionResult));
        when(questionResult.getExamResult()).thenReturn(examResult);
        when(examResult.getExam()).thenReturn(exam);
        when(examResult.getUserId()).thenReturn(USER_ID);
        when(questionResult.getQuestion()).thenReturn(question);
        when(questionResult.isOpener()).thenReturn(true);
        when(question.getQuestionNo()).thenReturn(7);
    }

    private List<ChatMessageDto> chat() {
        return List.of(
                new ChatMessageDto(1, ChatRole.USER, "질문", LocalDateTime.now()),
                new ChatMessageDto(2, ChatRole.LLM, "답", LocalDateTime.now()));
    }

    @Test
    @DisplayName("요약이 아직 없는 대화의 상세보기는 요약만 null 이고 대화와 문제 정보는 채워서 돌려준다")
    void getScrapbookDetail_요약null_예외없이요약만비어서내려간다() {
        givenOpenerQuestionResultOwnedByUser();
        when(chatService.getChatHistoryByQuestionResultId(QUESTION_RESULT_ID))
                .thenReturn(ChatHistoryResponse.builder().chat(chat()).summary(null).build());

        ScrapbookDetailResponse response =
                assertDoesNotThrow(() -> service().getScrapbookDetail(USER_ID, QUESTION_RESULT_ID));

        assertNull(response.getPromptSummary());
        assertEquals(2, response.getChat().size());
        assertEquals("질문", response.getChat().get(0).content());
        assertEquals(7, response.getQuestionNo());
    }

    @Test
    @DisplayName("요약이 있으면 가공 없이 그대로 돌려준다")
    void getScrapbookDetail_요약있음_그대로내려간다() {
        givenOpenerQuestionResultOwnedByUser();
        when(chatService.getChatHistoryByQuestionResultId(QUESTION_RESULT_ID))
                .thenReturn(ChatHistoryResponse.builder().chat(chat()).summary("  요약문  ").build());

        ScrapbookDetailResponse response = service().getScrapbookDetail(USER_ID, QUESTION_RESULT_ID);

        // 앞뒤 공백까지 그대로 -> 서비스가 요약 문자열을 건드리지 않는다
        assertEquals("  요약문  ", response.getPromptSummary());
        assertEquals(2, response.getChat().size());
    }
}
