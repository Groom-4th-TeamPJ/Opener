package spring.backend.domain.scrapbook.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;
import spring.backend.domain.scrapbook.dto.response.ScrapbookResponse;
import spring.backend.domain.scrapbook.dto.response.detail.ScrapbookDetailResponse;
import spring.backend.domain.scrapbook.service.spec.ScrapbookService;
import spring.backend.shared.infrastructure.security.dto.AuthUser;
import spring.backend.shared.response.codes.ErrorCode;
import spring.backend.shared.response.exception.BusinessException;

import java.util.List;

@RestController
@RequestMapping("/scrapbooks")
@Tag(name = "\uD83D\uDCD2 Scrapbook", description = "스크랩북")
public class ScrapbookController {

    private final ScrapbookService scrapbookService;

    public ScrapbookController(ScrapbookService scrapbookService) {
        this.scrapbookService = scrapbookService;
    }

    @Operation(
            summary = "스크랩북 분류 조회"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스크랩북 리스트 조회 성공",
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

    @Operation(
            summary = "스크랩북 분류 선택 후 리스트 조회"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스크랩북 필터 검색 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ScrapbookResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/{examId}")
    public ScrapbookResponse getScrapbookQuestions(
            @PathVariable Long examId,
            @PageableDefault(size = 8, sort = "openerUsedAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal AuthUser authUser
    ) {
        if (authUser == null || authUser.id() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return scrapbookService.getScrapbookContents(authUser.id(), examId, pageable);
    }

    @Operation(
            summary = "스크랩북 문제 상세 조회"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "스크랩북 문제 상세 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ScrapbookDetailResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "403", description = "인가 실패"),
            @ApiResponse(responseCode = "404", description = "스크랩북 문제를 찾을 수 없음")
    })
    @GetMapping("/question-results/{questionResultId}")
    public ScrapbookDetailResponse getScrapbookDetails(
            @PathVariable Long questionResultId,
            @AuthenticationPrincipal AuthUser authUser) {
        if (authUser == null || authUser.id() == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return scrapbookService.getScrapbookDetail(authUser.id(), questionResultId);
    }
}
