/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.configuration.CoreCCAdapterConfiguration;
import com.farao_community.farao.gridcapa_core_cc.api.exception.CoreCCInvalidDataException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@Component
public class UrlValidationService {
    private final List<String> whitelist;

    public UrlValidationService(CoreCCAdapterConfiguration coreCCAdapterConfiguration) {
        this.whitelist = coreCCAdapterConfiguration.whitelist();
    }

    public InputStream openUrlStream(final String urlString) {
        if (whitelist.stream().noneMatch(urlString::startsWith)) {
            final String message = String.format("URL '%s' is not part of application's whitelisted urls: %s", urlString, String.join(", ", whitelist));
            throw new CoreCCInvalidDataException(message);
        }
        try {
            final URL url = new URI(urlString).toURL();
            return url.openStream();
        } catch (IOException | URISyntaxException | IllegalArgumentException e) {
            throw new CoreCCInvalidDataException(String.format("Cannot download file resource from URL '%s'", urlString), e);
        }
    }
}
