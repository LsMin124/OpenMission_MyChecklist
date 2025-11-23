package com.example.demo.controller;

import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/todo/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // 추후 인증기능 추가 전까진 테스트용 id 사용
    private static final Long TEMP_USER_ID =1L;

    // Task 등록
    @PostMapping
    public ResponseEntity<Long> createTask(@RequestBody @Valid TaskRequest request) {
        Long taskId = taskService.createTask(TEMP_USER_ID, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskId);
    }

    // 오늘 task 목록 조회
    @GetMapping("/today")
    public ResponseEntity<List<TaskResponse>> getTasksForToday(
            @RequestParam(required = false) LocalDate date) {

        if (date == null) {
            date = LocalDate.now();
        }

        List<TaskResponse> responses = taskService.getTasksForToday(TEMP_USER_ID, date);
        return ResponseEntity.ok(responses);
    }

    // 작업 완료 처리
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Long> completeTask(
            @PathVariable Long taskId,
            @RequestParam(required = false) LocalDate date) {

        if (date == null) {
            date = LocalDate.now();
        }

        taskService.completeTask(TEMP_USER_ID, taskId, date);
        return ResponseEntity.ok().build();
    }

}
