package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        Long taskId = taskService.createTask(getUserId(userDetails), request);
        return ApiResponse.success(SuccessCode.CREATE_SUCCESS, taskId);
    }

    // 오늘 task 목록 조회
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<Map<String, List<TaskResponse>>>> getTasksForToday(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) LocalDate date) {
        Map<String, List<TaskResponse>> schedule = taskService.getTaskSchedule(getUserId(userDetails), getDateOrDefault(date));
        return ApiResponse.success(SuccessCode.SELECT_SUCCESS, schedule);
    }

    // 작업 완료 처리
    @PostMapping("/{taskId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestParam(required = false) LocalDate date) {
        taskService.completeTask(getUserId(userDetails), taskId, getDateOrDefault(date));
        return ApiResponse.success(SuccessCode.UPDATE_SUCCESS);
    }

    // task 완료 취소
    @DeleteMapping("/{taskId}/complete")
    public ResponseEntity<ApiResponse<Void>> cancelTaskCompletion(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId,
            @RequestParam(required = false) LocalDate date) {
        taskService.cancelTaskCompletion(getUserId(userDetails), taskId, getDateOrDefault(date));
        return ApiResponse.success(SuccessCode.UPDATE_SUCCESS);
    }

    // task 삭제
    @DeleteMapping("/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long taskId) {
        taskService.deleteTask(getUserId(userDetails), taskId);
        return ApiResponse.success(SuccessCode.DELETE_SUCCESS);
    }

    // 중복 제거
    private Long getUserId(UserDetails userDetails) {
        return Long.parseLong(userDetails.getUsername());
    }

    private LocalDate getDateOrDefault(LocalDate date) {
        return date == null ? LocalDate.now() : date;
    }
}
