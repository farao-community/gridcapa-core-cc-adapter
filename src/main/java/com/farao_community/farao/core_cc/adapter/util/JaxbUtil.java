/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_cc.adapter.util;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
public final class JaxbUtil {

    private JaxbUtil()    {
        throw new AssertionError("Utility class should not be constructed");
    }

    public static <T> T unmarshalContent(final Class<T> clazz, final InputStream inputStream) throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        final Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        final JAXBElement<T> jaxbElement = jaxbUnmarshaller.unmarshal(new StreamSource(inputStream), clazz);
        return jaxbElement.getValue();
    }
}
