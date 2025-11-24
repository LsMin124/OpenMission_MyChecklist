package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.exception.SuccessCode;
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
import java.util.Map;

@RestController
@RequestMapping("/todo/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    // Task 등록
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody @Valid TaskRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long taskId = taskService.createTask(userId, request);
        return ApiResponse.success(SuccessCode.CREATE_SUCCESS, taskId);
    }

    // 오늘 task 목록 조회
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Map<String, List<TaskResponse>>>> getTasksForToday(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) LocalDate date) {
        Long userId = Long.parseLong(userDetails.getUsername());
        if (date == null) date = LocalDate.now();
        Map<String, List<TaskResponse>> schedule = taskService.getTaskSchedule(userId, date);
        return ApiResponse.success(SuccessCode.SELECT_SUCCESS, schedule);
    }

    // 작업 완료 처리
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestParam(required = false) LocalDate date) {
        Long userId = Long.parseLong(userDetails.getUsername());
        if (date == null) {
            date = LocalDate.now();
        }
        taskService.completeTask(userId, taskId, date);
        return ApiResponse.success(SuccessCode.UPDATE_SUCCESS);
    }

    // task 완료 취소
    @DeleteMapping("/{taskId}/complete")
    public ResponseEntity<ApiResponse<Void>> cancelTaskCompletion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestParam(required = false) LocalDate date) {

        Long userId = Long.parseLong(userDetails.getUsername());
        if (date == null) date = LocalDate.now();

        taskService.cancelTaskCompletion(userId, taskId, date);
        return ApiResponse.success(SuccessCode.UPDATE_SUCCESS);
    }

    // task 삭제
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {

        Long userId = Long.parseLong(userDetails.getUsername());
        taskService.deleteTask(userId, taskId);

        return ApiResponse.success(SuccessCode.DELETE_SUCCESS);
    }
}
