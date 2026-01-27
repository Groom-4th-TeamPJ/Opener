package spring.backend.domain.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.domain.can.dto.response.CanResponse;
import spring.backend.domain.can.service.spec.CanService;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

@Slf4j
@RestController
@RequestMapping("/users")
@Tag(name = "\uD83D\uDC64 User", description = "사용자 정보")
public class UserController {

    private final CanService canService;

    public UserController(CanService canService) {
        this.canService = canService;
    }

    @Operation (
            summary = "내 캔 개수 조회",
            description = "현재 로그인한 사용자의 캔 개수를 조회합니다."
    )
    @GetMapping("me/cans/count")
    public CanResponse getMyCans(@AuthenticationPrincipal AuthUser authUser) {
        if (authUser == null || authUser.id() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return canService.getCurrentCan(authUser.id());
    }
}
