/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.app;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessEventDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCRequest;
import com.farao_community.farao.gridcapa_core_cc.starter.CoreCCClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class CoreCCAdapterListenerTest {

    @MockBean
    private CoreCCClient coreCCClient;

    @MockBean
    private MinioAdapter minioAdapter;

    @Captor
    ArgumentCaptor<CoreCCRequest> argumentCaptor;

    @Autowired
    private com.farao_community.farao.core_cc.adapter.app.CoreCCAdapterListener coreCCAdapterListener;
    private String cgmFileType;
    private String cbcoraFileType;
    private String glskFileType;
    private String raoRequestFileType;
    private String refprogFileType;
    private String virtualHubFileType;
    private String cgmFileName;
    private String cbcoraFileName;
    private String glskFileName;
    private String raoRequestFileName;
    private String refprogFileName;
    private String virtualHubFileName;
    private String cgmFileUrl;
    private String cbcoraFileUrl;
    private String glskFileUrl;
    private String raoRequestFileUrl;
    private String refprogFileUrl;
    private String virtualHubFileUrl;
    private String cgmFilePath;
    private String cbcoraFilePath;
    private String glskFilePath;
    private String raoRequestFilePath;
    private String refprogFilePath;
    private String virtualHubFilePath;

    public TaskDto createTaskDtoWithStatus(TaskStatus status) {
        UUID id = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, timestamp));
        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, timestamp));
        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, timestamp));
        processFiles.add(new ProcessFileDto(raoRequestFilePath, raoRequestFileType, ProcessFileStatus.VALIDATED, raoRequestFileName, timestamp));
        processFiles.add(new ProcessFileDto(refprogFilePath, refprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, timestamp));
        processFiles.add(new ProcessFileDto(virtualHubFilePath, virtualHubFileType, ProcessFileStatus.VALIDATED, virtualHubFileName, timestamp));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        return new TaskDto(id, timestamp, status, null, processFiles, null, processEvents);
    }

    @BeforeEach
    void setUp() {
        cgmFileType = "CGM";
        cbcoraFileType = "CBCORA";
        glskFileType = "GLSK";
        raoRequestFileType = "RAOREQUEST";
        refprogFileType = "REFPROG";
        virtualHubFileType = "VIRTUALHUB";

        cgmFileName = "cgm";
        cbcoraFileName = "cbcora";
        glskFileName = "glsk";
        raoRequestFileName = "raorequest";
        refprogFileName = "refprog";
        virtualHubFileName = "virtualhub";

        cgmFilePath = "/CGM";
        cbcoraFilePath = "/CBCORA";
        glskFilePath = "/GLSK";
        raoRequestFilePath = "/RAOREQUEST";
        refprogFilePath = "/REFPROG";
        virtualHubFilePath = "/VIRTUALHUB";

        cgmFileUrl = "file://CGM/cgm.uct";
        cbcoraFileUrl = "file://CBCORA/cbcora.xml";
        glskFileUrl = "file://GLSK/glsk.xml";
        raoRequestFileUrl = "file://STUDYPOINTS/raorequest.xml";
        refprogFileUrl = "file://REFPROG/refprog.xml";
        virtualHubFileUrl = "file://REFPROG/virtualhub.xml";

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cgmFilePath, 1)).thenReturn(cgmFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cbcoraFilePath, 1)).thenReturn(cbcoraFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(glskFilePath, 1)).thenReturn(glskFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(raoRequestFilePath, 1)).thenReturn(raoRequestFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(refprogFilePath, 1)).thenReturn(refprogFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(virtualHubFilePath, 1)).thenReturn(virtualHubFileUrl);
    }

    @Test
    void testGetManualCoreCCRequest() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        CoreCCRequest coreCCRequest = coreCCAdapterListener.getManualCoreCCRequest(taskDto);
        Assertions.assertEquals(taskDto.getId().toString(), coreCCRequest.getId());
        Assertions.assertEquals(cgmFileName, coreCCRequest.getCgm().getFilename());
        Assertions.assertEquals(cgmFileUrl, coreCCRequest.getCgm().getUrl());
        Assertions.assertFalse(coreCCRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetAutomaticCoreCCRequest() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        CoreCCRequest coreCCRequest = coreCCAdapterListener.getAutomaticCoreCCRequest(taskDto);
        Assertions.assertTrue(coreCCRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetCoreCCRequestWithIncorrectFiles() {
        String wrongRefprogFileType = "REF-PROG";
        UUID id = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, timestamp));
        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, timestamp));
        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, timestamp));
        processFiles.add(new ProcessFileDto(raoRequestFilePath, raoRequestFileType, ProcessFileStatus.VALIDATED, raoRequestFileName, timestamp));
        processFiles.add(new ProcessFileDto(refprogFilePath, wrongRefprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, timestamp));
        processFiles.add(new ProcessFileDto(virtualHubFilePath, virtualHubFileType, ProcessFileStatus.VALIDATED, virtualHubFileName, timestamp));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, null, processFiles, null, processEvents);
        Assertions.assertThrows(IllegalStateException.class, () -> coreCCAdapterListener.getManualCoreCCRequest(taskDto));

    }

    @Test
    void consumeReadyAutoTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        coreCCAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreCCClient).run(argumentCaptor.capture());
        CoreCCRequest coreCCRequest = argumentCaptor.getValue();
        assert coreCCRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeReadyTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        coreCCAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreCCClient).run(argumentCaptor.capture());
        CoreCCRequest coreCCRequest = argumentCaptor.getValue();
        Assertions.assertFalse(coreCCRequest.getLaunchedAutomatically());
    }

    @Test
    void consumeSuccessAutoTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.SUCCESS);
        coreCCAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreCCClient).run(argumentCaptor.capture());
        CoreCCRequest coreCCRequest = argumentCaptor.getValue();
        assert coreCCRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeSuccessTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.SUCCESS);
        coreCCAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreCCClient).run(argumentCaptor.capture());
        CoreCCRequest coreCCRequest = argumentCaptor.getValue();
        Assertions.assertFalse(coreCCRequest.getLaunchedAutomatically());
    }

    @Test
    void consumeErrorAutoTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.ERROR);
        coreCCAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreCCClient).run(argumentCaptor.capture());
        CoreCCRequest coreCCRequest = argumentCaptor.getValue();
        assert coreCCRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeErrorTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.ERROR);
        coreCCAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreCCClient).run(argumentCaptor.capture());
        CoreCCRequest coreCCRequest = argumentCaptor.getValue();
        Assertions.assertFalse(coreCCRequest.getLaunchedAutomatically());
    }

    @Test
    void consumeCreatedAutoTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.CREATED);
        coreCCAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreCCClient, Mockito.never()).run(argumentCaptor.capture());
    }

    @Test
    void consumeCreatedTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.CREATED);
        coreCCAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreCCClient, Mockito.never()).run(argumentCaptor.capture());
    }

//    @Test
//    void consumeAutoTaskThrowingError() {
//        Mockito.when(coreCCClient.run(Mockito.any())).thenThrow(new CoreCCInternalException("message"));
//        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.ERROR);
//        Consumer<TaskDto> taskDtoConsumer = coreCCAdapterListener.consumeAutoTask();
//        Assertions.assertThrows(CoreCCAdapterException.class, () -> taskDtoConsumer.accept(taskDto));
//    }
//
//    @Test
//    void consumeTaskThrowingError() {
//        Mockito.when(coreCCClient.run(Mockito.any())).thenThrow(new CoreCCInternalException("message"));
//        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.ERROR);
//        Consumer<TaskDto> taskDtoConsumer = coreCCAdapterListener.consumeTask();
//        Assertions.assertThrows(CoreCCAdapterException.class, () -> taskDtoConsumer.accept(taskDto));
//    }
}
