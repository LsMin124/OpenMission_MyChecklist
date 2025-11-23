package com.example.demo.service;

import com.example.demo.domain.Task;
import com.example.demo.domain.TaskType;
import com.example.demo.domain.User;
import com.example.demo.dto.TaskRequest;
import com.example.demo.repository.TaskCompletionRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

// 가짜 db를 사용한 테스트
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    // 가짜 레포지토리
    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskCompletionRepository taskCompletionRepository;

    @Mock
    private UserRepository userRepository;

    // 가짜 레포들을 주입
    @InjectMocks
    private TaskService taskService;

    @Test
    @DisplayName("task 생성 시 레포지토리에 저장이 호출됨을 확인")
    void createTask() {
        Long userId = 1L;
        User user = User.builder().email("test.gmail.com").build();

        TaskRequest request = TaskRequest.builder()
                .title("운동")
                .taskType(TaskType.ONE_TIME)
                .dueDate(LocalDate.now())
                .build();

        Task task = Task.builder()
                .title("운동")
                .user(user)
                .build();

        // 엔티티 캡슐화로 id 접근이 안돼서 이렇게 함...
        ReflectionTestUtils.setField(task, "id", 100L);

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(taskRepository.save(any(Task.class))).willReturn(task);

        Long taskId = taskService.createTask(userId, request);

        assertThat(taskId).isEqualTo(100L);
    }
}
