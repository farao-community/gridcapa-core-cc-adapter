/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class JobLauncherManualServiceTest {

    @Autowired
    private JobLauncherManualService service;

    @MockBean
    private CoreCCAdapterService adapterService;
    @MockBean
    private Logger eventsLogger;
    @MockBean
    private TaskManagerService taskManagerService;

    @Test
    void launchJobWithNoTaskDtoTest() {
        final String timestamp = "2024-09-18T09:30Z";
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.empty());

        service.launchJob(timestamp, List.of());

        Mockito.verifyNoInteractions(adapterService);
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"NOT_CREATED", "CREATED", "PENDING", "RUNNING", "STOPPING", "INTERRUPTED"})
    void launchJobWithNotReadyTask(final TaskStatus taskStatus) {
        final String timestamp = "2024-09-18T09:30Z";
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse(timestamp), taskStatus, null, null, null, null, null, null);
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.of(taskDto));

        service.launchJob(timestamp, List.of());

        Mockito.verify(eventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.any(OffsetDateTime.class));
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"READY", "SUCCESS", "ERROR"})
    void launchJobWithReadyTaskAndParameters(final TaskStatus taskStatus) {
        final String timestamp = "2024-09-18T09:30Z";
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse(timestamp), taskStatus, null, null, null, null, null, null);
        Mockito.when(taskManagerService.getTaskFromTimestamp(timestamp)).thenReturn(Optional.of(taskDto));
        final List<TaskParameterDto> parameters = List.of(new TaskParameterDto("id", "type", "value", "default"));

        service.launchJob(timestamp, parameters);

        final ArgumentCaptor<List<TaskParameterDto>> parametersCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(adapterService, Mockito.times(1)).handleTask(Mockito.eq(taskDto), Mockito.eq(false), parametersCaptor.capture());
        Assertions.assertThat(parametersCaptor.getValue()).isEqualTo(parameters);
    }
}
