/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.exception.CoreCCAdapterException;
import com.farao_community.farao.core_cc.adapter.exception.MissingFileException;
import com.farao_community.farao.core_cc.adapter.exception.RaoRequestImportException;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatusUpdate;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCRequest;
import com.farao_community.farao.gridcapa_core_cc.app.inputs.rao_request.Payload;
import com.farao_community.farao.gridcapa_core_cc.app.inputs.rao_request.RequestMessage;
import com.farao_community.farao.gridcapa_core_cc.starter.CoreCCClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import com.unicorn.request.request_payload.File;
import com.unicorn.request.request_payload.Files;
import com.unicorn.request.request_payload.RequestItem;
import com.unicorn.request.request_payload.RequestItems;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@SpringBootTest
class CoreCCAdapterServiceTest {
    @Autowired
    private CoreCCAdapterService service;

    @MockBean
    private CoreCCClient coreCCClient;
    @MockBean
    private FileImporter fileImporter;
    @MockBean
    private MinioAdapter minioAdapter;
    @MockBean
    private Logger eventsLogger;
    @MockBean
    private StreamBridge streamBridge;
    @MockBean
    private TaskManagerService taskManagerService;

    @Test
    void missingRaoRequestFileTest() {
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), TaskStatus.READY, List.of(), List.of(), null, null, null, null);

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("Some input files are missing, the task can't be launched")
                .havingCause()
                .isInstanceOf(MissingFileException.class)
                .withMessageStartingWith("No RAOREQUEST file found in task");
    }

    @Test
    void errorWhenReadingRaorequestTest() throws RaoRequestImportException {
        final ProcessFileDto raoRequestProcessFile = new ProcessFileDto("http://test-uri/F302", "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        final OffsetDateTime timestamp = OffsetDateTime.now();
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), timestamp, TaskStatus.READY, List.of(raoRequestProcessFile), List.of(raoRequestProcessFile), null, null, null, null);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("http://test-uri/F302", 1)).thenReturn("preSignedUrl");
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenThrow(RaoRequestImportException.class);

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("Error occurred during loading of RAOREQUEST file content")
                .withCauseInstanceOf(RaoRequestImportException.class);
    }

    @Test
    void noDataInRaoRequestTest() throws RaoRequestImportException {
        final ProcessFileDto raoRequestProcessFile = new ProcessFileDto("http://test-uri/F302", "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        final OffsetDateTime timestamp = OffsetDateTime.now();
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), timestamp, TaskStatus.READY, List.of(raoRequestProcessFile), List.of(raoRequestProcessFile), null, null, null, null);

        final RequestMessage requestMessage = new RequestMessage();
        final Payload payload = new Payload();
        final RequestItems requestItems = new RequestItems();
        requestMessage.setPayload(payload);
        payload.setRequestItems(requestItems);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("http://test-uri/F302", 1)).thenReturn("preSignedUrl");
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("No data for timestamp " + timestamp + " in RAOREQUEST file");
    }

    @Test
    void missingOtherInputFileTest() throws RaoRequestImportException {
        final ProcessFileDto raoRequestProcessFile = new ProcessFileDto("http://test-uri/F302", "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse("2024-06-18T09:30Z"), TaskStatus.READY, List.of(raoRequestProcessFile), List.of(raoRequestProcessFile), null, null, null, null);

        final RequestMessage requestMessage = new RequestMessage();
        final Payload payload = new Payload();
        final RequestItems requestItems = new RequestItems();
        final RequestItem requestItem = new RequestItem();
        final Files files = new Files();
        final File file = new File();
        requestMessage.setPayload(payload);
        payload.setRequestItems(requestItems);
        requestItem.setTimeInterval("2024-06-18T09:00Z/2024-06-18T10:00Z");
        requestItem.setFiles(files);
        requestItems.getRequestItem().add(requestItem);
        files.getFile().add(file);
        file.setUrl("documentIdentification://test-document-id");

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("http://test-uri/F302", 1)).thenReturn("preSignedUrl");
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("Some input files are missing, the task can't be launched")
                .havingCause()
                .isExactlyInstanceOf(MissingFileException.class)
                .withMessage("No file found in task 2024-06-18T09:30Z matching DocumentId test-document-id");
    }

    @Test
    void missingVirtualhubFileTest() throws RaoRequestImportException {
        final ProcessFileDto raoRequestProcessFile = new ProcessFileDto("http://test-uri/F302", "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        final String cgmFilePath = "http://test-uri/F119";
        final ProcessFileDto cgmProcessFile = new ProcessFileDto(cgmFilePath, "CGM", ProcessFileStatus.VALIDATED, "cgm.zip", "cgm-document-id", OffsetDateTime.now());
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse("2024-06-18T09:30Z"), TaskStatus.READY, List.of(raoRequestProcessFile, cgmProcessFile), List.of(raoRequestProcessFile, cgmProcessFile), null, null, null, null);

        final RequestMessage requestMessage = new RequestMessage();
        final Payload payload = new Payload();
        final RequestItems requestItems = new RequestItems();
        final RequestItem requestItem = new RequestItem();
        final Files files = new Files();
        final File file = new File();
        requestMessage.setPayload(payload);
        payload.setRequestItems(requestItems);
        requestItem.setTimeInterval("2024-06-18T09:00Z/2024-06-18T10:00Z");
        requestItem.setFiles(files);
        requestItems.getRequestItem().add(requestItem);
        files.getFile().add(file);
        file.setUrl("documentIdentification://cgm-document-id");

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("http://test-uri/F302", 1)).thenReturn("preSignedUrl");
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cgmFilePath, 1)).thenReturn("cgm-presigned-url");

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("Some input files are missing, the task can't be launched")
                .havingCause()
                .isExactlyInstanceOf(MissingFileException.class)
                .withMessage("No VIRTUALHUB file found in task 2024-06-18T09:30Z");
    }

    @Test
    void failureInAddingNewRunInTaskHistoryTest() throws RaoRequestImportException {
        final boolean automaticLauch = false;
        final UUID taskId = UUID.randomUUID();
        final UUID currentRunId = UUID.randomUUID();
        final OffsetDateTime taskTimestamp = OffsetDateTime.parse("2024-06-18T09:30Z");
        final Setup testSetup = getTaskDtoWithSingleVeresionOfEachFileExceptDcCgm(taskId, taskTimestamp, currentRunId);
        final TaskDto taskDto = testSetup.taskDto();

        Mockito.when(taskManagerService.addNewRunInTaskHistory(Mockito.anyString(), Mockito.anyList())).thenReturn(Optional.empty());

        service.handleTask(taskDto, automaticLauch);

        final ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        Mockito.verifyNoInteractions(coreCCClient);
        Mockito.verify(eventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.eq(taskDto.getTimestamp()));
        Mockito.verify(streamBridge, Mockito.times(1)).send(Mockito.anyString(), captor.capture());
        final TaskStatusUpdate taskStatusUpdate = captor.getValue();
        Assertions.assertThat(taskStatusUpdate)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", taskId)
                .hasFieldOrPropertyWithValue("taskStatus", TaskStatus.ERROR);
    }

    @Test
    void failureInUpdatingTaskStatusTest() throws RaoRequestImportException {
        final boolean automaticLauch = false;
        final UUID taskId = UUID.randomUUID();
        final UUID currentRunId = UUID.randomUUID();
        final OffsetDateTime taskTimestamp = OffsetDateTime.parse("2024-06-18T09:30Z");
        final Setup testSetup = getTaskDtoWithSingleVeresionOfEachFileExceptDcCgm(taskId, taskTimestamp, currentRunId);
        final TaskDto taskDto = testSetup.taskDto();
        final TaskDto updatedTaskDto = testSetup.updatedTaskDto();

        Mockito.when(taskManagerService.addNewRunInTaskHistory(Mockito.anyString(), Mockito.anyList())).thenReturn(Optional.of(updatedTaskDto));
        Mockito.when(taskManagerService.updateTaskStatus(Mockito.anyString(), Mockito.eq(TaskStatus.PENDING))).thenReturn(false);

        service.handleTask(taskDto, automaticLauch);

        final ArgumentCaptor<TaskStatusUpdate> captor = ArgumentCaptor.forClass(TaskStatusUpdate.class);
        Mockito.verifyNoInteractions(coreCCClient);
        Mockito.verify(eventsLogger, Mockito.times(1)).warn(Mockito.anyString(), Mockito.eq(taskDto.getTimestamp()));
        Mockito.verify(streamBridge, Mockito.times(1)).send(Mockito.anyString(), captor.capture());
        final TaskStatusUpdate taskStatusUpdate = captor.getValue();
        Assertions.assertThat(taskStatusUpdate)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", taskId)
                .hasFieldOrPropertyWithValue("taskStatus", TaskStatus.ERROR);
    }

    @Test
    void exceptionIfRunIsNotAddedProperlyTest() throws RaoRequestImportException {
        final boolean automaticLauch = false;
        final UUID taskId = UUID.randomUUID();
        final UUID currentRunId = UUID.randomUUID();
        final OffsetDateTime taskTimestamp = OffsetDateTime.parse("2024-06-18T09:30Z");
        final Setup testSetup = getTaskDtoWithSingleVeresionOfEachFileExceptDcCgm(taskId, taskTimestamp, currentRunId);
        final TaskDto taskDto = testSetup.taskDto();

        Mockito.when(taskManagerService.addNewRunInTaskHistory(Mockito.anyString(), Mockito.anyList())).thenReturn(Optional.of(taskDto)); // return the taskDto without RunHistory here
        Mockito.when(taskManagerService.updateTaskStatus(Mockito.anyString(), Mockito.eq(TaskStatus.PENDING))).thenReturn(true);

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, automaticLauch));
    }

    @Test
    void singleVersionOfEachFilePresentExceptDcCgmTest() throws RaoRequestImportException {
        final boolean automaticLauch = false;
        final UUID taskId = UUID.randomUUID();
        final UUID currentRunId = UUID.randomUUID();
        final OffsetDateTime taskTimestamp = OffsetDateTime.parse("2024-06-18T09:30Z");
        final Setup testSetup = getTaskDtoWithSingleVeresionOfEachFileExceptDcCgm(taskId, taskTimestamp, currentRunId);
        final TaskDto taskDto = testSetup.taskDto();
        final TaskDto updatedTaskDto = testSetup.updatedTaskDto();

        Mockito.when(taskManagerService.addNewRunInTaskHistory(Mockito.anyString(), Mockito.anyList())).thenReturn(Optional.of(updatedTaskDto));
        Mockito.when(taskManagerService.updateTaskStatus(Mockito.anyString(), Mockito.eq(TaskStatus.PENDING))).thenReturn(true);

        service.handleTask(taskDto, automaticLauch);

        final ArgumentCaptor<CoreCCRequest> coreCCRequestArgumentCaptor = ArgumentCaptor.forClass(CoreCCRequest.class);
        Mockito.verify(coreCCClient, Mockito.timeout(100).times(1)).run(coreCCRequestArgumentCaptor.capture());
        final CoreCCRequest coreCCRequest = coreCCRequestArgumentCaptor.getValue();
        Assertions.assertThat(coreCCRequest)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", taskId.toString())
                .hasFieldOrPropertyWithValue("timestamp", taskTimestamp)
                .hasFieldOrPropertyWithValue("launchedAutomatically", automaticLauch)
                .hasFieldOrPropertyWithValue("currentRunId", currentRunId.toString())
                .hasFieldOrPropertyWithValue("taskParameterList", List.of());
        Assertions.assertThat(coreCCRequest.getCgm())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "cgm.zip")
                .hasFieldOrPropertyWithValue("url", "cgm-presigned-url");
        Assertions.assertThat(coreCCRequest.getDcCgm())
                .isNull();
        Assertions.assertThat(coreCCRequest.getCbcora())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "cbcora.xml")
                .hasFieldOrPropertyWithValue("url", "cbcora-presigned-url");
        Assertions.assertThat(coreCCRequest.getGlsk())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "glsk.xml")
                .hasFieldOrPropertyWithValue("url", "glsk-presigned-url");
        Assertions.assertThat(coreCCRequest.getRefProg())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "refprog.xml")
                .hasFieldOrPropertyWithValue("url", "refprog-presigned-url");
        Assertions.assertThat(coreCCRequest.getRaoRequest())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "raorequest.xml")
                .hasFieldOrPropertyWithValue("url", "raorequest-presigned-url");
        Assertions.assertThat(coreCCRequest.getVirtualHub())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "virtualhub.xml")
                .hasFieldOrPropertyWithValue("url", "virtualhub-presigned-url");
    }

    @Test
    void singleVersionOfEachFilePresentTest() throws RaoRequestImportException {
        final boolean automaticLaunch = true;
        final UUID taskId = UUID.randomUUID();
        final UUID currentRunId = UUID.randomUUID();
        final OffsetDateTime taskTimestamp = OffsetDateTime.parse("2024-06-18T09:30Z");
        final Setup testSetup = getTaskDtoWithSingleVeresionOfEachFile(taskId, taskTimestamp, currentRunId);
        final TaskDto taskDto = testSetup.taskDto();
        final TaskDto updatedTaskDto = testSetup.updatedTaskDto();

        Mockito.when(taskManagerService.addNewRunInTaskHistory(Mockito.anyString(), Mockito.anyList())).thenReturn(Optional.of(updatedTaskDto));
        Mockito.when(taskManagerService.updateTaskStatus(Mockito.anyString(), Mockito.eq(TaskStatus.PENDING))).thenReturn(true);

        service.handleTask(taskDto, automaticLaunch);

        final ArgumentCaptor<CoreCCRequest> coreCCRequestArgumentCaptor = ArgumentCaptor.forClass(CoreCCRequest.class);
        Mockito.verify(coreCCClient, Mockito.timeout(100).times(1)).run(coreCCRequestArgumentCaptor.capture());
        final CoreCCRequest coreCCRequest = coreCCRequestArgumentCaptor.getValue();
        Assertions.assertThat(coreCCRequest)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", taskId.toString())
                .hasFieldOrPropertyWithValue("timestamp", taskTimestamp)
                .hasFieldOrPropertyWithValue("launchedAutomatically", automaticLaunch)
                .hasFieldOrPropertyWithValue("currentRunId", currentRunId.toString())
                .hasFieldOrPropertyWithValue("taskParameterList", List.of());
        Assertions.assertThat(coreCCRequest.getCgm())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "cgm.zip")
                .hasFieldOrPropertyWithValue("url", "cgm-presigned-url");
        Assertions.assertThat(coreCCRequest.getDcCgm())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "dccgm.zip")
                .hasFieldOrPropertyWithValue("url", "dccgm-presigned-url");
        Assertions.assertThat(coreCCRequest.getCbcora())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "cbcora.xml")
                .hasFieldOrPropertyWithValue("url", "cbcora-presigned-url");
        Assertions.assertThat(coreCCRequest.getGlsk())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "glsk.xml")
                .hasFieldOrPropertyWithValue("url", "glsk-presigned-url");
        Assertions.assertThat(coreCCRequest.getRefProg())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "refprog.xml")
                .hasFieldOrPropertyWithValue("url", "refprog-presigned-url");
        Assertions.assertThat(coreCCRequest.getRaoRequest())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "raorequest.xml")
                .hasFieldOrPropertyWithValue("url", "raorequest-presigned-url");
        Assertions.assertThat(coreCCRequest.getVirtualHub())
                .isNotNull()
                .hasFieldOrPropertyWithValue("filename", "virtualhub.xml")
                .hasFieldOrPropertyWithValue("url", "virtualhub-presigned-url");
    }

    @Test
    void handleRequestWithParametersTest() throws RaoRequestImportException {
        final boolean automaticLauch = false;
        final UUID taskId = UUID.randomUUID();
        final UUID currentRunId = UUID.randomUUID();
        final OffsetDateTime taskTimestamp = OffsetDateTime.parse("2024-06-18T09:30Z");
        final Setup testSetup = getTaskDtoWithSingleVeresionOfEachFileExceptDcCgm(taskId, taskTimestamp, currentRunId);
        final TaskDto taskDto = testSetup.taskDto();
        final TaskDto updatedTaskDto = testSetup.updatedTaskDto();
        final List<TaskParameterDto> parameters = List.of(Mockito.mock(TaskParameterDto.class));

        Mockito.when(taskManagerService.addNewRunInTaskHistory(Mockito.anyString(), Mockito.anyList())).thenReturn(Optional.of(updatedTaskDto));
        Mockito.when(taskManagerService.updateTaskStatus(Mockito.anyString(), Mockito.eq(TaskStatus.PENDING))).thenReturn(true);

        service.handleTask(taskDto, automaticLauch, parameters);

        final ArgumentCaptor<CoreCCRequest> coreCCRequestArgumentCaptor = ArgumentCaptor.forClass(CoreCCRequest.class);
        Mockito.verify(coreCCClient, Mockito.timeout(100).times(1)).run(coreCCRequestArgumentCaptor.capture());
        final CoreCCRequest coreCCRequest = coreCCRequestArgumentCaptor.getValue();
        Assertions.assertThat(coreCCRequest).isNotNull();
        Assertions.assertThat(coreCCRequest.getTaskParameterList())
                .containsExactlyElementsOf(parameters);
    }

    private Setup getTaskDtoWithSingleVeresionOfEachFileExceptDcCgm(UUID taskId, OffsetDateTime taskTimestamp, UUID currentRunId) throws RaoRequestImportException {
        final String raorequestFilePath = "http://test-uri/RAOREQUEST";
        final ProcessFileDto raoRequestProcessFile = new ProcessFileDto(raorequestFilePath, "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        final String cgmFilePath = "http://test-uri/CGM";
        final ProcessFileDto cgmProcessFile = new ProcessFileDto(cgmFilePath, "CGM", ProcessFileStatus.VALIDATED, "cgm.zip", "cgm-document-id", OffsetDateTime.now());
        final String glskFilePath = "http://test-uri/GLSK";
        final ProcessFileDto glskProcessFile = new ProcessFileDto(glskFilePath, "GLSK", ProcessFileStatus.VALIDATED, "glsk.xml", "glsk-document-id", OffsetDateTime.now());
        final String cbcoraFilePath = "http://test-uri/CBCORA";
        final ProcessFileDto cbcoraProcessFile = new ProcessFileDto(cbcoraFilePath, "CBCORA", ProcessFileStatus.VALIDATED, "cbcora.xml", "cbcora-document-id", OffsetDateTime.now());
        final String refprogFilePath = "http://test-uri/REFPROG";
        final ProcessFileDto refprogProcessFile = new ProcessFileDto(refprogFilePath, "REFPROG", ProcessFileStatus.VALIDATED, "refprog.xml", "refprog-document-id", OffsetDateTime.now());
        final String virtualhubFilePath = "http://test-uri/VIRTUALHUB";
        final ProcessFileDto virtualhubProcessFile = new ProcessFileDto(virtualhubFilePath, "VIRTUALHUB", ProcessFileStatus.VALIDATED, "virtualhub.xml", "virtualhub-document-id", OffsetDateTime.now());
        final List<ProcessFileDto> inputs = List.of(raoRequestProcessFile, cgmProcessFile, glskProcessFile, cbcoraProcessFile, refprogProcessFile, virtualhubProcessFile);
        final List<ProcessFileDto> availableInputs = List.of(raoRequestProcessFile, cgmProcessFile, glskProcessFile, cbcoraProcessFile, refprogProcessFile, virtualhubProcessFile);
        final TaskDto taskDto = new TaskDto(taskId, taskTimestamp, TaskStatus.READY,
                inputs,
                availableInputs,
                null, null, null, null);
        final List<ProcessRunDto> runHistory = new ArrayList<>();
        runHistory.add(new ProcessRunDto(UUID.randomUUID(), taskTimestamp, inputs));
        runHistory.add(new ProcessRunDto(currentRunId, OffsetDateTime.parse("2024-09-18T09:30Z"), inputs));
        final TaskDto updatedTaskDto = new TaskDto(taskId, taskTimestamp, TaskStatus.READY,
                inputs,
                availableInputs,
                null, null, runHistory, List.of());
        final RequestMessage requestMessage = new RequestMessage();
        final Payload payload = new Payload();
        final RequestItems requestItems = new RequestItems();
        final RequestItem requestItem = new RequestItem();
        final Files files = new Files();
        final File cgmFile = new File();
        final File glskFile = new File();
        final File cbcoraFile = new File();
        final File refprogFile = new File();
        final File virtualhubFile = new File();
        requestMessage.setPayload(payload);
        payload.setRequestItems(requestItems);
        requestItem.setTimeInterval("2024-06-18T09:00Z/2024-06-18T10:00Z");
        requestItem.setFiles(files);
        requestItems.getRequestItem().add(requestItem);
        cgmFile.setUrl("documentIdentification://cgm-document-id");
        glskFile.setUrl("documentIdentification://glsk-document-id");
        cbcoraFile.setUrl("documentIdentification://cbcora-document-id");
        refprogFile.setUrl("documentIdentification://refprog-document-id");
        virtualhubFile.setUrl("documentIdentification://virtualhub-document-id");
        files.getFile().add(cgmFile);
        files.getFile().add(glskFile);
        files.getFile().add(cbcoraFile);
        files.getFile().add(refprogFile);
        files.getFile().add(virtualhubFile);

        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(raorequestFilePath, 1)).thenReturn("raorequest-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cgmFilePath, 1)).thenReturn("cgm-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(glskFilePath, 1)).thenReturn("glsk-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cbcoraFilePath, 1)).thenReturn("cbcora-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(refprogFilePath, 1)).thenReturn("refprog-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(virtualhubFilePath, 1)).thenReturn("virtualhub-presigned-url");

        return new Setup(taskDto, updatedTaskDto, requestMessage);
    }

    private Setup getTaskDtoWithSingleVeresionOfEachFile(UUID taskId, OffsetDateTime taskTimestamp, UUID currentRunId) throws RaoRequestImportException {
        final Setup setupWithoutDcCgm = getTaskDtoWithSingleVeresionOfEachFileExceptDcCgm(taskId, taskTimestamp, currentRunId);
        final String dccgmFilePath = "http://test-uri/DCCGM";
        final ProcessFileDto dccgmProcessFile = new ProcessFileDto(dccgmFilePath, "DCCGM", ProcessFileStatus.VALIDATED, "dccgm.zip", "dccgm-document-id", OffsetDateTime.now());
        final TaskDto taskDtoWithoutDcCgm = setupWithoutDcCgm.taskDto();
        final TaskDto updatedTaskDtoWithoutDcCgm = setupWithoutDcCgm.updatedTaskDto();
        final List<ProcessFileDto> inputs = new ArrayList<>(taskDtoWithoutDcCgm.getInputs());
        inputs.add(dccgmProcessFile);
        final List<ProcessFileDto> availableInputs = new ArrayList<>(taskDtoWithoutDcCgm.getAvailableInputs());
        availableInputs.add(dccgmProcessFile);

        final TaskDto taskDto = new TaskDto(taskDtoWithoutDcCgm.getId(), taskDtoWithoutDcCgm.getTimestamp(), taskDtoWithoutDcCgm.getStatus(),
                inputs, availableInputs,
                taskDtoWithoutDcCgm.getOutputs(), taskDtoWithoutDcCgm.getProcessEvents(), taskDtoWithoutDcCgm.getRunHistory(), taskDtoWithoutDcCgm.getParameters());

        final List<ProcessRunDto> runHistory = new ArrayList<>();
        runHistory.add(new ProcessRunDto(UUID.randomUUID(), taskTimestamp, inputs));
        runHistory.add(new ProcessRunDto(currentRunId, OffsetDateTime.parse("2024-09-18T09:30Z"), inputs));
        final TaskDto updatedTaskDto = new TaskDto(updatedTaskDtoWithoutDcCgm.getId(), updatedTaskDtoWithoutDcCgm.getTimestamp(), updatedTaskDtoWithoutDcCgm.getStatus(),
                inputs, availableInputs,
                updatedTaskDtoWithoutDcCgm.getOutputs(), updatedTaskDtoWithoutDcCgm.getProcessEvents(),
                runHistory,
                updatedTaskDtoWithoutDcCgm.getParameters());

        final File dccgmFile = new File();
        dccgmFile.setUrl("documentIdentification://dccgm-document-id");
        setupWithoutDcCgm.requestMessage().getPayload().getRequestItems().getRequestItem().get(0).getFiles().getFile().add(dccgmFile);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(dccgmFilePath, 1)).thenReturn("dccgm-presigned-url");

        return new Setup(taskDto, updatedTaskDto, setupWithoutDcCgm.requestMessage());
    }

    private record Setup(TaskDto taskDto, TaskDto updatedTaskDto, RequestMessage requestMessage) { }
}
