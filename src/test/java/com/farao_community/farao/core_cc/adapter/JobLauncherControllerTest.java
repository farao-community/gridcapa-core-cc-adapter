/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter;

import com.farao_community.farao.core_cc.adapter.exception.CoreCCAdapterException;
import com.farao_community.farao.core_cc.adapter.exception.TaskNotFoundException;
import com.farao_community.farao.core_cc.adapter.service.JobLauncherManualService;
import com.farao_community.farao.gridcapa.task_manager.api.ParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class JobLauncherControllerTest {

    @Autowired
    private JobLauncherController jobLauncherController;

    @MockBean
    private JobLauncherManualService jobLauncherService;

    @Test
    void launchJobOk() {
        String timestamp = "2021-12-09T21:30";
        Mockito.doNothing().when(jobLauncherService).launchJob(timestamp, List.of());

        ResponseEntity<Void> response = jobLauncherController.launchJob(timestamp, List.of());

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void launchJobWithParametersOk() {
        String timestamp = "2021-12-09T21:30";
        List<ParameterDto> parameterDtos = List.of(new ParameterDto("id", "name", 1, "type", "title", 1, "value", "defaultValue"));
        Mockito.doNothing().when(jobLauncherService).launchJob(Mockito.eq(timestamp), Mockito.anyList());

        ArgumentCaptor<List<TaskParameterDto>> captor = ArgumentCaptor.forClass(List.class);
        ResponseEntity<Void> response = jobLauncherController.launchJob(timestamp, parameterDtos);

        Mockito.verify(jobLauncherService).launchJob(Mockito.eq(timestamp), captor.capture());
        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Assertions.assertThat(captor.getValue())
                .isNotNull()
                .hasSize(1)
                .first()
                .hasFieldOrPropertyWithValue("id", "id")
                .hasFieldOrPropertyWithValue("parameterType", "type")
                .hasFieldOrPropertyWithValue("value", "value")
                .hasFieldOrPropertyWithValue("defaultValue", "defaultValue");
    }

    @Test
    void launchJobTaskNotFoundTest() {
        String timestamp = "2021-12-09T21:30";
        Mockito.doThrow(TaskNotFoundException.class).when(jobLauncherService).launchJob(timestamp, List.of());

        ResponseEntity<Void> response = jobLauncherController.launchJob(timestamp, List.of());

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void launchJobInvalidDataTest() {
        String timestamp = "2021-12-09T21:30";
        Mockito.doThrow(CoreCCAdapterException.class).when(jobLauncherService).launchJob(timestamp, List.of());

        ResponseEntity<Void> response = jobLauncherController.launchJob(timestamp, List.of());

        Assertions.assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
