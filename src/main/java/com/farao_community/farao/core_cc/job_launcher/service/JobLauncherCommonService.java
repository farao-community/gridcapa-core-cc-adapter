/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.job_launcher.service;

import com.farao_community.farao.core_cc.adapter.app.CoreCCAdapterService;
import com.farao_community.farao.core_cc.job_launcher.JobLauncherConfigurationProperties;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import org.slf4j.Logger;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class JobLauncherCommonService {
    private final Logger jobLauncherEventsLogger;
    private final RestTemplateBuilder restTemplateBuilder;
    private final CoreCCAdapterService coreCCAdapterService;
    private final String taskManagerTimestampBaseUrl;

    public JobLauncherCommonService(JobLauncherConfigurationProperties jobLauncherConfigurationProperties, Logger jobLauncherEventsLogger, RestTemplateBuilder restTemplateBuilder, CoreCCAdapterService coreCCAdapterService) {
        this.jobLauncherEventsLogger = jobLauncherEventsLogger;
        this.restTemplateBuilder = restTemplateBuilder;
        this.coreCCAdapterService = coreCCAdapterService;
        this.taskManagerTimestampBaseUrl = jobLauncherConfigurationProperties.url().taskManagerTimestampUrl();
    }

    public void launchJob(TaskDto taskDto, boolean isLaunchedAutomatically) {
        String timestamp = taskDto.getTimestamp().toString();
        jobLauncherEventsLogger.info("Task launched on TS {}", timestamp);
        updateTaskStatusToPending(timestamp);
        addNewRunInTaskHistory(timestamp);
        coreCCAdapterService.handleTask(taskDto, isLaunchedAutomatically);
    }

    private void updateTaskStatusToPending(String timestamp) {
        final String url = taskManagerTimestampBaseUrl + timestamp + "/status?status=PENDING";
        restTemplateBuilder.build().put(url, TaskDto.class);
    }

    private void addNewRunInTaskHistory(String timestamp) {
        final String url = taskManagerTimestampBaseUrl + timestamp + "/runHistory";
        restTemplateBuilder.build().put(url, TaskDto.class);
    }
}
