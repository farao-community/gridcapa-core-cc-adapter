/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.FileType;
import com.farao_community.farao.core_cc.adapter.configuration.CoreCCAdapterConfiguration;
import com.farao_community.farao.core_cc.adapter.exception.CoreCCAdapterException;
import com.farao_community.farao.core_cc.adapter.exception.MissingFileException;
import com.farao_community.farao.core_cc.adapter.exception.RaoRequestImportException;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCFileResource;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCRequest;
import com.farao_community.farao.gridcapa_core_cc.app.inputs.rao_request.RequestMessage;
import com.farao_community.farao.gridcapa_core_cc.starter.CoreCCClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.unicorn.request.request_payload.RequestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.threeten.extra.Interval;

import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class CoreCCAdapterService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreCCAdapterService.class);
    private final CoreCCClient coreCCClient;
    private final FileImporter fileImporter;
    private final MinioAdapter minioAdapter;
    private final Logger eventsLogger;
    private final RestTemplateBuilder restTemplateBuilder;
    private final String taskManagerTimestampBaseUrl;

    public CoreCCAdapterService(CoreCCClient coreCCClient, FileImporter fileImporter, MinioAdapter minioAdapter, Logger eventsLogger, RestTemplateBuilder restTemplateBuilder, CoreCCAdapterConfiguration coreCCAdapterConfiguration) {
        this.coreCCClient = coreCCClient;
        this.fileImporter = fileImporter;
        this.minioAdapter = minioAdapter;
        this.eventsLogger = eventsLogger;
        this.restTemplateBuilder = restTemplateBuilder;
        this.taskManagerTimestampBaseUrl = coreCCAdapterConfiguration.taskManagerTimestampUrl();
    }

    public void handleTask(TaskDto taskDto, boolean isLaunchedAutomatically) {
        final String runMode = isLaunchedAutomatically ? "automatic" : "manual";
        final OffsetDateTime taskTimestamp = taskDto.getTimestamp();
        try {
            LOGGER.info("Handling {} run request on TS {} ", runMode, taskTimestamp);
            final CoreCCRequest coreCCRequest = getCoreCCRequest(taskDto, isLaunchedAutomatically);

            eventsLogger.info("Task launched on TS {}", taskTimestamp);
            updateTaskStatusToPending(taskTimestamp);
            addNewRunInTaskHistory(taskTimestamp);

            runAsync(coreCCRequest);
        } catch (RaoRequestImportException rrie) {
            throw new CoreCCAdapterException("Error occurred during loading of RAOREQUEST file content", rrie);
        } catch (MissingFileException mfe) {
            throw new CoreCCAdapterException("Some input files are missing, the task can't be launched", mfe);
        } catch (CoreCCAdapterException cccae) {
            throw cccae;
        } catch (Exception e) {
            throw new CoreCCAdapterException(String.format("Error while handling %s run request on TS %s", runMode, taskTimestamp), e);
        }
    }

    void runAsync(CoreCCRequest request) {
        CompletableFuture.runAsync(() -> coreCCClient.run(request));
    }

    CoreCCRequest getCoreCCRequest(TaskDto taskDto, boolean isLaunchedAutomatically) throws RaoRequestImportException {
        final String id = taskDto.getId().toString();
        final OffsetDateTime taskTimestamp = taskDto.getTimestamp();
        final List<ProcessFileDto> availableInputFiles = taskDto.getAvailableInputs();

        final ProcessFileDto raoRequestProcessFile = findRaoRequestProcessFile(taskDto.getInputs())
                .orElseThrow(() -> new MissingFileException(String.format("No RAOREQUEST file found in task %s", taskTimestamp)));

        final EnumMap<FileType, CoreCCFileResource> inputFilesMap = new EnumMap<>(FileType.class);
        CoreCCFileResource raoRequestFileResource = getCoreCCFileResource(raoRequestProcessFile);
        inputFilesMap.put(FileType.RAOREQUEST, raoRequestFileResource);

        getDocumentIdsFromRaoRequest(taskTimestamp, raoRequestFileResource).stream()
                .map(documentId -> findProcessFileMatchingDocumentId(availableInputFiles, documentId)
                        .orElseThrow(() -> new MissingFileException(String.format("No file found in task %s matching DocumentId %s", taskTimestamp, documentId))))
                .forEach(processFile -> addProcessFileInInputFilesMap(processFile, inputFilesMap));

        // TODO Remove this code specific to VIRTUALHUB files when Coreso has made it clear how to handle them
        ProcessFileDto virtualhubProcessFile = findVirtualhubProcessFile(taskDto.getInputs())
                .orElseThrow(() -> new MissingFileException(String.format("No VIRTUALHUB file found in task %s", taskTimestamp)));
        CoreCCFileResource virtualhubsFileResource = getCoreCCFileResource(virtualhubProcessFile);
        inputFilesMap.put(FileType.VIRTUALHUB, virtualhubsFileResource);

        return new CoreCCRequest(
                id,
                taskTimestamp,
                inputFilesMap.get(FileType.CGM),
                inputFilesMap.get(FileType.DCCGM),
                inputFilesMap.get(FileType.CBCORA),
                inputFilesMap.get(FileType.GLSK),
                inputFilesMap.get(FileType.REFPROG),
                inputFilesMap.get(FileType.RAOREQUEST),
                inputFilesMap.get(FileType.VIRTUALHUB),
                isLaunchedAutomatically,
                taskDto.getParameters()
        );
    }

    private static Optional<ProcessFileDto> findRaoRequestProcessFile(List<ProcessFileDto> inputs) {
        return inputs.stream()
                .filter(f -> "RAOREQUEST".equals(f.getFileType()))
                .findFirst();
    }

    // TODO Remove this method when Coreso has made it clear how to handle VIRTUALHUB files
    private static Optional<ProcessFileDto> findVirtualhubProcessFile(List<ProcessFileDto> inputs) {
        return inputs.stream()
                .filter(f -> "VIRTUALHUB".equals(f.getFileType()))
                .findFirst();
    }

    private static Optional<ProcessFileDto> findProcessFileMatchingDocumentId(List<ProcessFileDto> availableInputs, String documentId) {
        return availableInputs.stream()
                .filter(availableInput -> documentId.equals(availableInput.getDocumentId()))
                .findFirst();
    }

    private List<String> getDocumentIdsFromRaoRequest(OffsetDateTime taskTimestamp, CoreCCFileResource raoRequestFileResource) throws RaoRequestImportException {
        final RequestMessage raoRequestMessage = fileImporter.importRaoRequest(raoRequestFileResource);

        final RequestItem requestItem = raoRequestMessage.getPayload().getRequestItems().getRequestItem().stream()
                .filter(item -> Interval.parse(item.getTimeInterval()).contains(taskTimestamp.toInstant()))
                .findFirst()
                .orElseThrow(() -> new CoreCCAdapterException(String.format("No data for timestamp %s in RAOREQUEST file", taskTimestamp)));

        return requestItem.getFiles().getFile().stream()
                .filter(f -> !"CFG_RAO".equals(f.getCode())) // TODO Remove this filter when Coreso has made it clear how to handle VITRUALHUB files
                .map(f -> f.getUrl().replace("documentIdentification://", ""))
                .toList();
    }

    private void addProcessFileInInputFilesMap(ProcessFileDto processFileDto, EnumMap<FileType, CoreCCFileResource> inputFiles) {
        final String fileType = processFileDto.getFileType();
        LOGGER.info("Received {} with DocumentId {}", fileType, processFileDto.getDocumentId());
        switch (fileType) {
            case "CGM" -> inputFiles.put(FileType.CGM, getCoreCCFileResource(processFileDto));
            case "DCCGM" -> {
                // TODO It seems that the filePath will never be null for any filetype, as the processFileDto is retrieved
                //  from the "available inputs" of the task, which is only populated with files from database.
                //  Therefore, it could maybe be possible to simplify the code by removing switch and only doing something like:
                //  inputFiles.put(FileType.valueOf(fileType), getCoreCCFileResource(processFileDto));
                if (null != processFileDto.getFilePath()) {
                    inputFiles.put(FileType.DCCGM, getCoreCCFileResource(processFileDto));
                }
            }
            case "CBCORA" -> inputFiles.put(FileType.CBCORA, getCoreCCFileResource(processFileDto));
            case "GLSK" -> inputFiles.put(FileType.GLSK, getCoreCCFileResource(processFileDto));
            case "REFPROG" -> inputFiles.put(FileType.REFPROG, getCoreCCFileResource(processFileDto));
            case "VIRTUALHUB" -> inputFiles.put(FileType.VIRTUALHUB, getCoreCCFileResource(processFileDto));
            default -> LOGGER.warn("Unexpected filetype {}, file {} won't be added to CoreCCRequest", fileType, processFileDto.getFilename());
        }
    }

    private CoreCCFileResource getCoreCCFileResource(ProcessFileDto processFileDto) {
        final String filename = processFileDto.getFilename();
        final String fileUrl = minioAdapter.generatePreSignedUrlFromFullMinioPath(processFileDto.getFilePath(), 1);
        return new CoreCCFileResource(filename, fileUrl);
    }

    private void updateTaskStatusToPending(OffsetDateTime timestamp) {
        final String url = taskManagerTimestampBaseUrl + timestamp + "/status?status=PENDING";
        restTemplateBuilder.build().put(url, TaskDto.class);
    }

    private void addNewRunInTaskHistory(OffsetDateTime timestamp) {
        final String url = taskManagerTimestampBaseUrl + timestamp + "/runHistory";
        restTemplateBuilder.build().put(url, TaskDto.class);
    }
}
