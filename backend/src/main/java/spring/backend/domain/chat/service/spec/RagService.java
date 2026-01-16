package spring.backend.domain.chat.service.spec;

import java.util.function.Consumer;

public interface RagService {
    void generateSimilarProblemStream(String problemContext, Consumer<String> chunkConsumer);
}
