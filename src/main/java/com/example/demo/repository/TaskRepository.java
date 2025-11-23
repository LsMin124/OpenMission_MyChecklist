package com.example.demo.repository;

import com.example.demo.domain.Task;
import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // 사용자의 task 목록 전부 조회 - fetch join 고려?
    List<Task> findAllByUser(User user);
}
