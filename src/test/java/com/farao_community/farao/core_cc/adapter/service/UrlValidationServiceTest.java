/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.service;

import com.farao_community.farao.core_cc.adapter.configuration.CoreCCAdapterConfiguration;
import com.farao_community.farao.gridcapa_core_cc.api.exception.CoreCCInvalidDataException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class UrlValidationServiceTest {
    @Test
    void whitelistExceptionTest() {
        final CoreCCAdapterConfiguration configuration = Mockito.mock(CoreCCAdapterConfiguration.class);
        Mockito.when(configuration.whitelist()).thenReturn(List.of("http://test/", "https://test/"));
        final UrlValidationService service = new UrlValidationService(configuration);

        Assertions.assertThatExceptionOfType(CoreCCInvalidDataException.class)
                .isThrownBy(() -> service.openUrlStream("ftp://test/test.xml"))
                .withMessage("URL 'ftp://test/test.xml' is not part of application's whitelisted urls: http://test/, https://test/");
    }

    @Test
    void readExceptionTest() {
        final CoreCCAdapterConfiguration configuration = Mockito.mock(CoreCCAdapterConfiguration.class);
        Mockito.when(configuration.whitelist()).thenReturn(List.of("test/"));
        final UrlValidationService service = new UrlValidationService(configuration);

        Assertions.assertThatExceptionOfType(CoreCCInvalidDataException.class)
                .isThrownBy(() -> service.openUrlStream("test/test.xml"))
                .withMessage("Cannot download file resource from URL 'test/test.xml'");
    }
}
