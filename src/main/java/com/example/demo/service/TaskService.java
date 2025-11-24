package com.example.demo.service;

import com.example.demo.domain.Task;
import com.example.demo.domain.TaskCompletion;
import com.example.demo.domain.TaskType;
import com.example.demo.domain.User;
import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.repository.TaskCompletionRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 디폴트는 읽기 전용
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final TaskCompletionRepository taskCompletionRepository;

    // task 생성
    @Transactional
    public Long createTask(Long userId, TaskRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // dto 사용
        Task task = request.toEntity(user);

        return taskRepository.save(task).getId();
    }

    // 오늘 task 목록 조회
    public List<TaskResponse> getTasksForToday(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // user의 모든 task 조회
        List<Task> allTasks = taskRepository.findAllByUser(user);

        // 오늘 날짜로 된 완료기록 한번에 조회 - set으로 조회속도 최적화
        List<TaskCompletion> completions = taskCompletionRepository.findByTaskUserAndCompletionDate(user, date);
        Set<Long> competedTaskIds = completions.stream()
                .map(tc -> tc.getTask().getId())
                .collect(Collectors.toSet());

        // 필터링 및 dto로 변환
        return allTasks.stream()
                .filter(task -> shouldShowTaskOnDate(task, date, competedTaskIds))
                .map(task -> TaskResponse.from(task, competedTaskIds.contains(task.getId())))
                .collect(Collectors.toList());
    }

    // 해당 날짜에 해당 task의 노출여부 결정 로직
    private boolean shouldShowTaskOnDate(Task task, LocalDate date, Set<Long> competedTaskIds) {

        // task가 일회성인지 확인
        if (task.getTaskType() == TaskType.ONE_TIME) {
            // 오늘이 듀인 경우 - 노출
            if (task.getDueDate().isEqual(date)) {
                return true;
            }
            // 마감일이 지났는데 1. 미완료인 경우 - 노출, 2. 완료된 경우 - 노출하지 않음
            if (task.getDueDate().isBefore(date) && !competedTaskIds.contains(task.getId())) {
                return true;
            }
            return false;
        } else {
            // 일단 daily로 해놓고, 나머지 enum 등으로 추가?
            return "DAILY".equals(task.getRecurrenceRule());
        }
    }

    @Transactional
    public void completeTask(Long userId, Long taskId, LocalDate date) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));

        // 본인 확인 - 나중에 security 적용?
        if (!task.getUser().getId().equals(userId)) {
            throw new IllegalStateException("no authority");
        }

        if (taskCompletionRepository.existsByTaskAndCompletionDate(task, date)) {
            return;
        }

        TaskCompletion completion = TaskCompletion.builder()
                .task(task)
                .completionDate(date)
                .build();

        taskCompletionRepository.save(completion);


    }

    // task 완료 취소
    @Transactional
    public void cancelTaskCompletion(Long userId, Long taskId, LocalDate date) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다."));

        if (!task.getUser().getId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        // 완료 기록을 찾아서 삭제
        TaskCompletion completion = taskCompletionRepository.findByTaskAndCompletionDate(task, date)
                .orElse(null); // 없으면 null

        if (completion != null) {
            taskCompletionRepository.delete(completion);
        }
    }

    // task 삭제
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("작업을 찾을 수 없습니다."));

        if (!task.getUser().getId().equals(userId)) {
            throw new IllegalStateException("권한이 없습니다.");
        }

        // 1. 연관된 완료 기록들 먼저 싹 삭제 (참조 무결성 유지)
        taskCompletionRepository.deleteAllByTask(task);

        // 2. 할 일(규칙) 삭제
        taskRepository.delete(task);
    }
}
