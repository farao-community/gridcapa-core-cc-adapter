/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.util.LoggingUtil;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 * @author Daniel Thirion {@literal <daniel.thirion at rte-france.com>}
 */
@Service
public class JobLauncherManualService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherManualService.class);

    private final CoreCCAdapterService adapterService;
    private final Logger eventsLogger;
    private final TaskManagerService taskManagerService;
    private final CopyOnWriteArraySet<String> timestampsBeingLaunched = new CopyOnWriteArraySet<>();

    public JobLauncherManualService(CoreCCAdapterService adapterService, Logger eventsLogger, TaskManagerService taskManagerService) {
        this.adapterService = adapterService;
        this.eventsLogger = eventsLogger;
        this.taskManagerService = taskManagerService;
    }

    public void launchJob(final String timestamp, final List<TaskParameterDto> parameters) {
        final String sanifiedTimestamp = LoggingUtil.sanifyString(timestamp);
        LOGGER.info("Received order to launch task {}", sanifiedTimestamp);
        LOGGER.info("Adding {} to tasks being launched.", sanifiedTimestamp);
        final boolean timestampAdded = timestampsBeingLaunched.add(sanifiedTimestamp);
        if (!timestampAdded) {
            LOGGER.warn("Task {} already being launched, stopping this thread.", sanifiedTimestamp);
            return;
        } else {
            LOGGER.info("{} has been correctly added to tasks being launched.", sanifiedTimestamp);
        }
        try {
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
                LOGGER.error("Failed to launch task with timestamp {}: could not retrieve task from the task-manager", sanifiedTimestamp);
            }
        } catch (final Exception e) {
            LOGGER.error("Exception occured while launching task with timestamp {}", sanifiedTimestamp);
            throw e;
        } finally {
            LOGGER.info("Removing {} from tasks being launched.", sanifiedTimestamp);
            timestampsBeingLaunched.remove(sanifiedTimestamp);
        }
    }

    private static boolean isTaskReadyToBeLaunched(final TaskDto taskDto) {
        return taskDto.getStatus() == TaskStatus.READY
                || taskDto.getStatus() == TaskStatus.SUCCESS
                || taskDto.getStatus() == TaskStatus.ERROR;
    }
}
