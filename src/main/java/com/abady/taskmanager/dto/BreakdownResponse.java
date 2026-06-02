package com.abady.taskmanager.dto;

import java.util.List;

public record BreakdownResponse(Long taskId, String title, List<String> subtasks) {}
