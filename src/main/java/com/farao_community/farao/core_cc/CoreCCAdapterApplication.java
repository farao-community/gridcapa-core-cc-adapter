/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc;

import com.farao_community.farao.core_cc.adapter.configuration.CoreCCAdapterConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@SuppressWarnings("hideutilityclassconstructor")
@EnableWebMvc
@EnableConfigurationProperties({CoreCCAdapterConfiguration.class})
@SpringBootApplication
public class CoreCCAdapterApplication {
    public static void main(String[] args) {
        SpringApplication.run(CoreCCAdapterApplication.class, args);
    }
}
