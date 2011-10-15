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


import java.util.Iterator;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.fulcrum.testcontainer.BaseUnitTest;
/**
 * Basic test that ParameterParser instantiates.
 *
 * @author <a href="epugh@opensourceconnections.com">Eric Pugh</a>
 * @version $Id$
 */
public class ParameterParserTest extends BaseUnitTest
{
    private ParameterParser parameterParser = null;

    private ParserService parserService;

    /**
     * Defines the testcase name for JUnit.
     *
     * @param name the testcase's name.
     */
    public ParameterParserTest(String name)
    {
        super(name);
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        try
        {
            parserService = (ParserService)this.lookup(ParserService.ROLE);
            parameterParser = (ParameterParser) parserService.getParser(DefaultParameterParser.class);
        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testConfiguredUrlCaseFolding() throws Exception
    {
        assertTrue(parameterParser.getUrlFolding() == ParserService.URL_CASE_FOLDING_NONE);
    }
    
    public void testConfiguredAutomaticUpload() throws Exception {
        assertTrue(parserService.getAutomaticUpload());
    }
    
    public void testConfiguredParameterEncoding() throws Exception {
        assertEquals("utf-8", parserService.getParameterEncoding());
    }

    /**
     * Simple test to verify that URL Case Folding works properly
     *
     * @throws Exception
     */
    public void testRepositoryExists() throws Exception
    {
        assertEquals("TRIMMED_and_Not_Modified",parameterParser.convertAndTrim(" TRIMMED_and_Not_Modified ", ParserService.URL_CASE_FOLDING_NONE));
        assertEquals("trimmed_and_lower_case",parameterParser.convertAndTrim(" TRIMMED_and_Lower_Case ", ParserService.URL_CASE_FOLDING_LOWER));
        assertEquals("TRIMMED_AND_UPPER_CASE",parameterParser.convertAndTrim(" TRIMMED_and_Upper_Case ", ParserService.URL_CASE_FOLDING_UPPER));
    }

    /**
     * This Test method checks the DefaultParameterParser which carries two Sets inside it.
     * The suggested problem was that pp.keySet() returns both Keys, but pp.getStrings("key")
     * only checks for keys which are not FileItems.
     *
     * @throws Exception
     */
    public void testAddPathInfo() throws Exception
    {
        FileItemFactory factory = new DiskFileItemFactory(10240, null);

        assertEquals("keySet() is not empty!", 0, parameterParser.keySet().size());

        FileItem test = factory.createItem("upload-field", "application/octet-stream", false, null);
        // Push this into the parser using DefaultParameterParser's add() method.
        ((DefaultParameterParser) parameterParser).add("upload-field", test);

        assertEquals("FileItem not found in keySet()!", 1, parameterParser.keySet().size());

        Iterator it = parameterParser.keySet().iterator();
        assertTrue(it.hasNext());

        String name = (String) it.next();
        assertEquals("Wrong name found", "upload-field", name);

        assertFalse(it.hasNext());

        parameterParser.add("other-field", "foo");

        assertEquals("Wrong number of fields found ", 2, parameterParser.getKeys().length);

        assertTrue(parameterParser.containsKey("upload-field"));
        assertTrue(parameterParser.containsKey("other-field"));

        // The following will actually cause a ClassCastException because getStrings() (and others) are not catering for FileItems.
        assertNull("The returned should be null because a FileItem is not a String", parameterParser.getStrings("upload-field"));
        assertFalse(parameterParser.containsKey("missing-field"));
    }
}
