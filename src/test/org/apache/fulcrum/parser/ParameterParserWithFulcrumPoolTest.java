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

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.Part;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.parser.ValueParser.URLCaseFolding;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test that ParameterParser instantiates.
 *
 * @author <a href="epugh@opensourceconnections.com">Eric Pugh</a>
 * @version $Id: ParameterParserTest.java 1848895 2018-12-13 21:04:26Z painter $
 */
public class ParameterParserWithFulcrumPoolTest extends ParameterParserTest
{

    @Override
    @BeforeEach
    public void setUpBefore() throws Exception
    {
        try
        {
            setConfigurationFileName("src/test/TestComponentConfigWithFulcrumPool.xml");
            
            setRoleFileName( "src/test/TestRoleConfigWithFulcrumPool.xml");
            
            parserService = (ParserService)this.lookup(ParserService.ROLE);
            parameterParser = parserService.getParser(DefaultParameterParser.class);

        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

}
