package com.example.demo.dto;

import com.example.demo.domain.Task;
import com.example.demo.domain.TaskType;
import com.example.demo.domain.User;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class TaskRequest {

    @NotBlank(message = "제목은 필수사항입니다.")
    private String title;

    private String description;

    @NotNull(message = "작업유형은 필수사항입니다.")
    private TaskType taskType;

    @FutureOrPresent(message = "마감일은 과거일 수 없습니다.")
    private LocalDate dueDate;

    private String recurrenceRule;

    @Builder
    public TaskRequest(String title, String description, TaskType taskType, LocalDate dueDate, String recurrenceRule) {
        this.title = title;
        this.description = description;
        this.taskType = taskType;
        this.dueDate = dueDate;
        this.recurrenceRule = recurrenceRule;
    }

    public Task toEntity(User user) {
        return Task.builder()
                .user(user)
                .title(title)
                .description(description)
                .taskType(taskType)
                .dueDate(dueDate)
                .recurrenceRule(recurrenceRule)
                .build();
    }
}
