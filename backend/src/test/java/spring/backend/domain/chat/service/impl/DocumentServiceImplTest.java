package spring.backend.domain.chat.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

// [asy] 는 문제의 그림을 그리는 Asymptote 소스 코드
// 블록을 가진 문서에서 글자수의 평균 48% 를 차지해 임베딩 벡터가 좌표·draw 명령 쪽으로 기울고,
// 자연어 한 문장인 질문과 맞물리지 않아 임계값 0.7 에서 20문항 전부 검색 0건이었다
class DocumentServiceImplTest {

    @Test
    @DisplayName("Asymptote 블록을 지우고 문제 본문만 남긴다")
    void stripAsyBlocks_그림소스_본문만남는다() {
        String raw = """
                연 ABCD 의 넓이는 얼마입니까?

                [asy]
                size(150); draw((-5,0)--(15,0),Arrows(4));
                pair A=(0,7),B=(1,0); label("$A$",A,W);
                [/asy]""";

        String stripped = DocumentServiceImpl.stripAsyBlocks(raw);

        assertEquals("연 ABCD 의 넓이는 얼마입니까?", stripped);
        assertFalse(stripped.contains("draw"));
    }

    @Test
    @DisplayName("한 문서에 블록이 여러 개여도 전부 지우고 사이 문장은 남긴다")
    void stripAsyBlocks_블록다수_사이문장보존() {
        String raw = "첫 그림 [asy]draw(A--B);[/asy] 가운데 설명 [asy]draw(C--D);[/asy] 마지막 질문";

        String stripped = DocumentServiceImpl.stripAsyBlocks(raw);

        assertFalse(stripped.contains("draw"));
        assertTrue(stripped.contains("가운데 설명"));
        assertTrue(stripped.contains("마지막 질문"));
    }

    @Test
    @DisplayName("블록이 없는 문서는 원본 그대로 돌려준다")
    void stripAsyBlocks_블록없음_원본유지() {
        String raw = "30 의 120% 와 20 의 130% 의 양수 차이는 무엇입니까?";

        assertSame(raw, DocumentServiceImpl.stripAsyBlocks(raw));
    }

    // 여는 태그만 있고 닫히지 않은 문서는 손대지 않는다
    // 끝까지 지우면 그림이 아니라 문제 본문이 사라져 색인에서 통째로 누락된다
    @Test
    @DisplayName("닫히지 않은 블록은 지우지 않고 본문을 보존한다")
    void stripAsyBlocks_미종료블록_본문보존() {
        String raw = "삼각형의 넓이는? [asy]draw(A--B);";

        String stripped = DocumentServiceImpl.stripAsyBlocks(raw);

        assertTrue(stripped.contains("삼각형의 넓이는?"));
    }
}
