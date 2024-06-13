/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.job_launcher.service;

import com.farao_community.farao.core_cc.job_launcher.JobLauncherConfigurationProperties;
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
 */
@Service
public class JobLauncherService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherService.class);

    private final JobLauncherCommonService jobLauncherCommonService;
    private final Logger jobLauncherEventsLogger;
    private final RestTemplateBuilder restTemplateBuilder;
    private final String taskManagerTimestampBaseUrl;

    public JobLauncherService(JobLauncherCommonService jobLauncherCommonService,
                              JobLauncherConfigurationProperties jobLauncherConfigurationProperties,
                              Logger jobLauncherEventsLogger,
                              RestTemplateBuilder restTemplateBuilder) {
        this.jobLauncherCommonService = jobLauncherCommonService;
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
        this.restTemplateBuilder = restTemplateBuilder;
        this.taskManagerTimestampBaseUrl = jobLauncherConfigurationProperties.url().taskManagerTimestampUrl();
    }

    /**
     * Trying to launch a computation. TaskDto is retrieved and status is checked, if its status enables to launch
     * the computation (READY, SUCCESS or ERROR), task status is changed to PENDING in the meantime and a message
     * to launch the computation is sent.
     * A boolean is returned to define the status of the launch operation. It is false only when the task is not found.
     * When the status does not enable to launch the computation it still returns true because the HTTP request was
     * correctly formed, it is just internal business logic that does not allow to launch the computation. If not
     * launched status will remain the same, if launched status will be changed to PENDING.
     *
     * @param timestamp: Task timestamp to be launched.
     * @return False only when the timestamp does not exist. Otherwise, true whether computation is launched or not.
     */
    public boolean launchJob(String timestamp, List<TaskParameterDto> parameters) {
        LOGGER.info("Received order to launch task {}", timestamp);
        final String requestUrl = getUrlToRetrieveTaskDto(timestamp);
        LOGGER.info("Requesting URL: {}", requestUrl);
        final ResponseEntity<TaskDto> responseEntity = restTemplateBuilder.build().getForEntity(requestUrl, TaskDto.class); // NOSONAR
        TaskDto taskDto = responseEntity.getBody();
        if (responseEntity.getStatusCode() == HttpStatus.OK && taskDto != null) {
            if (!parameters.isEmpty()) {
                taskDto = new TaskDto(taskDto.getId(), taskDto.getTimestamp(), taskDto.getStatus(), taskDto.getInputs(), taskDto.getAvailableInputs(), taskDto.getOutputs(), taskDto.getProcessEvents(), taskDto.getRunHistory(), parameters);
            }
            // Propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", taskDto.getId().toString());

            if (isTaskReadyToBeLaunched(taskDto)) {
                jobLauncherCommonService.launchJob(taskDto, false);
            } else {
                jobLauncherEventsLogger.warn("Failed to launch task with timestamp {} because it is not ready yet", taskDto.getTimestamp());
            }
            return true;
        }
        return false;
    }

    private static boolean isTaskReadyToBeLaunched(TaskDto taskDto) {
        return taskDto.getStatus() == TaskStatus.READY
                || taskDto.getStatus() == TaskStatus.SUCCESS
                || taskDto.getStatus() == TaskStatus.ERROR
                || taskDto.getStatus() == TaskStatus.INTERRUPTED;
    }

    private String getUrlToRetrieveTaskDto(String timestamp) {
        return taskManagerTimestampBaseUrl + timestamp;
    }
}
