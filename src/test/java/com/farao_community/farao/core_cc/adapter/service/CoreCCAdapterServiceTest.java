/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.exception.CoreCCAdapterException;
import com.farao_community.farao.core_cc.adapter.exception.MissingFileException;
import com.farao_community.farao.core_cc.adapter.exception.RaoRequestImportException;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
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
    private RestTemplate restTemplate;
    @MockBean
    private RestTemplateBuilder restTemplateBuilder;

    @BeforeEach
    void init() {
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    void missingRaoRequestFileTest() {
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), TaskStatus.READY, List.of(), List.of(), null, null, null, null);

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("Some input files are missing, the task can't be launched")
                .havingCause()
                .isInstanceOf(MissingFileException.class)
                .withMessageStartingWith("No RAOREQUEST file found in task");
    }

    @Test
    void errorWhenReadingRaorequestTest() throws RaoRequestImportException {
        ProcessFileDto raoRequestProcessFile = new ProcessFileDto("http://test-uri/F302", "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        OffsetDateTime timestamp = OffsetDateTime.now();
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), timestamp, TaskStatus.READY, List.of(raoRequestProcessFile), List.of(raoRequestProcessFile), null, null, null, null);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("http://test-uri/F302", 1)).thenReturn("preSignedUrl");
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenThrow(RaoRequestImportException.class);

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("Error occurred during loading of RAOREQUEST file content")
                .withCauseInstanceOf(RaoRequestImportException.class);
    }

    @Test
    void noDataInRaoRequestTest() throws RaoRequestImportException {
        ProcessFileDto raoRequestProcessFile = new ProcessFileDto("http://test-uri/F302", "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        OffsetDateTime timestamp = OffsetDateTime.now();
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), timestamp, TaskStatus.READY, List.of(raoRequestProcessFile), List.of(raoRequestProcessFile), null, null, null, null);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("http://test-uri/F302", 1)).thenReturn("preSignedUrl");
        RequestMessage requestMessage = new RequestMessage();
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);
        Payload payload = new Payload();
        requestMessage.setPayload(payload);
        RequestItems requestItems = new RequestItems();
        payload.setRequestItems(requestItems);

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("No data for timestamp " + timestamp + " in RAOREQUEST file");
    }

    @Test
    void missingOtherInputFileTest() throws RaoRequestImportException {
        ProcessFileDto raoRequestProcessFile = new ProcessFileDto("http://test-uri/F302", "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse("2024-06-18T09:30Z"), TaskStatus.READY, List.of(raoRequestProcessFile), List.of(raoRequestProcessFile), null, null, null, null);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("http://test-uri/F302", 1)).thenReturn("preSignedUrl");
        RequestMessage requestMessage = new RequestMessage();
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);
        Payload payload = new Payload();
        requestMessage.setPayload(payload);
        RequestItems requestItems = new RequestItems();
        payload.setRequestItems(requestItems);
        RequestItem requestItem = new RequestItem();
        requestItem.setTimeInterval("2024-06-18T09:00Z/2024-06-18T10:00Z");
        Files files = new Files();
        requestItem.setFiles(files);
        requestItems.getRequestItem().add(requestItem);
        File file = new File();
        files.getFile().add(file);
        file.setUrl("documentIdentification://test-document-id");

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("Some input files are missing, the task can't be launched")
                .havingCause()
                .isExactlyInstanceOf(MissingFileException.class)
                .withMessage("No file found in task 2024-06-18T09:30Z matching DocumentId test-document-id");
    }

    @Test
    void missingVirtualhubFileTest() throws RaoRequestImportException {
        ProcessFileDto raoRequestProcessFile = new ProcessFileDto("http://test-uri/F302", "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        String cgmFilePath = "http://test-uri/F119";
        ProcessFileDto cgmProcessFile = new ProcessFileDto(cgmFilePath, "CGM", ProcessFileStatus.VALIDATED, "cgm.zip", "cgm-document-id", OffsetDateTime.now());
        TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.parse("2024-06-18T09:30Z"), TaskStatus.READY, List.of(raoRequestProcessFile, cgmProcessFile), List.of(raoRequestProcessFile, cgmProcessFile), null, null, null, null);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath("http://test-uri/F302", 1)).thenReturn("preSignedUrl");
        RequestMessage requestMessage = new RequestMessage();
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);
        Payload payload = new Payload();
        requestMessage.setPayload(payload);
        RequestItems requestItems = new RequestItems();
        payload.setRequestItems(requestItems);
        RequestItem requestItem = new RequestItem();
        requestItem.setTimeInterval("2024-06-18T09:00Z/2024-06-18T10:00Z");
        Files files = new Files();
        requestItem.setFiles(files);
        requestItems.getRequestItem().add(requestItem);
        File file = new File();
        files.getFile().add(file);
        file.setUrl("documentIdentification://cgm-document-id");

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cgmFilePath, 1)).thenReturn("cgm-presigned-url");

        Assertions.assertThatExceptionOfType(CoreCCAdapterException.class)
                .isThrownBy(() -> service.handleTask(taskDto, false))
                .withMessage("Some input files are missing, the task can't be launched")
                .havingCause()
                .isExactlyInstanceOf(MissingFileException.class)
                .withMessage("No VIRTUALHUB file found in task 2024-06-18T09:30Z");
    }

    @Test
    void singleVersionOfEachFilePresentExceptDcCgmTest() throws RaoRequestImportException {
        UUID taskId = UUID.randomUUID();
        OffsetDateTime taskTimestamp = OffsetDateTime.parse("2024-06-18T09:30Z");
        String raorequestFilePath = "http://test-uri/RAOREQUEST";
        ProcessFileDto raoRequestProcessFile = new ProcessFileDto(raorequestFilePath, "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        String cgmFilePath = "http://test-uri/CGM";
        ProcessFileDto cgmProcessFile = new ProcessFileDto(cgmFilePath, "CGM", ProcessFileStatus.VALIDATED, "cgm.zip", "cgm-document-id", OffsetDateTime.now());
        String glskFilePath = "http://test-uri/GLSK";
        ProcessFileDto glskProcessFile = new ProcessFileDto(glskFilePath, "GLSK", ProcessFileStatus.VALIDATED, "glsk.xml", "glsk-document-id", OffsetDateTime.now());
        String cbcoraFilePath = "http://test-uri/CBCORA";
        ProcessFileDto cbcoraProcessFile = new ProcessFileDto(cbcoraFilePath, "CBCORA", ProcessFileStatus.VALIDATED, "cbcora.xml", "cbcora-document-id", OffsetDateTime.now());
        String refprogFilePath = "http://test-uri/REFPROG";
        ProcessFileDto refprogProcessFile = new ProcessFileDto(refprogFilePath, "REFPROG", ProcessFileStatus.VALIDATED, "refprog.xml", "refprog-document-id", OffsetDateTime.now());
        String virtualhubFilePath = "http://test-uri/VIRTUALHUB";
        ProcessFileDto virtualhubProcessFile = new ProcessFileDto(virtualhubFilePath, "VIRTUALHUB", ProcessFileStatus.VALIDATED, "virtualhub.xml", "virtualhub-document-id", OffsetDateTime.now());
        TaskDto taskDto = new TaskDto(taskId, taskTimestamp, TaskStatus.READY,
                List.of(raoRequestProcessFile, cgmProcessFile, glskProcessFile, cbcoraProcessFile, refprogProcessFile, virtualhubProcessFile),
                List.of(raoRequestProcessFile, cgmProcessFile, glskProcessFile, cbcoraProcessFile, refprogProcessFile, virtualhubProcessFile),
                null, null, null, null);

        RequestMessage requestMessage = new RequestMessage();
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);
        Payload payload = new Payload();
        requestMessage.setPayload(payload);
        RequestItems requestItems = new RequestItems();
        payload.setRequestItems(requestItems);
        RequestItem requestItem = new RequestItem();
        requestItem.setTimeInterval("2024-06-18T09:00Z/2024-06-18T10:00Z");
        Files files = new Files();
        requestItem.setFiles(files);
        requestItems.getRequestItem().add(requestItem);
        File cgmFile = new File();
        cgmFile.setUrl("documentIdentification://cgm-document-id");
        File glskFile = new File();
        glskFile.setUrl("documentIdentification://glsk-document-id");
        File cbcoraFile = new File();
        cbcoraFile.setUrl("documentIdentification://cbcora-document-id");
        File refprogFile = new File();
        refprogFile.setUrl("documentIdentification://refprog-document-id");
        File virtualhubFile = new File();
        virtualhubFile.setUrl("documentIdentification://virtualhub-document-id");
        files.getFile().add(cgmFile);
        files.getFile().add(glskFile);
        files.getFile().add(cbcoraFile);
        files.getFile().add(refprogFile);
        files.getFile().add(virtualhubFile);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(raorequestFilePath, 1)).thenReturn("raorequest-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cgmFilePath, 1)).thenReturn("cgm-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(glskFilePath, 1)).thenReturn("glsk-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cbcoraFilePath, 1)).thenReturn("cbcora-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(refprogFilePath, 1)).thenReturn("refprog-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(virtualhubFilePath, 1)).thenReturn("virtualhub-presigned-url");

        service.handleTask(taskDto, false);

        ArgumentCaptor<CoreCCRequest> coreCCRequestArgumentCaptor = ArgumentCaptor.forClass(CoreCCRequest.class);
        Mockito.verify(coreCCClient, Mockito.timeout(100).times(1)).run(coreCCRequestArgumentCaptor.capture());
        Mockito.verify(restTemplate, Mockito.times(2)).put(Mockito.anyString(), Mockito.eq(TaskDto.class));
        CoreCCRequest coreCCRequest = coreCCRequestArgumentCaptor.getValue();
        Assertions.assertThat(coreCCRequest)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", taskId.toString())
                .hasFieldOrPropertyWithValue("timestamp", taskTimestamp)
                .hasFieldOrPropertyWithValue("launchedAutomatically", false)
                .hasFieldOrPropertyWithValue("taskParameterList", null);
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
        UUID taskId = UUID.randomUUID();
        OffsetDateTime taskTimestamp = OffsetDateTime.parse("2024-06-18T09:30Z");
        String raorequestFilePath = "http://test-uri/RAOREQUEST";
        ProcessFileDto raoRequestProcessFile = new ProcessFileDto(raorequestFilePath, "RAOREQUEST", ProcessFileStatus.VALIDATED, "raorequest.xml", null, OffsetDateTime.now());
        String cgmFilePath = "http://test-uri/CGM";
        ProcessFileDto cgmProcessFile = new ProcessFileDto(cgmFilePath, "CGM", ProcessFileStatus.VALIDATED, "cgm.zip", "cgm-document-id", OffsetDateTime.now());
        String dccgmFilePath = "http://test-uri/DCCGM";
        ProcessFileDto dccgmProcessFile = new ProcessFileDto(dccgmFilePath, "DCCGM", ProcessFileStatus.VALIDATED, "dccgm.zip", "dccgm-document-id", OffsetDateTime.now());
        String glskFilePath = "http://test-uri/GLSK";
        ProcessFileDto glskProcessFile = new ProcessFileDto(glskFilePath, "GLSK", ProcessFileStatus.VALIDATED, "glsk.xml", "glsk-document-id", OffsetDateTime.now());
        String cbcoraFilePath = "http://test-uri/CBCORA";
        ProcessFileDto cbcoraProcessFile = new ProcessFileDto(cbcoraFilePath, "CBCORA", ProcessFileStatus.VALIDATED, "cbcora.xml", "cbcora-document-id", OffsetDateTime.now());
        String refprogFilePath = "http://test-uri/REFPROG";
        ProcessFileDto refprogProcessFile = new ProcessFileDto(refprogFilePath, "REFPROG", ProcessFileStatus.VALIDATED, "refprog.xml", "refprog-document-id", OffsetDateTime.now());
        String virtualhubFilePath = "http://test-uri/VIRTUALHUB";
        ProcessFileDto virtualhubProcessFile = new ProcessFileDto(virtualhubFilePath, "VIRTUALHUB", ProcessFileStatus.VALIDATED, "virtualhub.xml", "virtualhub-document-id", OffsetDateTime.now());
        TaskDto taskDto = new TaskDto(taskId, taskTimestamp, TaskStatus.READY,
                List.of(raoRequestProcessFile, cgmProcessFile, dccgmProcessFile, glskProcessFile, cbcoraProcessFile, refprogProcessFile, virtualhubProcessFile),
                List.of(raoRequestProcessFile, cgmProcessFile, dccgmProcessFile, glskProcessFile, cbcoraProcessFile, refprogProcessFile, virtualhubProcessFile),
                null, null, null, null);

        RequestMessage requestMessage = new RequestMessage();
        Mockito.when(fileImporter.importRaoRequest(Mockito.any())).thenReturn(requestMessage);
        Payload payload = new Payload();
        requestMessage.setPayload(payload);
        RequestItems requestItems = new RequestItems();
        payload.setRequestItems(requestItems);
        RequestItem requestItem = new RequestItem();
        requestItem.setTimeInterval("2024-06-18T09:00Z/2024-06-18T10:00Z");
        Files files = new Files();
        requestItem.setFiles(files);
        requestItems.getRequestItem().add(requestItem);
        File cgmFile = new File();
        File dccgmFile = new File();
        File glskFile = new File();
        File cbcoraFile = new File();
        File refprogFile = new File();
        File virtualhubFile = new File();
        cgmFile.setUrl("documentIdentification://cgm-document-id");
        dccgmFile.setUrl("documentIdentification://dccgm-document-id");
        glskFile.setUrl("documentIdentification://glsk-document-id");
        cbcoraFile.setUrl("documentIdentification://cbcora-document-id");
        refprogFile.setUrl("documentIdentification://refprog-document-id");
        virtualhubFile.setUrl("documentIdentification://virtualhub-document-id");
        files.getFile().add(cgmFile);
        files.getFile().add(dccgmFile);
        files.getFile().add(glskFile);
        files.getFile().add(cbcoraFile);
        files.getFile().add(refprogFile);
        files.getFile().add(virtualhubFile);

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(raorequestFilePath, 1)).thenReturn("raorequest-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cgmFilePath, 1)).thenReturn("cgm-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(dccgmFilePath, 1)).thenReturn("dccgm-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(glskFilePath, 1)).thenReturn("glsk-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cbcoraFilePath, 1)).thenReturn("cbcora-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(refprogFilePath, 1)).thenReturn("refprog-presigned-url");
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(virtualhubFilePath, 1)).thenReturn("virtualhub-presigned-url");

        service.handleTask(taskDto, false);

        ArgumentCaptor<CoreCCRequest> coreCCRequestArgumentCaptor = ArgumentCaptor.forClass(CoreCCRequest.class);
        Mockito.verify(coreCCClient, Mockito.timeout(100).times(1)).run(coreCCRequestArgumentCaptor.capture());
        Mockito.verify(restTemplate, Mockito.times(2)).put(Mockito.anyString(), Mockito.eq(TaskDto.class));
        CoreCCRequest coreCCRequest = coreCCRequestArgumentCaptor.getValue();
        Assertions.assertThat(coreCCRequest)
                .isNotNull()
                .hasFieldOrPropertyWithValue("id", taskId.toString())
                .hasFieldOrPropertyWithValue("timestamp", taskTimestamp)
                .hasFieldOrPropertyWithValue("launchedAutomatically", false)
                .hasFieldOrPropertyWithValue("taskParameterList", null);
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
}
