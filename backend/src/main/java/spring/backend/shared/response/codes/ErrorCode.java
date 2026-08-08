package spring.backend.shared.response.codes;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Auth 관련 에러 (A_xxx)
    DUPLICATE_EMAIL(409, "A_001", "이미 사용 중인 이메일입니다"),
    INVALID_CREDENTIALS(401, "A_002", "이메일 또는 비밀번호가 일치하지 않습니다"),
    DEACTIVATED_USER(403, "A_003", "비활성화된 계정입니다"),
    UNAUTHORIZED(401, "A_004", "인증이 필요합니다"),
    TOKEN_EXPIRED(401, "A_005", "토큰이 만료되었습니다"),
    INVALID_TOKEN(401, "A_006", "유효하지 않은 토큰입니다"),
    INVALID_REFRESH_TOKEN(401, "A_007", "유효하지 않은 Refresh Token입니다"),
    REFRESH_TOKEN_NOT_FOUND(401, "A_008", "Refresh Token을 찾을 수 없습니다"),
    USER_NOT_FOUND(404, "A_009", "사용자를 찾을 수 없습니다"),
    OAUTH_ACCESS_TOKEN_ERROR(500, "A_010", "OAuth Access Token 발급에 실패했습니다"),
    OAUTH_USER_INFO_ERROR(500, "A_011", "OAuth 사용자 정보 조회에 실패했습니다"),
    INVALID_OAUTH_PROVIDER(400, "A_012", "지원하지 않는 OAuth 제공자입니다"),
    BLACKLISTED_TOKEN(401, "A_013", "블랙리스트에 등록된 토큰입니다"),
    PASSWORD_MISMATCH(400, "A_014", "현재 비밀번호가 일치하지 않습니다"),
    ACCOUNT_LOCKED(423, "A_015", "계정이 잠겼습니다"),
    ALREADY_REGISTERED_USER(409, "A_016", "이미 가입된 사용자입니다"),
    INVALID_SIGNUP_TOKEN(400, "A_017", "유효하지 않은 회원가입 토큰입니다"),

    // Client/Validation 에러 (V_xxx)
    INVALID_INPUT(400, "V_001", "입력값이 올바르지 않습니다"),
    MISSING_PARAMETER(400, "V_002", "필수 파라미터가 누락되었습니다"),
    METHOD_NOT_ALLOWED(405, "V_003", "지원하지 않는 HTTP 메서드입니다"),

    // Business Logic 에러 (B_xxx)
    RESOURCE_NOT_FOUND(404, "B_001", "요청한 리소스를 찾을 수 없습니다"),
    RESOURCE_CONFLICT(409, "B_002", "리소스 충돌이 발생했습니다"),
    AUTHORIZATION_FAILED(403, "B_003", "권한이 없습니다"),

    // Server 에러 (S_xxx)
    INTERNAL_SERVER_ERROR(500, "S_001", "서버 내부 오류가 발생했습니다"),
    DATABASE_ERROR(500, "S_002", "데이터베이스 오류가 발생했습니다"),

    // chatting 에러 (C_xxx)
    INVALID_SESSION(400, "C_001", "사용자의 세션 접근권한이 없습니다."),
    SESSION_INITIALIZE_FAIL(500, "C_002", "세션 초기화에 실패하였습니다."),
    SESSION_EXPIRED(401, "C_003", "만료된 세션입니다."),
    MESSAGE_INPUT_FAIL(500, "C_004", "Redis 메세지 적재에 실패했습니다."),
    NO_MESSAGE_STORED(400, "C_005", "저장할 채팅 내용이 없습니다."),
    LLM_RESPONSE_FAIL(502, "C_006", "LLM 응답에 실패하였습니다."),
    INVALID_QUESTION(400, "C_007", "변형문제를 생성할 권한이 없습니다."),
    LLM_TIMEOUT(408, "C_008", "LLM 응답 시간이 초과되었습니다."),
    LLM_GENERATE_FAIL(502, "C_009", "변형문제 생성에 실패하였습니다."),
    LLM_CIRCUIT_OPEN(503, "C_010", "LLM 서비스가 일시적으로 이용 불가능합니다. 잠시 후 다시 시도해주세요."),
    REDIS_CIRCUIT_OPEN(503, "C_011", "채팅 저장소가 일시적으로 이용 불가능합니다."),
    RAG_DISABLED(503, "C_012", "변형문제 생성 기능이 비활성화되어 있습니다."),
    // INVALID_SESSION(400) 에 뭉쳐 있던 두 사유를 분리한다
    // 400 은 요청 형식 오류를 뜻하는데 실제로는 인가 실패(403)와 대상 부재(404)였다
    SESSION_ACCESS_DENIED(403, "C_013", "세션 접근 권한이 없습니다."),
    SESSION_NOT_FOUND(404, "C_014", "세션을 찾을 수 없습니다."),
    // TTL 이 남았는데 버퍼가 비었다 = 복제 유실·LRU 축출. 조용히 넘기면 유실을 유실로 알 수 없다
    CHAT_BUFFER_LOST(500, "C_015", "채팅 버퍼가 예기치 않게 비어 있습니다."),

    // Can 관련 에러 (N_xxx)
    INSUFFICIENT_CANS(400, "N_001", "CAN이 부족합니다"),
    CAN_NOT_FOUND(404, "N_002", "사용자의 CAN 정보를 찾을 수 없습니다"),

    // Exam 관련 에러 (E_xxx)
    EXAM_NOT_FOUND(404, "E_001", "시험을 찾을 수 없습니다"),

    // Exam Result 관련 에러 (R_xxx)
    RESULT_ALREADY_SUBMITTED(400, "R_001", "이미 제출된 시험 결과입니다"),
    RESULT_NOT_FOUND(404, "R_002", "시험 결과를 찾을 수 없습니다"),

    // Question Result 관련 에러 (QR_xxx)
    QUESTION_RESULT_NOT_FOUND(404, "QR_001", "문제 결과를 찾을 수 없습니다"),

    // Question 관련 에러 (Q_xxx)
    QUESTION_NOT_FOUND(404, "Q_001", "문제를 찾을 수 없습니다"),
    QUESTION_NOT_IN_EXAM(400, "Q_002", "문제가 해당 시험에 속하지 않습니다"),
    QUESTION_HAS_NO_PASSAGES(400, "Q_003", "문제에 지문이 없습니다"),

    // Scrapbook 관련 에러 (S_xxx)
    SCRAPBOOK_NOT_FOUND(404, "S_001", "스크랩북을 찾을 수 없습니다"),
    SCRAPBOOK_DETAIL_NOT_FOUND(404, "S_002", "스크랩북 상세보기를 찾을 수 없습니다"),
    SCRAPBOOK_CHAT_HISTORY_NOT_FOUND(404, "S_003", "스크랩북 채팅 기록을 찾을 수 없습니다");


    private final int status;
    private final String code;
    private final String message;
}
