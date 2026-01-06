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

  // Report 관련 에러 (R_xxx)
  REPORT_NOT_FOUND(404, "R_001", "제보를 찾을 수 없습니다"),
  REPORT_ACCESS_DENIED(403, "R_002", "본인의 제보만 접근할 수 있습니다"),
  REPORT_UPDATE_DENIED(403, "R_003", "본인의 제보만 수정할 수 있습니다"),
  REPORT_DELETE_DENIED(403, "R_004", "본인의 제보만 삭제할 수 있습니다"),
  REPORT_NOT_OWNER(403, "R_005", "본인의 제보가 아니거나 존재하지 않습니다"),
  REPORT_ADMIN_REPLY_REQUIRED(400, "R_006", "승인 또는 반려 시 관리자 답변은 필수입니다"),

  // Review 관련 에러 (V_xxx - Review의 V)
  REVIEW_NOT_FOUND(404, "V_001", "리뷰를 찾을 수 없습니다"),
  REVIEW_UPDATE_DENIED(403, "V_002", "본인의 리뷰만 수정할 수 있습니다"),
  REVIEW_DELETE_DENIED(403, "V_003", "본인의 리뷰만 삭제할 수 있습니다"),

  // Path Domain Error
  PATH_SERVICE_UNAVAILABLE_AREA(400, "P_001", "길찾기 서비스를 제공할 수 없는 구간입니다. 너무 멀거나 제공할 수 없습니다."),
  PATH_FIND_UNAVAILABLE(500, "P_002", "길찾기 기능을 이용할 수 없습니다."),
  PATH_TMAP_API_ERROR(500, "P_003", "Tmap 길찾기 api를 사용할 수 없습니다."),
  PATH_KAKAO_API_ERROR(500, "P_004", "Kakao 주소변환 api를 사용할 수 없습니다."),

  // Server 에러 (S_xxx)
  INTERNAL_SERVER_ERROR(500, "S_001", "서버 내부 오류가 발생했습니다"),
  DATABASE_ERROR(500, "S_002", "데이터베이스 오류가 발생했습니다"),

  // Relation 에러(P_xxx)
  RELATION_NOT_FOUND(404, "P_001", "보호자 - 사용자 관계를 찾을 수 없습니다."),
  RELATION_EXISTS(403, "P_002", "이미 관계가 존재하는 사용자입니다.");

  private final int status;
  private final String code;
  private final String message;
}
