package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Auditable;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Task extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType;

    private LocalDate dueDate;

    private String recurrenceRule;

    @Builder
    public Task(User user,  String title, String description, TaskType taskType, LocalDate dueDate, String recurrenceRule) {
        this.user = user;
        this.title = title;
        this.description = description;
        this.taskType = taskType;
        this.dueDate = dueDate;
        this.recurrenceRule = recurrenceRule;
    }
}
