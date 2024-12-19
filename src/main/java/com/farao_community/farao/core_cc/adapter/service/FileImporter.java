/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.exception.RaoRequestImportException;
import com.farao_community.farao.core_cc.adapter.util.JaxbUtil;
import com.farao_community.farao.gridcapa_core_cc.api.resource.CoreCCFileResource;
import com.farao_community.farao.gridcapa_core_cc.app.inputs.rao_request.RequestMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Service
public class FileImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileImporter.class);

    private final UrlValidationService urlValidationService;

    public FileImporter(UrlValidationService urlValidationService) {
        this.urlValidationService = urlValidationService;
    }

    public RequestMessage importRaoRequest(final CoreCCFileResource raoRequestFileResource) throws RaoRequestImportException {
        try (InputStream raoRequestInputStream = urlValidationService.openUrlStream(raoRequestFileResource.getUrl())) {
            return JaxbUtil.unmarshalContent(RequestMessage.class, raoRequestInputStream);
        } catch (IOException e) {
            final String errorMessage = String.format("Cannot download rao request file from URL '%s'", raoRequestFileResource.getUrl());
            LOGGER.error(errorMessage);
            throw new RaoRequestImportException(errorMessage, e);
        } catch (JAXBException e) {
            final String errorMessage = "Error occurred when converting InputStream to object of type RequestMessage";
            LOGGER.error(errorMessage);
            throw new RaoRequestImportException(errorMessage, e);
        }
    }
}
