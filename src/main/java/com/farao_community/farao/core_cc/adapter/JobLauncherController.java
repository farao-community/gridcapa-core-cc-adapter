/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter;

import com.farao_community.farao.core_cc.adapter.exception.TaskNotFoundException;
import com.farao_community.farao.core_cc.adapter.service.JobLauncherManualService;
import com.farao_community.farao.gridcapa.task_manager.api.ParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class JobLauncherController {
    private static final Logger LOGGER = LoggerFactory.getLogger(JobLauncherController.class);

    private final JobLauncherManualService jobLauncherService;

    public JobLauncherController(JobLauncherManualService jobLauncherManualService) {
        this.jobLauncherService = jobLauncherManualService;
    }

    @PostMapping(value = "/start/{timestamp}")
    public ResponseEntity<Void> launchJob(@PathVariable String timestamp, @RequestBody List<ParameterDto> parameters) {
        List<TaskParameterDto> taskParameterDtos = List.of();
        if (parameters != null) {
            taskParameterDtos = parameters.stream().map(TaskParameterDto::new).toList();
        }
        try {
            jobLauncherService.launchJob(timestamp, taskParameterDtos);
            return ResponseEntity.ok().build();
        } catch (TaskNotFoundException tnfe) {
            return getNotFoundResponseEntity(timestamp);
        }
    }

    private ResponseEntity<Void> getNotFoundResponseEntity(@PathVariable String timestamp) {
        LOGGER.error("Failed to retrieve task with timestamp {}", timestamp);
        return ResponseEntity.notFound().build();
    }
}
