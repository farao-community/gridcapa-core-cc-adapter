/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.configuration.CoreCCAdapterConfiguration;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * @author Marc Schwitzguebel {@literal <marc.schwitzguebel at rte-france.com>}
 */
@Service
public class TaskManagerService {

    private final RestTemplateBuilder restTemplateBuilder;
    private final String taskManagerTimestampBaseUrl;

    public TaskManagerService(RestTemplateBuilder restTemplateBuilder, CoreCCAdapterConfiguration coreCCAdapterConfiguration) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.taskManagerTimestampBaseUrl = coreCCAdapterConfiguration.taskManagerTimestampUrl();
    }

    public void updateTaskStatusToPending(OffsetDateTime timestamp) {
        final String url = taskManagerTimestampBaseUrl + timestamp + "/status?status=PENDING";
        restTemplateBuilder.build().put(url, null);
    }

    public void addNewRunInTaskHistory(OffsetDateTime timestamp, List<ProcessFileDto> inputFiles) {
        final String url = taskManagerTimestampBaseUrl + timestamp + "/runHistory";
        restTemplateBuilder.build().put(url, inputFiles);
    }

    public TaskDto getUpdatedTask(OffsetDateTime timestamp) {
        final String url = taskManagerTimestampBaseUrl + timestamp;
        return restTemplateBuilder.build().getForEntity(url, TaskDto.class).getBody();
    }
}
