/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.exception.TaskNotFoundException;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class JobLauncherManualServiceTest {

    @Autowired
    private JobLauncherManualService service;

    @MockBean
    private CoreCCAdapterService adapterService;
    @MockBean
    private RestTemplateBuilder restTemplateBuilder;
    @MockBean
    private Logger eventsLogger;

    @Test
    void launchJobWithNoTaskDtoTest() {
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        Assertions.assertThatExceptionOfType(TaskNotFoundException.class)
                .isThrownBy(() -> service.launchJob("", List.of()));
        Mockito.verifyNoInteractions(adapterService);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"NOT_CREATED", "CREATED", "PENDING", "RUNNING", "STOPPING"})
    void launchJobWithNotReadyTask(TaskStatus taskStatus) {
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), taskStatus, null, null, null, null, null, null);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(ResponseEntity.ok(taskDto));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        service.launchJob("", List.of());

        Mockito.verifyNoInteractions(adapterService);
        Mockito.verify(eventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"READY", "SUCCESS", "ERROR"})
    void launchJobWithReadyTask(TaskStatus taskStatus) {
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), taskStatus, null, null, null, null, null, null);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(ResponseEntity.ok(taskDto));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        service.launchJob("", List.of());

        Mockito.verify(adapterService, Mockito.times(1)).handleTask(taskDto, false);
    }

    @Test
    void launchJobWithReadyTaskAndParameters() {
        final RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), TaskStatus.READY, null, null, null, null, null, null);
        Mockito.when(restTemplate.getForEntity(Mockito.anyString(), Mockito.eq(TaskDto.class))).thenReturn(ResponseEntity.ok(taskDto));
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);

        service.launchJob("", List.of(new TaskParameterDto("id", "type", "value", "default")));

        final ArgumentCaptor<TaskDto> taskDtoCaptor = ArgumentCaptor.forClass(TaskDto.class);
        Mockito.verify(adapterService, Mockito.times(1)).handleTask(taskDtoCaptor.capture(), Mockito.eq(false));
        Assertions.assertThat(taskDtoCaptor.getValue())
                .isNotNull();
        Assertions.assertThat(taskDtoCaptor.getValue().getParameters())
                .isNotNull()
                .isNotEmpty();
    }
}
