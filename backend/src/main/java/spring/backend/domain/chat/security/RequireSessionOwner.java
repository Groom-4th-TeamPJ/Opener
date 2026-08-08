package spring.backend.domain.chat.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 이 애너테이션이 붙은 메서드는 진입 전에 반드시 세션 소유자 검증을 통과한다
// 호출부마다 검증을 적는 방식은 진입점이 늘어날 때 누락을 막을 구조가 없다
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireSessionOwner {
}
