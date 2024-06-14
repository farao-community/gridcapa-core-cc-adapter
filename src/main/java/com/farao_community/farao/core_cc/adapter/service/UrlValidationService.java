/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.configuration.CoreCCAdapterConfiguration;
import com.farao_community.farao.gridcapa_core_cc.api.exception.CoreCCInvalidDataException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.StringJoiner;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@Component
public class UrlValidationService {
    private final List<String> whitelist;

    public UrlValidationService(CoreCCAdapterConfiguration coreCCAdapterConfiguration) {
        this.whitelist = coreCCAdapterConfiguration.whitelist();
    }

    public InputStream openUrlStream(String urlString) {
        if (whitelist.stream().noneMatch(urlString::startsWith)) {
            StringJoiner sj = new StringJoiner(", ", "Whitelist: ", ".");
            whitelist.forEach(sj::add);
            throw new CoreCCInvalidDataException(String.format("URL '%s' is not part of application's whitelisted url's %s", urlString, sj));
        }
        try {
            URL url = new URL(urlString);
            return url.openStream();
        } catch (IOException e) {
            throw new CoreCCInvalidDataException(String.format("Cannot download FileResource file from URL '%s'", urlString), e);
        }
    }
}
