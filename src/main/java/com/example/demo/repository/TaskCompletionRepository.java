package com.example.demo.repository;

import com.example.demo.domain.Task;
import com.example.demo.domain.TaskCompletion;
import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface TaskCompletionRepository extends JpaRepository<TaskCompletion, Long> {

    // 특정 날짜에 해당 작업이 완료되었는지 확인
    boolean existsByTaskAndCompletionDate(Task task, LocalDate date);

    // 특정 날짜의 사용자 완료 기록을 한번에 조회 (N+1 문제 방지)
    List<TaskCompletion> findByTaskUserAndCompletionDate(User user, LocalDate date);
}
