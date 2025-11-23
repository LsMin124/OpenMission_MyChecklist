package com.example.demo.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "task_completions",
    indexes = @Index(name = "idx_completion_date", columnList = "completionDate"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TaskCompletion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // 실무에선 지연로딩을 해야 쿼리 낭비가 없다고 함 - 연관 데이터는 필요할때 가져오기
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @Column(nullable = false)
    private LocalDate completionDate;

    @Builder
    public TaskCompletion(Task task, LocalDate completionDate) {
        this.task = task;
        this.completionDate = completionDate;
    }
}
