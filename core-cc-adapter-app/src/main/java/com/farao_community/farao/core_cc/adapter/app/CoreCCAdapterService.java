/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.app;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCFileResource;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCRequest;
import com.farao_community.farao.gridcapa_core_cc.starter.CoreCCClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@Service
public class CoreCCAdapterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreCCAdapterService.class);
    private final CoreCCClient coreCCClient;
    private final MinioAdapter minioAdapter;

    public CoreCCAdapterService(CoreCCClient coreCCClient, MinioAdapter minioAdapter) {
        this.coreCCClient = coreCCClient;
        this.minioAdapter = minioAdapter;
    }

    void handleAutoTask(TaskDto taskDto) {
        try {
            if (taskDto.getStatus() == TaskStatus.READY
                    || taskDto.getStatus() == TaskStatus.SUCCESS
                    || taskDto.getStatus() == TaskStatus.ERROR) {
                LOGGER.info("Handling automatic run request on TS {} ", taskDto.getTimestamp());
                CoreCCRequest request = getAutomaticCoreCCRequest(taskDto);
                runAsync(request);
            } else {
                LOGGER.warn("Failed to handle automatic run request on timestamp {} because it is not ready yet", taskDto.getTimestamp());
            }
        } catch (Exception e) {
            throw new CoreCCAdapterException(String.format("Error during handling automatic run request %s on TS ", taskDto.getTimestamp()), e);
        }
    }

    void runAsync(CoreCCRequest request) {
        CompletableFuture.runAsync(() -> coreCCClient.run(request));
    }

    void handleManualTask(TaskDto taskDto) {
        try {
            if (taskDto.getStatus() == TaskStatus.READY
                    || taskDto.getStatus() == TaskStatus.SUCCESS
                    || taskDto.getStatus() == TaskStatus.ERROR) {
                LOGGER.info("Handling manual run request on TS {} ", taskDto.getTimestamp());
                CoreCCRequest request = getManualCoreCCRequest(taskDto);
                runAsync(request);
            } else {
                LOGGER.warn("Failed to handle manual run request on timestamp {} because it is not ready yet", taskDto.getTimestamp());
            }
        } catch (Exception e) {
            throw new CoreCCAdapterException(String.format("Error during handling manual run request %s on TS ", taskDto.getTimestamp()), e);
        }

    }

    CoreCCRequest getManualCoreCCRequest(TaskDto taskDto) {
        return getCoreCCRequest(taskDto, false);
    }

    CoreCCRequest getAutomaticCoreCCRequest(TaskDto taskDto) {
        return getCoreCCRequest(taskDto, true);
    }

    CoreCCRequest getCoreCCRequest(TaskDto taskDto, boolean isLaunchedAutomatically) {
        String id = taskDto.getId().toString();
        OffsetDateTime offsetDateTime = taskDto.getTimestamp();
        List<ProcessFileDto> processFiles = taskDto.getInputs();
        CoreCCFileResource cgm = null;
        CoreCCFileResource dcCgm = null;
        CoreCCFileResource cbcora = null;
        CoreCCFileResource glsk = null;
        CoreCCFileResource refprog = null;
        CoreCCFileResource raoRequest = null;
        CoreCCFileResource virtualHub = null;
        for (ProcessFileDto processFileDto : processFiles) {
            String fileType = processFileDto.getFileType();
            String fileUrl = computeFileUrl(processFileDto);
            switch (fileType) {
                case "DCCGM":
                    if (null != fileUrl) {
                        LOGGER.info("Received DC CGM");
                        dcCgm = new CoreCCFileResource(processFileDto.getFilename(), fileUrl);
                    }
                    break;
                case "CGM":
                    LOGGER.info("Received CGM");
                    cgm = new CoreCCFileResource(processFileDto.getFilename(), fileUrl);
                    break;
                case "CBCORA":
                    LOGGER.info("Received CBCORA");
                    cbcora = new CoreCCFileResource(processFileDto.getFilename(), fileUrl);
                    break;
                case "GLSK":
                    LOGGER.info("Received GLSK");
                    glsk = new CoreCCFileResource(processFileDto.getFilename(), fileUrl);
                    break;
                case "REFPROG":
                    LOGGER.info("Received REFPROG");
                    refprog = new CoreCCFileResource(processFileDto.getFilename(), fileUrl);
                    break;
                case "RAOREQUEST":
                    LOGGER.info("Received RAOREQUEST");
                    raoRequest = new CoreCCFileResource(processFileDto.getFilename(), fileUrl);
                    break;
                case "VIRTUALHUB":
                    LOGGER.info("Received VIRTUALHUB");
                    virtualHub = new CoreCCFileResource(processFileDto.getFilename(), fileUrl);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + processFileDto.getFileType());
            }
        }
        return new CoreCCRequest(
                id,
                offsetDateTime,
                cgm,
                dcCgm,
                cbcora,
                glsk,
                refprog,
                raoRequest,
                virtualHub,
                isLaunchedAutomatically,
                taskDto.getParameters()
        );
    }

    /**
     * Resolve file url, dealing with optional DC CGM input file.
     *
     * @param processFileDto
     * @return
     */
    private String computeFileUrl(final ProcessFileDto processFileDto) {
        return "DCCGM".equals(processFileDto.getFileType()) && null == processFileDto.getFilePath() ?
                null : minioAdapter.generatePreSignedUrlFromFullMinioPath(processFileDto.getFilePath(), 1);
    }
}
