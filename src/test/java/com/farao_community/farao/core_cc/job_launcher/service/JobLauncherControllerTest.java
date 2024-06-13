package com.farao_community.farao.core_cc.job_launcher.service;

import com.farao_community.farao.core_cc.job_launcher.JobLauncherController;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@SpringBootTest
class JobLauncherControllerTest {

    @Autowired
    private JobLauncherController jobLauncherController;

    private RestTemplate restTemplate;

    @MockBean
    private RestTemplateBuilder restTemplateBuilder;

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        Mockito.when(restTemplateBuilder.build()).thenReturn(restTemplate);
    }

    @Test
    void testLaunchJobOk() {
        TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(taskDto.getId()).thenReturn(UUID.randomUUID());

        ResponseEntity getResponseEntity = Mockito.mock(ResponseEntity.class);
        Mockito.when(restTemplate.getForEntity("http://test-uri/2021-12-09T21:30", TaskDto.class)).thenReturn(getResponseEntity);
        Mockito.when(getResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(getResponseEntity.getBody()).thenReturn(taskDto);

        ResponseEntity putResponseEntity = Mockito.mock(ResponseEntity.class);
        Mockito.when(restTemplate.getForEntity("http://test-uri/2021-12-09T21:30/status?status=PENDING", TaskDto.class)).thenReturn(putResponseEntity);
        Mockito.when(putResponseEntity.getStatusCode()).thenReturn(HttpStatus.OK);
        Mockito.when(putResponseEntity.getBody()).thenReturn(taskDto);

        ResponseEntity<Void> response = jobLauncherController.launchJob("2021-12-09T21:30", List.of());
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    void testLaunchJobNotFound() {
        ResponseEntity responseEntity = Mockito.mock(ResponseEntity.class);
        Mockito.when(restTemplate.getForEntity("http://test-uri/2021-12-09T21:30", TaskDto.class)).thenReturn(responseEntity);
        Mockito.when(responseEntity.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND);
        Mockito.when(responseEntity.getBody()).thenReturn(null);
        TaskDto taskDto = Mockito.mock(TaskDto.class);
        Mockito.when(responseEntity.getBody()).thenReturn(taskDto);
        Mockito.when(taskDto.getId()).thenReturn(UUID.randomUUID());

        ResponseEntity<Void> response = jobLauncherController.launchJob("2021-12-09T21:30", List.of());
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
