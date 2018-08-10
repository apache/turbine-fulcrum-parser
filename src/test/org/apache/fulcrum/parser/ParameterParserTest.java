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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.Part;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.parser.ValueParser.URLCaseFolding;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.Before;
import org.junit.Test;
/**
 * Basic test that ParameterParser instantiates.
 *
 * @author <a href="epugh@opensourceconnections.com">Eric Pugh</a>
 * @version $Id$
 */
public class ParameterParserTest extends BaseUnit4Test
{
    private ParameterParser parameterParser = null;
    
    private Part test;

    @Before
    public void setUpBefore() throws Exception
    {
        try
        {
            ParserService parserService = (ParserService)this.lookup(ParserService.ROLE);
            parameterParser = parserService.getParser(DefaultParameterParser.class);
            
            test = new Part()
            {

                @Override
                public void write(String fileName) throws IOException
                {
                }

                @Override
                public String getSubmittedFileName()
                {
                    return null;
                }

                @Override
                public long getSize()
                {
                    return 0;
                }

                @Override
                public String getName()
                {
                    return "upload-field";
                }

                @Override
                public InputStream getInputStream() throws IOException
                {
                    return null;
                }

                @Override
                public Collection<String> getHeaders(String name)
                {
                    return null;
                }

                @Override
                public Collection<String> getHeaderNames()
                {
                    return null;
                }

                @Override
                public String getHeader(String name)
                {
                    return null;
                }

                @Override
                public String getContentType()
                {
                    return "application/octet-stream";
                }

                @Override
                public void delete() throws IOException
                {
                }
            };
        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    @Test
    public void testConfiguredUrlCaseFolding() throws Exception
    {
        assertTrue(parameterParser.getUrlFolding() == URLCaseFolding.NONE);
    }

    /**
     * Simple test to verify that URL Case Folding works properly
     *
     * @throws Exception
     */
    @Test
    public void testRepositoryExists() throws Exception
    {
        assertEquals("TRIMMED_and_Not_Modified",parameterParser.convertAndTrim(" TRIMMED_and_Not_Modified ", URLCaseFolding.NONE));
        assertEquals("trimmed_and_lower_case",parameterParser.convertAndTrim(" TRIMMED_and_Lower_Case ", URLCaseFolding.LOWER));
        assertEquals("TRIMMED_AND_UPPER_CASE",parameterParser.convertAndTrim(" TRIMMED_and_Upper_Case ", URLCaseFolding.UPPER));
    }

    /**
     * This Test method checks the DefaultParameterParser which carries two Sets inside it.
     * The suggested problem was that pp.keySet() returns both Keys, but pp.getStrings("key")
     * only checks for keys which are not Parts.
     *
     * @throws Exception
     */
    @Test
    public void testAddPathInfo() throws Exception
    {
        assertEquals("keySet() is not empty!", 0, parameterParser.keySet().size());

        // Push this into the parser using DefaultParameterParser's add() method.
        ((DefaultParameterParser) parameterParser).add("upload-field", test);

        assertEquals("Part not found in keySet()!", 1, parameterParser.keySet().size());

        Iterator<String> it = parameterParser.keySet().iterator();
        assertTrue(it.hasNext());

        String name = it.next();
        assertEquals("Wrong name found", "upload-field", name);

        assertFalse(it.hasNext());

        parameterParser.add("other-field", "foo");

        assertEquals("Wrong number of fields found ", 2, parameterParser.getKeys().length);

        assertTrue(parameterParser.containsKey("upload-field"));
        assertTrue(parameterParser.containsKey("other-field"));

        // The following will actually cause a ClassCastException because getStrings() (and others) are not catering for Parts.
        assertNull("The returned should be null because a Part is not a String", parameterParser.getStrings("upload-field"));
        assertFalse(parameterParser.containsKey("missing-field"));
        
        // The following will actually cause a ClassCastException because getPart() (and others) are not catering for Non-Parts, e.g String.
        assertNull("The returned should be null because a String is not a Part", parameterParser.getPart( "other-field" ));
        Part uploadField = parameterParser.getPart( "upload-field" );
        assertTrue(uploadField.getName().equals( "upload-field" ));
    }

}
