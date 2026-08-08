package spring.backend.domain.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import spring.backend.domain.chat.dto.request.ChatSaveRequest;
import spring.backend.domain.chat.dto.request.ChatSendRequest;
import spring.backend.domain.chat.dto.request.GenerateQuestionRequest;
import spring.backend.domain.chat.dto.request.OpenerAnalysisRequest;
import spring.backend.domain.chat.dto.response.GenerateQuestionResponse;
import spring.backend.domain.chat.service.spec.ChatService;
import spring.backend.domain.chat.service.spec.QuestionNewService;
import spring.backend.shared.infrastructure.security.dto.AuthUser;

// @RestController -> 반환값을 JSON 본문으로 직렬화, View 없이 API 응답에 특화
// @RequestMapping("/chat") -> 채팅 엔드포인트 공통 prefix 한 곳에서 관리
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "\uD83D\uDCAC Chat", description = "AI 채팅 / 분석")
public class ChatController {

    // 인터페이스 타입으로 주입 -> 컨트롤러는 구현 세부사항 모르게 하여 결합도 낮춤
    private final ChatService chatService;
    private final QuestionNewService questionNewService;

    // sse 연결
    @Operation (
            summary = "채팅 세션 연결 (SSE)" ,
            description = "특정 채팅 세션에 대해 서버-발송 이벤트(SSE) 연결을 설정합니다.\n" +
                    "- 클라이언트는 이 엔드포인트에 연결하여 서버로부터 실시간 메시지를 수신할 수 있습니다."
    )
    // produces=TEXT_EVENT_STREAM -> 응답을 끊지 않고 SSE 스트림으로 유지, 서버 푸시 가능
    // 반환 타입 Flux -> 논블로킹으로 청크를 흘려보내 스레드 점유 없이 다수 동시 연결 처리
    @GetMapping(
            value = "/connect",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<ServerSentEvent<String>> connectSession(
            @RequestParam Long sessionId,
            // @AuthenticationPrincipal -> 토큰에서 검증된 사용자 주입, 클라이언트 전달 id 위변조 차단
            @AuthenticationPrincipal AuthUser authUser) {

        return chatService.connectSession(sessionId, authUser.id());
    }

    @Operation (
            summary = "채팅 메시지 전송",
            description = "사용자가 채팅 메시지를 서버로 전송합니다.\n" +
                    "- 서버는 이 메시지를 처리하고 응답을 생성합니다."
    )
    // 메시지 전송 (사용자 → 서버) POST /api/chat/message
    // 본문 없이 200 만 반환 -> 실제 LLM 응답은 SSE 스트림으로 가므로 HTTP 응답은 접수 확인만
    @PostMapping("/message")
    public ResponseEntity<Void> sendMessage(
            @RequestBody ChatSendRequest req,
            @AuthenticationPrincipal AuthUser authUser) {

        chatService.processMessage(req, authUser.id());

        return ResponseEntity.ok().build();
    }

    @Operation (
            summary = "채팅 메시지 저장",
            description = "채팅 내역을 서버에 저장합니다."
    )
    // 대화 저장 (Redis → PostgreSQL) -> 휘발성 버퍼 내용을 사용자가 원하는 시점에 영구 보관
    @PostMapping("/save-message")
    public ResponseEntity<Void> saveConversation(
            @RequestBody ChatSaveRequest req,
            @AuthenticationPrincipal AuthUser authUser) {

        chatService.saveMessages(req, authUser.id());

        return ResponseEntity.ok().build();
    }

    @Operation (
            summary = "채팅 세션 해제",
            description = "특정 채팅 세션을 명시적으로 해제합니다.\n" +
                    "- 이 작업은 세션과 관련된 리소스를 정리하는 데 사용됩니다."
    )
    // 명시적 세션 해제
    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnectSession(
            @RequestParam Long sessionId,
            @AuthenticationPrincipal AuthUser authUser) {
        chatService.disconnectSession(sessionId, authUser.id());

        return ResponseEntity.ok().build();
    }

    @Operation (
            summary = "오프너 분석 요청",
            description = "오프너 분석을 서버에 요청합니다."
    )
    // 오프너 분석
    @PostMapping("/analysis")
    public ResponseEntity<Void> openerAnalysis(
            @RequestBody OpenerAnalysisRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        chatService.openerAnalysis(req, authUser.id());

        return ResponseEntity.ok().build();
    }

    /**
     * RAG 기반 변형 문제 생성
     */
    @Operation(
            summary = "RAG 기반 변형 문제 생성",
            description = "주어진 원본 문제를 바탕으로 RAG(검색-생성 통합) 방식을 활용하여 변형 문제를 생성합니다.\n" +
                    "- 사용자는 원본 문제와 원하는 변형 유형을 제공하며, 시스템은 관련 자료를 검색하고 새로운 문제를 생성합니다."

    )
    @PostMapping("/generate")
    public ResponseEntity<GenerateQuestionResponse> generateVariantQuestion(
            @Valid @RequestBody GenerateQuestionRequest req,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        GenerateQuestionResponse response = questionNewService.generateQuestion(
                req,
                authUser.id()
        );

        return ResponseEntity.ok(response);
    }
}
