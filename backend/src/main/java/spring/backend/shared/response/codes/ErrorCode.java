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

  // Client/Validation 에러 (C_xxx)
  INVALID_INPUT(400, "C_001", "입력값이 올바르지 않습니다"),
  MISSING_PARAMETER(400, "C_002", "필수 파라미터가 누락되었습니다"),

  // Business Logic 에러 (B_xxx)
  RESOURCE_NOT_FOUND(404, "B_001", "요청한 리소스를 찾을 수 없습니다"),
  RESOURCE_CONFLICT(409, "B_002", "리소스 충돌이 발생했습니다"),

  // Exam 관련 에러 (E_xxx)
  EXAM_NOT_FOUND(404, "E_001", "시험을 찾을 수 없습니다"),

  // Exam Result 관련 에러 (R_xxx)
  RESULT_ALREADY_SUBMITTED(400, "R_001", "이미 제출된 시험 결과입니다"),
  RESULT_NOT_FOUND(404, "R_002", "시험 결과를 찾을 수 없습니다"),

  // Question 관련 에러 (Q_xxx)
  QUESTION_NOT_FOUND(404, "Q_001", "문제를 찾을 수 없습니다"),
  QUESTION_NOT_IN_EXAM(400, "Q_002", "문제가 해당 시험에 속하지 않습니다"),

  // Server 에러 (S_xxx)
  INTERNAL_SERVER_ERROR(500, "S_001", "서버 내부 오류가 발생했습니다"),
  DATABASE_ERROR(500, "S_002", "데이터베이스 오류가 발생했습니다");


  private final int status;
  private final String code;
  private final String message;
}
