/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.configuration.CoreCCAdapterConfiguration;
import com.farao_community.farao.core_cc.adapter.exception.TaskNotFoundException;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class JobLauncherManualService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherManualService.class);

    private final CoreCCAdapterService adapterService;
    private final Logger eventsLogger;
    private final RestTemplateBuilder restTemplateBuilder;
    private final String taskManagerTimestampBaseUrl;

    public JobLauncherManualService(CoreCCAdapterService adapterService,
                                    CoreCCAdapterConfiguration coreCCAdapterConfiguration,
                                    Logger eventsLogger,
                                    RestTemplateBuilder restTemplateBuilder) {
        this.adapterService = adapterService;
        this.eventsLogger = eventsLogger;
        this.restTemplateBuilder = restTemplateBuilder;
        this.taskManagerTimestampBaseUrl = coreCCAdapterConfiguration.taskManagerTimestampUrl();
    }

    public void launchJob(String timestamp, List<TaskParameterDto> parameters) {
        LOGGER.info("Received order to launch task {}", timestamp);
        final String requestUrl = getUrlToRetrieveTaskDto(timestamp);
        LOGGER.info("Requesting URL: {}", requestUrl);
        final ResponseEntity<TaskDto> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto.class); // NOSONAR
        TaskDto taskDto = responseEntity.getBody();

        if (responseEntity.getStatusCode() != HttpStatus.OK || taskDto == null) {
            throw new TaskNotFoundException();
        }

        // Propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
        // This should be done only once, as soon as the information to add in mdc is available.
        MDC.put("gridcapa-task-id", taskDto.getId().toString());

        if (isTaskReadyToBeLaunched(taskDto)) {
            if (!parameters.isEmpty()) {
                taskDto = getTaskDtoWithParameters(taskDto, parameters);
            }

            adapterService.handleTask(taskDto, false);
        } else {
            eventsLogger.warn("Failed to launch task with timestamp {} because it is not ready yet", taskDto.getTimestamp());
        }
    }

    private String getUrlToRetrieveTaskDto(String timestamp) {
        return taskManagerTimestampBaseUrl + timestamp;
    }

    private static boolean isTaskReadyToBeLaunched(TaskDto taskDto) {
        return taskDto.getStatus() == TaskStatus.READY
                || taskDto.getStatus() == TaskStatus.SUCCESS
                || taskDto.getStatus() == TaskStatus.ERROR;
    }

    private static TaskDto getTaskDtoWithParameters(TaskDto taskDto, List<TaskParameterDto> parameters) {
        return new TaskDto(taskDto.getId(), taskDto.getTimestamp(), taskDto.getStatus(), taskDto.getInputs(), taskDto.getAvailableInputs(), taskDto.getOutputs(), taskDto.getProcessEvents(), taskDto.getRunHistory(), parameters);
    }
}
