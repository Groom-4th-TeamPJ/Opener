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

    // Client/Validation 에러 (V_xxx)
    INVALID_INPUT(400, "V_001", "입력값이 올바르지 않습니다"),
    MISSING_PARAMETER(400, "V_002", "필수 파라미터가 누락되었습니다"),

    // Business Logic 에러 (B_xxx)
    RESOURCE_NOT_FOUND(404, "B_001", "요청한 리소스를 찾을 수 없습니다"),
    RESOURCE_CONFLICT(409, "B_002", "리소스 충돌이 발생했습니다"),

    // Server 에러 (S_xxx)
    INTERNAL_SERVER_ERROR(500, "S_001", "서버 내부 오류가 발생했습니다"),
    DATABASE_ERROR(500, "S_002", "데이터베이스 오류가 발생했습니다"),

    // chatting 에러 (C_xxx)
    INVALID_SESSION(400, "C_001", "사용자의 세션 접근권한이 없습니다."),
    SESSION_INITIALIZE_FAIL(500, "C_002", "세션 초기화에 실패하였습니다."),
    SESSION_EXPIRED(401, "C_003", "만료된 세션입니다."),
    MESSAGE_INPUT_FAIL(401, "C_004", "Redis 메세지 적재에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
