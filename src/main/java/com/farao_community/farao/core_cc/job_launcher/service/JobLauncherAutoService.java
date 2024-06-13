/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.job_launcher.service;

import com.farao_community.farao.core_cc.job_launcher.JobLauncherConfigurationProperties;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Amira Kahya {@literal <amira.kahya at rte-france.com>}
 */
@Service
public class JobLauncherAutoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherAutoService.class);
    private final JobLauncherConfigurationProperties jobLauncherConfigurationProperties;
    private final JobLauncherCommonService jobLauncherCommonService;

    public JobLauncherAutoService(JobLauncherConfigurationProperties jobLauncherConfigurationProperties, JobLauncherCommonService jobLauncherCommonService) {
        this.jobLauncherConfigurationProperties = jobLauncherConfigurationProperties;
        this.jobLauncherCommonService = jobLauncherCommonService;
    }

    @Bean
    public Consumer<Flux<TaskDto>> consumeTaskDtoUpdate() {
        return f -> f
                .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
                .subscribe(this::runReadyTasks);
    }

    void runReadyTasks(TaskDto updatedTaskDto) {
        try {
            if (updatedTaskDto.getStatus() == TaskStatus.READY) {
                final boolean autoTriggerFiletypesDefinedInConfig = !jobLauncherConfigurationProperties.autoTriggerFiletypes().isEmpty();
                if (autoTriggerFiletypesDefinedInConfig && allTriggerFilesAlreadyUsed(updatedTaskDto)) {
                    // If all selected files corresponding to trigger filetypes are linked to some Run in Task's history,
                    // then the update does not concern a trigger file, so job launcher should do nothing
                    return;
                }

                // Propagate in logs MDC the task id as an extra field to be able to match microservices logs with calculation tasks.
                // This should be done only once, as soon as the information to add in mdc is available.
                MDC.put("gridcapa-task-id", updatedTaskDto.getId().toString());
                jobLauncherCommonService.launchJob(updatedTaskDto, true);
            }
        } catch (Exception e) {
            // this exeption block avoids application from disconnecting from spring cloud stream !
            LOGGER.error(e.getMessage(), e);
        }
    }

    private boolean allTriggerFilesAlreadyUsed(TaskDto updatedTaskDto) {
        final List<ProcessFileDto> triggerFiles = updatedTaskDto.getInputs().stream()
                .filter(f -> jobLauncherConfigurationProperties.autoTriggerFiletypes().contains(f.getFileType()))
                .toList();

        final Set<ProcessFileDto> filesUsedInPreviousRun = updatedTaskDto.getRunHistory().stream()
                .flatMap(run -> run.getInputs().stream())
                .collect(Collectors.toSet());
        return filesUsedInPreviousRun.containsAll(triggerFiles);
    }
}
