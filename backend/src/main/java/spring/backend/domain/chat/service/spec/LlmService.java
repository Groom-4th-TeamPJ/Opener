package spring.backend.domain.chat.service.spec;

import java.util.function.Consumer;

public interface LlmService {

  //LLM API 스트리밍 호출
  void chatStream(String sessionId, String userMessage, Consumer<String> chunkConsumer);
}
