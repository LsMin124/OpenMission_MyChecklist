package com.example.demo.controller;

import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/todo/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // Task 등록
    @PostMapping
    public ResponseEntity<Long> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid TaskRequest request) {

        Long userId = Long.parseLong(userDetails.getUsername());

        Long taskId = taskService.createTask(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(taskId);
    }

    // 오늘 task 목록 조회
    @GetMapping("/today")
    public ResponseEntity<List<TaskResponse>> getTasksForToday(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) LocalDate date) {

        Long userId = Long.parseLong(userDetails.getUsername());

        if (date == null) {
            date = LocalDate.now();
        }

        List<TaskResponse> responses = taskService.getTasksForToday(userId, date);
        return ResponseEntity.ok(responses);
    }

    // 작업 완료 처리
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<Long> completeTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestParam(required = false) LocalDate date) {

        Long userId = Long.parseLong(userDetails.getUsername());

        if (date == null) {
            date = LocalDate.now();
        }

        taskService.completeTask(userId, taskId, date);
        return ResponseEntity.ok().build();
    }

}
