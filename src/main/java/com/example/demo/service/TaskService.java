package com.example.demo.service;

import com.example.demo.domain.Task;
import com.example.demo.domain.TaskCompletion;
import com.example.demo.domain.TaskType;
import com.example.demo.domain.User;
import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.exception.CustomException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.TaskCompletionRepository;
import com.example.demo.repository.TaskRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.util.RecurrenceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 조회 전용 (성능 최적화)
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final UserRepository userRepository;

    // task 생성
    @Transactional
    public Long createTask(Long userId, TaskRequest request) {
        User user = getUserOrThrow(userId);
        Task task = request.toEntity(user);
        return taskRepository.save(task).getId();
    }

    // task 완료처리
    @Transactional
    public void completeTask(Long userId, Long taskId, LocalDate date) {
        Task task = getTaskOrThrow(taskId);
        validateOwner(task, userId);

        // 이미 완료된 기록이 없으면 새로 생성 (중복 방지)
        if (!taskCompletionRepository.existsByTaskAndCompletionDate(task, date)) {
            TaskCompletion completion = TaskCompletion.builder()
                    .task(task)
                    .completionDate(date)
                    .build();
            taskCompletionRepository.save(completion);
        }
    }

    // task 완료 취소
    @Transactional
    public void cancelTaskCompletion(Long userId, Long taskId, LocalDate date) {
        Task task = getTaskOrThrow(taskId);
        validateOwner(task, userId);

        // 해당 날짜의 완료 기록을 찾아서 삭제
        taskCompletionRepository.findByTaskAndCompletionDate(task, date)
                .ifPresent(taskCompletionRepository::delete);
    }

    // task 삭제
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = getTaskOrThrow(taskId);
        validateOwner(task, userId);

        // 1. 연관된 완료 기록 모두 삭제
        taskCompletionRepository.deleteAllByTask(task);
        // 2. 할 일 원본 삭제
        taskRepository.delete(task);
    }


    // 조회
    public Map<String, List<TaskResponse>> getTaskSchedule(Long userId, LocalDate date) {
        User user = getUserOrThrow(userId);
        List<Task> allTasks = taskRepository.findAllByUser(user);

        // 성능 최적화: 오늘 날짜에 완료된 Task ID들을 한 번에 조회 (Set으로 변환하여 검색 속도 O(1) 보장)
        Set<Long> completedTaskIds = getCompletedTaskIds(user, date);

        List<TaskResponse> todayList = new ArrayList<>();
        List<TaskResponse> upcomingList = new ArrayList<>();

        for (Task task : allTasks) {
            boolean isCompleted = completedTaskIds.contains(task.getId());

            // 1. 오늘 리스트: RecurrenceUtils에게 판단 위임 (관심사 분리)
            if (RecurrenceUtils.isScheduledForDate(task, date, isCompleted)) {
                todayList.add(TaskResponse.from(task, isCompleted));
            }

            // 2. 다가오는 일정: 일회성 작업 중 마감일이 미래인 것
            else if (task.getTaskType() == TaskType.ONE_TIME && task.getDueDate().isAfter(date)) {
                upcomingList.add(TaskResponse.from(task, false));
            }
        }

        // 다가오는 일정은 날짜순 정렬
        upcomingList.sort(Comparator.comparing(TaskResponse::getDueDate));

        return Map.of("today", todayList, "upcoming", upcomingList);
    }

    // 헬퍼 메서드
    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private Task getTaskOrThrow(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));
    }

    private void validateOwner(Task task, Long userId) {
        if (!task.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TASK_ACCESS_DENIED);
        }
    }

    private Set<Long> getCompletedTaskIds(User user, LocalDate date) {
        return taskCompletionRepository.findByTaskUserAndCompletionDate(user, date).stream()
                .map(tc -> tc.getTask().getId())
                .collect(Collectors.toSet());
    }
}