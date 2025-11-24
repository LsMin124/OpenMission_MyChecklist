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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // dto 사용
        Task task = request.toEntity(user);

        return taskRepository.save(task).getId();
    }

    // task 목록 조회
    public Map<String, List<TaskResponse>> getTaskSchedule(Long userId, LocalDate date) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

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

                // A. 마감일이 오늘이거나, 과거인데 아직 안 한 경우 -> 'Today'
                if ((due.isEqual(date) || due.isBefore(date)) && !isCompletedToday) {
                    todayList.add(TaskResponse.from(task, false));
                }
                // B. 마감일이 오늘인데 이미 완료한 경우 -> 'Today'
                else if (due.isEqual(date) && isCompletedToday) {
                    todayList.add(TaskResponse.from(task, true));
                }
                // C. 마감일이 미래인 경우 -> 'Upcoming'
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
    private boolean shouldShowTaskOnDate(Task task, LocalDate date, Set<Long> completedTaskIds) {
        if (task.getTaskType() == TaskType.ONE_TIME) {
            // 일회성 로직은 기존과 동일
            if (task.getDueDate().isEqual(date)) return true;
            return task.getDueDate().isBefore(date) && !completedTaskIds.contains(task.getId());
        } else {
            // recurring 확장
            String rule = task.getRecurrenceRule();
            if (rule == null) return false;

            // 1. 매일 반복
            if ("DAILY".equals(rule)) {
                return true;
            }

            // 2. N일 마다 반복 (예: "EVERY_N_DAYS:3")
            if (rule.startsWith("EVERY_N_DAYS:")) {
                try {
                    int n = Integer.parseInt(rule.split(":")[1]);
                    // 기준일(생성일)과 조회 날짜의 차이를 구함
                    LocalDate startDate = task.getCreatedAt().toLocalDate();
                    long daysBetween = ChronoUnit.DAYS.between(startDate, date);

                    // 과거 날짜는 안 보여줌 && N일 간격으로 딱 떨어지는 날인지 확인
                    return daysBetween >= 0 && daysBetween % n == 0;
                } catch (Exception e) {
                    return false; // 파싱 에러 시 무시
                }
            }

            // 3. 매월 반복 (예: "MONTHLY:25")
            if (rule.startsWith("MONTHLY:")) {
                try {
                    int dayOfMonth = Integer.parseInt(rule.split(":")[1]);
                    // 오늘이 그 날짜인지 확인
                    return date.getDayOfMonth() == dayOfMonth;
                } catch (Exception e) {
                    return false;
                }
            }

            return false;
        }
    }

    @Transactional
    public void completeTask(Long userId, Long taskId, LocalDate date) {

        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        if (!task.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TASK_ACCESS_DENIED);
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
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!task.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TASK_ACCESS_DENIED);
        }

        // 완료 기록을 찾아서 삭제
        taskCompletionRepository.findByTaskAndCompletionDate(task, date)
                .ifPresent(taskCompletionRepository::delete);
    }

    // task 삭제
    @Transactional
    public void deleteTask(Long userId, Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new CustomException(ErrorCode.TASK_NOT_FOUND));

        if (!task.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.TASK_ACCESS_DENIED);
        }

        // 1. 연관된 완료 기록들 먼저 싹 삭제 (참조 무결성 유지)
        taskCompletionRepository.deleteAllByTask(task);

        // 2. 할 일(규칙) 삭제
        taskRepository.delete(task);
    }
}
