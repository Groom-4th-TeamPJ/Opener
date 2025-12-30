package spring.backend.shared.response.format;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetailFormat {
  private String field;
  private Object rejectedValue;
  private String reason;
  private String code;
}
