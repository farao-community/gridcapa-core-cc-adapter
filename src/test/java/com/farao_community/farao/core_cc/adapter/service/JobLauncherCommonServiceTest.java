///*
// * Copyright (c) 2024, RTE (http://www.rte-france.com)
// * This Source Code Form is subject to the terms of the Mozilla Public
// * License, v. 2.0. If a copy of the MPL was not distributed with this
// * file, You can obtain one at http://mozilla.org/MPL/2.0/.
// */
//package com.farao_community.farao.core_cc.adapter.service;
//
//import com.farao_community.farao.core_cc.adapter.service.CoreCCAdapterService;
//import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
//import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
//import org.junit.jupiter.api.Test;
//import org.slf4j.Logger;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.boot.web.client.RestTemplateBuilder;
//import org.springframework.cloud.stream.function.StreamBridge;
//import org.springframework.web.client.RestTemplate;
//
//import java.time.OffsetDateTime;
//import java.util.ArrayList;
//import java.util.UUID;
//
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
//import static org.mockito.Mockito.verifyNoMoreInteractions;
//import static org.mockito.Mockito.when;
//
//@SpringBootTest
//class JobLauncherCommonServiceTest {
//
//    private static final String TEST_URL = "http://test-uri/";
//
//    @MockBean
//    private Logger eventsLogger;
//    @MockBean
//    private RestTemplateBuilder restTemplateBuilder;
//    @MockBean
//    private StreamBridge streamBridge;
//    @MockBean
//    private CoreCCAdapterService coreCCAdapterService;
//
//    @Autowired
//    private JobLauncherCommonService service;
//
//    @Test
//    void launchJobTest() {
//        String id = "1fdda469-53e9-4d63-a533-b935cffdd2f6";
//        String timestamp = "2022-04-27T10:10Z";
//        TaskDto taskDto = new TaskDto(UUID.fromString(id),
//                OffsetDateTime.parse(timestamp),
//                TaskStatus.READY,
//                new ArrayList<>(),
//                new ArrayList<>(),
//                new ArrayList<>(),
//                new ArrayList<>(),
//                new ArrayList<>(),
//                new ArrayList<>());
//        RestTemplate restTemplate = mock(RestTemplate.class);
//        when(restTemplateBuilder.build()).thenReturn(restTemplate);
//
//        service.launchJob(taskDto, false);
//
//        verify(restTemplate, times(1)).put(TEST_URL + timestamp + "/status?status=PENDING", TaskDto.class);
//        verify(restTemplate, times(1)).put(TEST_URL + timestamp + "/runHistory", TaskDto.class);
//        verify(coreCCAdapterService, times(1)).handleTask(taskDto, false);
//        verifyNoMoreInteractions(restTemplate);
//    }
//}
