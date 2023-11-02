/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.app;

import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * @author Godelaine de Montmorillon {@literal <godelaine.demontmorillon at rte-france.com>}
 * @author Philippe Edwards {@literal <philippe.edwards at rte-france.com>}
 */
@Component
public class CoreCCAdapterListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreCCAdapterListener.class);
    private  final  CoreCCAdapterService coreAdapter;

    public CoreCCAdapterListener(CoreCCAdapterService coreAdapter) {
        this.coreAdapter = coreAdapter;
    }

    @Bean
    public Consumer<Flux<TaskDto>> consumeTask() {
        return f -> f
                .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
                .subscribe(coreAdapter::handleManualTask);
    }

    @Bean
    public Consumer<Flux<TaskDto>> consumeAutoTask() {
        return f -> f
                .onErrorContinue((t, r) -> LOGGER.error(t.getMessage(), t))
                .subscribe(coreAdapter::handleAutoTask);
    }

}
