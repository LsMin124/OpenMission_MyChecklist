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
import java.util.*;
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

    // task 목록 조회
    public Map<String, List<TaskResponse>> getTaskSchedule(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<Task> allTasks = taskRepository.findAllByUser(user);

        // 오늘 완료된 기록 조회
        List<TaskCompletion> completions = taskCompletionRepository.findByTaskUserAndCompletionDate(user, date);
        Set<Long> completedTaskIds = completions.stream()
                .map(tc -> tc.getTask().getId())
                .collect(Collectors.toSet());

        List<TaskResponse> todayList = new ArrayList<>();
        List<TaskResponse> upcomingList = new ArrayList<>();

        for (Task task : allTasks) {
            // 1. 반복(RECURRING) 작업: 오늘 해당되면 'Today'에 추가
            if (task.getTaskType() == TaskType.RECURRING) {
                if ("DAILY".equals(task.getRecurrenceRule())) {
                    todayList.add(TaskResponse.from(task, completedTaskIds.contains(task.getId())));
                }
            }
            // 2. 일회성(ONE_TIME) 작업
            else if (task.getTaskType() == TaskType.ONE_TIME) {
                LocalDate due = task.getDueDate();
                boolean isCompletedToday = completedTaskIds.contains(task.getId());

                // A. 마감일이 오늘이거나, 과거인데 아직 안 한 경우 -> 'Today' (잔소리 모드)
                if ((due.isEqual(date) || due.isBefore(date)) && !isCompletedToday) {
                    todayList.add(TaskResponse.from(task, false));
                }
                // B. 마감일이 오늘인데 이미 완료한 경우 -> 'Today' (성취감용)
                else if (due.isEqual(date) && isCompletedToday) {
                    todayList.add(TaskResponse.from(task, true));
                }
                // C. 마감일이 미래인 경우 -> 'Upcoming' (미래 준비)
                else if (due.isAfter(date)) {
                    // 미래 일감은 완료 여부가 의미 없으므로 false, 날짜순 정렬을 위해 나중에 처리
                    upcomingList.add(TaskResponse.from(task, false));
                }
            }
        }

        // Upcoming 리스트는 마감일 임박순으로 정렬
        upcomingList.sort((t1, t2) -> t1.getDueDate().compareTo(t2.getDueDate()));

        Map<String, List<TaskResponse>> result = new HashMap<>();
        result.put("today", todayList);
        result.put("upcoming", upcomingList);

        return result;
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
