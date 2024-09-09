/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.FileType;
import com.farao_community.farao.core_cc.adapter.exception.CoreCCAdapterException;
import com.farao_community.farao.core_cc.adapter.exception.MissingFileException;
import com.farao_community.farao.core_cc.adapter.exception.RaoRequestImportException;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCFileResource;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCRequest;
import com.farao_community.farao.gridcapa_core_cc.app.inputs.rao_request.RequestMessage;
import com.farao_community.farao.gridcapa_core_cc.starter.CoreCCClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.unicorn.request.request_payload.RequestItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.threeten.extra.Interval;

import java.time.OffsetDateTime;
import java.util.ArrayList;
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
    private final TaskMangerService taskMangerService;

    public CoreCCAdapterService(CoreCCClient coreCCClient, FileImporter fileImporter, MinioAdapter minioAdapter, Logger eventsLogger, TaskMangerService taskMangerService) {
        this.coreCCClient = coreCCClient;
        this.fileImporter = fileImporter;
        this.minioAdapter = minioAdapter;
        this.eventsLogger = eventsLogger;
        this.taskMangerService = taskMangerService;
    }

    public void handleTask(TaskDto taskDto, boolean isLaunchedAutomatically) {
        final String runMode = isLaunchedAutomatically ? "automatic" : "manual";
        final OffsetDateTime taskTimestamp = taskDto.getTimestamp();
        try {
            LOGGER.info("Handling {} run request on TS {} ", runMode, taskTimestamp);
            List<ProcessFileDto> inputFiles = getInputProcessFilesFromRaoRequest(taskDto);

            eventsLogger.info("Task launched on TS {}", taskTimestamp);
            taskMangerService.updateTaskStatusToPending(taskTimestamp);
            taskMangerService.addNewRunInTaskHistory(taskTimestamp, inputFiles);
            TaskDto updatedTaskDto = taskMangerService.getUpdatedTask(taskTimestamp);
            final CoreCCRequest coreCCRequest = getCoreCCRequest(updatedTaskDto, inputFiles, isLaunchedAutomatically);
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

    private void runAsync(CoreCCRequest request) {
        CompletableFuture.runAsync(() -> coreCCClient.run(request));
    }

    private List<ProcessFileDto> getInputProcessFilesFromRaoRequest(TaskDto taskDto) throws RaoRequestImportException {
        final OffsetDateTime taskTimestamp = taskDto.getTimestamp();
        final List<ProcessFileDto> availableInputFiles = taskDto.getAvailableInputs();

        final ProcessFileDto raoRequestProcessFile = findRaoRequestProcessFile(taskDto.getInputs())
                .orElseThrow(() -> new MissingFileException(String.format("No RAOREQUEST file found in task %s", taskTimestamp)));

        final List<ProcessFileDto> inputFiles = new ArrayList<>();
        inputFiles.add(raoRequestProcessFile);

        final CoreCCFileResource raoRequestFileResource = getCoreCCFileResource(raoRequestProcessFile);
        getDocumentIdsFromRaoRequest(taskTimestamp, raoRequestFileResource).stream()
                .map(documentId -> findProcessFileMatchingDocumentId(availableInputFiles, documentId)
                        .orElseThrow(() -> new MissingFileException(String.format("No file found in task %s matching DocumentId %s", taskTimestamp, documentId))))
                .forEach(inputFiles::add);

        // TODO Remove this code specific to VIRTUALHUB files when Coreso has made it clear how to handle them
        final ProcessFileDto virtualhubProcessFile = findVirtualhubProcessFile(taskDto.getInputs())
                .orElseThrow(() -> new MissingFileException(String.format("No VIRTUALHUB file found in task %s", taskTimestamp)));
        inputFiles.add(virtualhubProcessFile);

        return inputFiles;
    }

    private CoreCCRequest getCoreCCRequest(TaskDto taskDto, List<ProcessFileDto> inputFiles, boolean isLaunchedAutomatically) {
        final String id = taskDto.getId().toString();
        final OffsetDateTime taskTimestamp = taskDto.getTimestamp();
        final EnumMap<FileType, CoreCCFileResource> inputFilesMap = new EnumMap<>(FileType.class);
        inputFiles.forEach(inputFile -> addProcessFileInInputFilesMap(inputFile, inputFilesMap));

        return new CoreCCRequest(
                id,
                getCurrentRunId(taskDto),
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
        try {
            final FileType fileType = FileType.valueOf(processFileDto.getFileType());
            LOGGER.info("Received {} with DocumentId {}", fileType, processFileDto.getDocumentId());
            inputFiles.put(fileType, getCoreCCFileResource(processFileDto));
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Unexpected filetype {}, file {} won't be added to CoreCCRequest", processFileDto.getFileType(), processFileDto.getFilename());
        }
    }

    private CoreCCFileResource getCoreCCFileResource(ProcessFileDto processFileDto) {
        final String filename = processFileDto.getFilename();
        final String fileUrl = minioAdapter.generatePreSignedUrlFromFullMinioPath(processFileDto.getFilePath(), 1);
        return new CoreCCFileResource(filename, fileUrl);
    }

    private String getCurrentRunId(TaskDto taskDto) {
        List<ProcessRunDto> runHistory = taskDto.getRunHistory();
        if (runHistory == null || runHistory.isEmpty()) {
            LOGGER.warn("Failed to handle manual run request on timestamp {} because it has no run history", taskDto.getTimestamp());
            throw new CoreCCAdapterException("Failed to handle manual run request on timestamp because it has no run history");
        }
        runHistory.sort((o1, o2) -> o2.getExecutionDate().compareTo(o1.getExecutionDate()));
        return runHistory.get(0).getId().toString();
    }
}
