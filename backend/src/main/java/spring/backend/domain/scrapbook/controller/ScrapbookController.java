package spring.backend.domain.scrapbook.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.backend.domain.scrapbook.dto.request.ScrapbookFilterSearchRequest;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;
import spring.backend.domain.scrapbook.service.spec.ScrapbookService;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.response.PageResponse;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import java.util.List;

@RestController
@RequestMapping("/scrapbooks")
public class ScrapbookController {

    private final ScrapbookService scrapbookService;

    public ScrapbookController(ScrapbookService scrapbookService) {
        this.scrapbookService = scrapbookService;
    }

    @Operation(
            summary = "스크랩북 분류 조회"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스크랩북 필터 조회 성공",
                        content = @Content(mediaType = "application/json",
                               schema = @Schema(implementation = ScrapbookFilterResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/filters")
    public List<ScrapbookFilterResponse> getScrapbookFilters(@AuthenticationPrincipal AuthUser authUser) {
        if (authUser == null || authUser.id() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return scrapbookService.getScrapbookFilters(authUser.id());
    }
}
