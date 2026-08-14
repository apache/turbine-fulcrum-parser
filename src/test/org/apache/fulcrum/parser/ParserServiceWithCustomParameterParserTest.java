package org.apache.fulcrum.parser;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Requests for the base {@link DefaultParameterParser} class must be served
 * by the class configured via {@link ParserService#PARAMETER_PARSER_CLASS_KEY},
 * without callers having to name the subclass explicitly.
 */
public class ParserServiceWithCustomParameterParserTest extends BaseUnit5Test
{
    private ParserService parserService;

    @BeforeEach
    public void setUpBefore() throws Exception
    {
        try
        {
            setConfigurationFileName("src/test/TestComponentConfigWithCustomParameterParser.xml");
            setRoleFileName("src/test/TestRoleConfig.xml");

            parserService = (ParserService) this.lookup(ParserService.ROLE);
        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void getParser_baseClassRequestUsesConfiguredSubclass() throws Exception
    {
        DefaultParameterParser pp = (DefaultParameterParser) parserService.getParser(DefaultParameterParser.class);

        assertEquals(TaggingParameterParser.class, pp.getClass());

        pp.add("k", "v");
        assertTrue(pp.getString("k").contains(TaggingParameterParser.TAG));

        parserService.putParser(pp);
    }

    @Test
    public void getParser_explicitSubclassRequestUnaffected() throws Exception
    {
        DefaultParameterParser pp = (DefaultParameterParser) parserService.getParser(TaggingParameterParser.class);

        assertEquals(TaggingParameterParser.class, pp.getClass());

        parserService.putParser(pp);
    }
}
