/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.exception.RaoRequestImportException;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCFileResource;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.xml.bind.JAXBException;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class FileImporterTest {
    private UrlValidationService urlValidationService;
    private FileImporter fileImporter;

    @BeforeEach
    void init() {
        urlValidationService = Mockito.mock(UrlValidationService.class);
        fileImporter = new FileImporter(urlValidationService);
    }

    @Test
    void jaxbExceptionTest() {
        Mockito.when(urlValidationService.openUrlStream(Mockito.anyString()))
                .thenReturn(null);

        final CoreCCFileResource raoRequestFileResource = new CoreCCFileResource("filename", "invalid-url");
        Assertions.assertThatExceptionOfType(RaoRequestImportException.class)
                .isThrownBy(() -> fileImporter.importRaoRequest(raoRequestFileResource))
                .withCauseInstanceOf(JAXBException.class)
                .withMessage("Error occurred when converting InputStream to object of type RequestMessage");
    }
}
