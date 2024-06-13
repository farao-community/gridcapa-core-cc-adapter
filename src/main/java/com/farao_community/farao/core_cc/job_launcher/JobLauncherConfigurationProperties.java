/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.job_launcher;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@ConfigurationProperties("job-launcher")
public record JobLauncherConfigurationProperties(UrlProperties url, ProcessProperties process, List<String> autoTriggerFiletypes) {

    public record UrlProperties(String taskManagerTimestampUrl, String taskManagerBusinessDateUrl) { }

    public record ProcessProperties(String timezone, int daysToAdd) { }
}
