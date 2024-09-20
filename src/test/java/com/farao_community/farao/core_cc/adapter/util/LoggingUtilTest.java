/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.util;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class LoggingUtilTest {
    @Test
    void sanifyStringNull() {
        final String input = null;
        Assertions.assertThat(LoggingUtil.sanifyString(input)).isEqualTo("null");
    }

    @Test
    void sanifyStringWithReturnCarriage() {
        final String input = "First line\nSecond line\r\nThird line";
        Assertions.assertThat(LoggingUtil.sanifyString(input)).isEqualTo("First line_Second line__Third line");
    }

    @Test
    void sanifyStringWithoutReturnCarriage() {
        final String input = "First element, Second element, Third element";
        Assertions.assertThat(LoggingUtil.sanifyString(input)).isEqualTo(input);
    }
}
