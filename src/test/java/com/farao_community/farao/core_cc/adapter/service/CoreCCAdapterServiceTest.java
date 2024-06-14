///*
// * Copyright (c) 2023, RTE (http://www.rte-france.com)
// * This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/.
// */
//package com.farao_community.farao.core_cc.adapter.service;
//
//import com.farao_community.farao.core_cc.adapter.exception.RaoRequestImportException;
//import com.farao_community.farao.gridcapa.task_manager.api.ProcessEventDto;
//import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
//import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
//import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
//import com.farao_community.farao.gridcapa.task_manager.api.TaskParameterDto;
//import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
//import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCRequest;
//import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.function.ThrowingSupplier;
//import org.mockito.Mockito;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//
//import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
//
///**
// * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
// */
//@SpringBootTest
//class CoreCCAdapterServiceTest {
//
//    @Autowired
//    private CoreCCAdapterService coreCCAdapterService;
//
//    @MockBean
//    private MinioAdapter minioAdapter;
//
//    private String cgmFileType;
//    private String dcCgmFileType;
//    private String cbcoraFileType;
//    private String glskFileType;
//    private String raoRequestFileType;
//    private String refprogFileType;
//    private String virtualHubFileType;
//    private String cgmFileName;
//    private String dcCgmFileName;
//    private String cbcoraFileName;
//    private String glskFileName;
//    private String raoRequestFileName;
//    private String refprogFileName;
//    private String virtualHubFileName;
//    private String cgmFileUrl;
//    private String dcCgmFileUrl;
//    private String cbcoraFileUrl;
//    private String glskFileUrl;
//    private String raoRequestFileUrl;
//    private String refprogFileUrl;
//    private String virtualHubFileUrl;
//    private String cgmFilePath;
//    private String dcCgmFilePath;
//    private String cbcoraFilePath;
//    private String glskFilePath;
//    private String raoRequestFilePath;
//    private String refprogFilePath;
//    private String virtualHubFilePath;
//
//    public TaskDto createTaskDtoWithStatus(TaskStatus status) {
//        UUID id = UUID.randomUUID();
//        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
//        List<ProcessFileDto> processFiles = new ArrayList<>();
//        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, "documentIdCgm", timestamp));
//        processFiles.add(new ProcessFileDto(dcCgmFilePath, dcCgmFileType, ProcessFileStatus.VALIDATED, dcCgmFileName, "documentIdDcCgm", timestamp));
//        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, "documentIdCbcora", timestamp));
//        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, "documentIdGlsk", timestamp));
//        processFiles.add(new ProcessFileDto(raoRequestFilePath, raoRequestFileType, ProcessFileStatus.VALIDATED, raoRequestFileName, null, timestamp));
//        processFiles.add(new ProcessFileDto(refprogFilePath, refprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, "documentIdRefprog", timestamp));
//        processFiles.add(new ProcessFileDto(virtualHubFilePath, virtualHubFileType, ProcessFileStatus.VALIDATED, virtualHubFileName, null, timestamp));
//        List<ProcessEventDto> processEvents = new ArrayList<>();
//        List<TaskParameterDto> parameters = new ArrayList<>();
//        return new TaskDto(id, timestamp, status, processFiles, processFiles, null, processEvents, null, parameters);
//    }
//
//    public TaskDto createTaskDtoWithStatusWithDcCgmAbsent(TaskStatus status) {
//        UUID id = UUID.randomUUID();
//        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
//        List<ProcessFileDto> processFiles = new ArrayList<>();
//        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, "documentIdCgm", timestamp));
//        processFiles.add(new ProcessFileDto(null, dcCgmFileType, null, null, null, null));
//        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, "documentIdCbcora", timestamp));
//        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, "documentIdGlsk", timestamp));
//        processFiles.add(new ProcessFileDto(raoRequestFilePath, raoRequestFileType, ProcessFileStatus.VALIDATED, raoRequestFileName, null, timestamp));
//        processFiles.add(new ProcessFileDto(refprogFilePath, refprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, "documentIdRefprog", timestamp));
//        processFiles.add(new ProcessFileDto(virtualHubFilePath, virtualHubFileType, ProcessFileStatus.VALIDATED, virtualHubFileName, null, timestamp));
//        List<ProcessEventDto> processEvents = new ArrayList<>();
//        List<TaskParameterDto> parameters = new ArrayList<>();
//        return new TaskDto(id, timestamp, status, processFiles, processFiles, null, processEvents, null, parameters);
//    }
//
//    @BeforeEach
//    void setUp() {
//        cgmFileType = "CGM";
//        dcCgmFileType = "DCCGM";
//        cbcoraFileType = "CBCORA";
//        glskFileType = "GLSK";
//        raoRequestFileType = "RAOREQUEST";
//        refprogFileType = "REFPROG";
//        virtualHubFileType = "VIRTUALHUB";
//
//        cgmFileName = "cgm";
//        dcCgmFileName = "dccgm";
//        cbcoraFileName = "cbcora";
//        glskFileName = "glsk";
//        raoRequestFileName = "raorequest";
//        refprogFileName = "refprog";
//        virtualHubFileName = "virtualhub";
//
//        cgmFilePath = "/CGM";
//        dcCgmFilePath = "/DCCGM";
//        cbcoraFilePath = "/CBCORA";
//        glskFilePath = "/GLSK";
//        raoRequestFilePath = "/RAOREQUEST";
//        refprogFilePath = "/REFPROG";
//        virtualHubFilePath = "/VIRTUALHUB";
//
//        cgmFileUrl = "file://CGM/cgm.uct";
//        dcCgmFileUrl = "file://DCCGM/dccgm.uct";
//        cbcoraFileUrl = "file://CBCORA/cbcora.xml";
//        glskFileUrl = "file://GLSK/glsk.xml";
//        raoRequestFileUrl = "file://RAOREQUEST/raorequest.xml";
//        refprogFileUrl = "file://REFPROG/refprog.xml";
//        virtualHubFileUrl = "file://VIRTUALHUB/virtualhub.xml";
//
//        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cgmFilePath, 1)).thenReturn(cgmFileUrl);
//        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(dcCgmFilePath, 1)).thenReturn(dcCgmFileUrl);
//        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cbcoraFilePath, 1)).thenReturn(cbcoraFileUrl);
//        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(glskFilePath, 1)).thenReturn(glskFileUrl);
//        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(raoRequestFilePath, 1)).thenReturn(raoRequestFileUrl);
//        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(refprogFilePath, 1)).thenReturn(refprogFileUrl);
//        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(virtualHubFilePath, 1)).thenReturn(virtualHubFileUrl);
//    }
//
//    @Test
//    void testGetManualCoreCCRequest() throws RaoRequestImportException {
//        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
//        CoreCCRequest coreCCRequest = coreCCAdapterService.getCoreCCRequest(taskDto, false);
//        Assertions.assertEquals(taskDto.getId().toString(), coreCCRequest.getId());
//        Assertions.assertEquals(cgmFileName, coreCCRequest.getCgm().getFilename());
//        Assertions.assertEquals(cgmFileUrl, coreCCRequest.getCgm().getUrl());
//        Assertions.assertFalse(coreCCRequest.getLaunchedAutomatically());
//    }
//
//    @Test
//    void testGetManualCoreCCRequestWithoutDcCgm() {
//        TaskDto taskDto = createTaskDtoWithStatusWithDcCgmAbsent(TaskStatus.READY);
//        CoreCCRequest coreCCRequest = coreCCAdapterService.getCoreCCRequest(taskDto, false);
//        Assertions.assertEquals(taskDto.getId().toString(), coreCCRequest.getId());
//        Assertions.assertEquals(cgmFileName, coreCCRequest.getCgm().getFilename());
//        Assertions.assertEquals(cgmFileUrl, coreCCRequest.getCgm().getUrl());
//        //if optional input absent, file resource shall be left null
//        Assertions.assertNull(coreCCRequest.getDcCgm());
//    }
//
//    @Test
//    void testGetAutomaticCoreCCRequest() {
//        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
//        CoreCCRequest coreCCRequest = coreCCAdapterService.getCoreCCRequest(taskDto, true);
//        Assertions.assertEquals(cbcoraFileName, coreCCRequest.getCbcora().getFilename());
//        Assertions.assertEquals(cbcoraFileUrl, coreCCRequest.getCbcora().getUrl());
//        Assertions.assertEquals(glskFileName, coreCCRequest.getGlsk().getFilename());
//        Assertions.assertEquals(glskFileUrl, coreCCRequest.getGlsk().getUrl());
//        Assertions.assertTrue(coreCCRequest.getLaunchedAutomatically());
//    }
//
//    @Test
//    void testGetCoreCCRequestWithIncorrectFiles() {
//        String wrongRefprogFileType = "REF-PROG";
//        UUID id = UUID.randomUUID();
//        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
//        List<ProcessFileDto> processFiles = new ArrayList<>();
//        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, "documentIdCgm", timestamp));
//        processFiles.add(new ProcessFileDto(dcCgmFilePath, dcCgmFileType, ProcessFileStatus.VALIDATED, dcCgmFileName, "documentIdDcCgm", timestamp));
//        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, "documentIdCbcora", timestamp));
//        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, "documentIdGlsk", timestamp));
//        processFiles.add(new ProcessFileDto(raoRequestFilePath, raoRequestFileType, ProcessFileStatus.VALIDATED, raoRequestFileName, null, timestamp));
//        processFiles.add(new ProcessFileDto(refprogFilePath, wrongRefprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, "documentIdRefprog", timestamp));
//        processFiles.add(new ProcessFileDto(virtualHubFilePath, virtualHubFileType, ProcessFileStatus.VALIDATED, virtualHubFileName, null, timestamp));
//        List<ProcessEventDto> processEvents = new ArrayList<>();
//        List<TaskParameterDto> parameters = new ArrayList<>();
//        TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, processFiles, processFiles, null, processEvents, null, parameters);
//        Assertions.assertThrows(IllegalStateException.class, () -> coreCCAdapterService.getCoreCCRequest(taskDto, false));
//    }
//
//    @Test
//    void testHandleManualTaskDoesNotThrowException() {
//        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
//        coreCCAdapterService.handleTask(taskDto, false);
//        assertDoesNotThrow((ThrowingSupplier<RuntimeException>) RuntimeException::new);
//    }
//
//    @Test
//    void testHandleAutoTaskDoesNotThrowException() {
//        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
//        coreCCAdapterService.handleTask(taskDto, true);
//        assertDoesNotThrow((ThrowingSupplier<RuntimeException>) RuntimeException::new);
//    }
//}
