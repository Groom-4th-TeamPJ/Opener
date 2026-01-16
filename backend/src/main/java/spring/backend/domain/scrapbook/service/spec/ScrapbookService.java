package spring.backend.domain.scrapbook.service.spec;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import spring.backend.domain.scrapbook.dto.request.ScrapbookFilterSearchRequest;
import spring.backend.domain.scrapbook.dto.response.ScrapbookFilterResponse;

import java.util.List;
import java.util.UUID;

public interface ScrapbookService {

    List<ScrapbookFilterResponse> getScrapbookFilters(UUID userId);
}
