package spring.backend.domain.can.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "캔 응답")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanResponse {
    int currentCan;
}
