/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
@ConfigurationProperties("core-cc-adapter")
public record CoreCCAdapterConfiguration(String taskManagerTimestampUrl,
                                         List<String> autoTriggerFiletypes,
                                         List<String> whitelist) {
}
