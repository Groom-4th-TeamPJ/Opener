package spring.backend.domain.exam.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import spring.backend.domain.exam.model.entity.Exam;
import spring.backend.domain.exam.model.enums.ExamType;

import java.util.Optional;

public interface JpaExamRepository extends JpaRepository<Exam, Long> {
    Optional<Exam> findByYearAndExamType(Integer year, ExamType examType);
}
