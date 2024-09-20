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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class JobLauncherManualService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherManualService.class);

    private final CoreCCAdapterService adapterService;
    private final Logger eventsLogger;
    private final TaskManagerService taskManagerService;

    public JobLauncherManualService(CoreCCAdapterService adapterService, Logger eventsLogger, TaskManagerService taskManagerService) {
        this.adapterService = adapterService;
        this.eventsLogger = eventsLogger;
        this.taskManagerService = taskManagerService;
    }

    public void launchJob(final String timestamp, final List<TaskParameterDto> parameters) {
        LOGGER.info("Received order to launch task {}", timestamp);
        final Optional<TaskDto> taskDtoOpt = taskManagerService.getTaskFromTimestamp(timestamp);
        if (taskDtoOpt.isPresent()) {
            final TaskDto taskDto = taskDtoOpt.get();
            // Propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
            // This should be done only once, as soon as the information to add in mdc is available.
            MDC.put("gridcapa-task-id", taskDto.getId().toString());

            if (isTaskReadyToBeLaunched(taskDto)) {
                adapterService.handleTask(taskDto, false, parameters);
            } else {
                eventsLogger.warn("Failed to launch task with timestamp {} because it is not ready yet", taskDto.getTimestamp());
            }
        } else {
            LOGGER.error("Failed to launch task with timestamp {}: could not retrieve task from the task-manager", timestamp);
        }
    }

    private static boolean isTaskReadyToBeLaunched(final TaskDto taskDto) {
        return taskDto.getStatus() == TaskStatus.READY
                || taskDto.getStatus() == TaskStatus.SUCCESS
                || taskDto.getStatus() == TaskStatus.ERROR;
    }
}
