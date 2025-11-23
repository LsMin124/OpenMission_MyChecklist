package com.example.demo.dto;

import com.example.demo.domain.Task;
import com.example.demo.domain.TaskType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private TaskType taskType;
    private LocalDate dueDate;      // 일회성용
    private String recurrenceRule;  // 주기성용
    private boolean isCompleted;

    // 엔티티 -> DTO 변환 메서드
    public static TaskResponse from(Task task, boolean isCompleted) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .taskType(task.getTaskType())
                .dueDate(task.getDueDate())
                .recurrenceRule(task.getRecurrenceRule())
                .isCompleted(isCompleted)
                .build();
    }
}