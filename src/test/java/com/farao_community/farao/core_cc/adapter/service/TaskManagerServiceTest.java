/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class TaskManagerServiceTest {
    @MockitoBean
    private RestTemplateBuilder restTemplateBuilder;
    @Autowired
    private TaskManagerService taskManagerService;

    @Test
    void getTaskFromTimestampNoRetry() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(restTemplate.getForEntity(Mockito.contains(timestamp), Mockito.eq(TaskDto.class)))
                .thenReturn(new ResponseEntity<>(taskDto, HttpStatus.OK));

        final Optional<TaskDto> result = taskManagerService.getTaskFromTimestamp(timestamp);

        Assertions.assertThat(result).contains(taskDto);
    }

    @Test
    void getTaskFromTimestampTaskNotFound() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.getForEntity(Mockito.contains(timestamp), Mockito.eq(TaskDto.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        final Optional<TaskDto> result = taskManagerService.getTaskFromTimestamp(timestamp);

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    void getTaskFromTimestampRetryOnce() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(restTemplate.getForEntity(Mockito.contains(timestamp), Mockito.eq(TaskDto.class)))
                .thenThrow(RestClientException.class)
                .thenReturn(new ResponseEntity<>(taskDto, HttpStatus.OK));

        final Optional<TaskDto> result = taskManagerService.getTaskFromTimestamp(timestamp);

        Assertions.assertThat(result).contains(taskDto);
    }

    @Test
    void getTaskFromTimestampAllRetry() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.getForEntity(Mockito.contains(timestamp), Mockito.eq(TaskDto.class)))
                .thenThrow(RestClientException.class);

        final Optional<TaskDto> result = taskManagerService.getTaskFromTimestamp(timestamp);

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    void addNewRunInTaskHistoryNoRetry() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(restTemplate.exchange(Mockito.contains(timestamp), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenReturn(new ResponseEntity<>(taskDto, HttpStatus.OK));

        final Optional<TaskDto> result = taskManagerService.addNewRunInTaskHistory(timestamp, List.of());

        Assertions.assertThat(result).contains(taskDto);
    }

    @Test
    void addNewRunInTaskHistoryTaskNotFound() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.exchange(Mockito.contains(timestamp), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        final Optional<TaskDto> result = taskManagerService.addNewRunInTaskHistory(timestamp, List.of());

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    void addNewRunInTaskHistoryRetryOnce() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(restTemplate.exchange(Mockito.contains(timestamp), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenThrow(RestClientException.class)
                .thenReturn(new ResponseEntity<>(taskDto, HttpStatus.OK));

        final Optional<TaskDto> result = taskManagerService.addNewRunInTaskHistory(timestamp, List.of());

        Assertions.assertThat(result).contains(taskDto);
    }

    @Test
    void addNewRunInTaskHistoryAllRetry() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.exchange(Mockito.contains(timestamp), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenThrow(RestClientException.class);

        final Optional<TaskDto> result = taskManagerService.addNewRunInTaskHistory(timestamp, List.of());

        Assertions.assertThat(result).isEmpty();
    }

    @Test
    void updateTaskStatusNoRetry() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(restTemplate.exchange(Mockito.contains(timestamp), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenReturn(new ResponseEntity<>(taskDto, HttpStatus.OK));

        final boolean result = taskManagerService.updateTaskStatus(timestamp, TaskStatus.PENDING);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void updateTaskStatusTaskNotFound() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.exchange(Mockito.contains(timestamp), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenReturn(new ResponseEntity<>(HttpStatus.NOT_FOUND));

        final boolean result = taskManagerService.updateTaskStatus(timestamp, TaskStatus.PENDING);

        Assertions.assertThat(result).isFalse();
    }

    @Test
    void updateTaskStatusRetryOnce() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        final TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(restTemplate.exchange(Mockito.contains(timestamp), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenThrow(RestClientException.class)
                .thenReturn(new ResponseEntity<>(taskDto, HttpStatus.OK));

        final boolean result = taskManagerService.updateTaskStatus(timestamp, TaskStatus.PENDING);

        Assertions.assertThat(result).isTrue();
    }

    @Test
    void updateTaskStatusAllRetry() {
        final String timestamp = "2024-09-13T09:30Z";
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
        Mockito.when(restTemplate.exchange(Mockito.contains(timestamp), Mockito.eq(HttpMethod.PUT), Mockito.any(HttpEntity.class), Mockito.eq(TaskDto.class)))
                .thenThrow(RestClientException.class);

        final boolean result = taskManagerService.updateTaskStatus(timestamp, TaskStatus.PENDING);

        Assertions.assertThat(result).isFalse();
    }
}
