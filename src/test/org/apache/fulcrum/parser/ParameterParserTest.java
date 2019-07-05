package org.apache.fulcrum.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.parser.ValueParser.URLCaseFolding;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Basic test that ParameterParser instantiates.
 *
 * @author <a href="epugh@opensourceconnections.com">Eric Pugh</a>
 * @version $Id$
 */

public class ParameterParserTest extends BaseUnit5Test
{
    protected ParameterParser parameterParser = null;
    
    protected ParserService parserService = null;
    
    protected Part mockTest = new Part()
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
            if (name.equals( "content-disposition")) {
                //return "form-data; name=\"file\"; filename*=utf-8''%c2%a3%20uploadedFileName.ext";
                //return "attachment; filename=genome.jpeg;";
                return "form-data; name=\"file\"; filename=\"uploadedFileName.ext\"";
            }
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

    @BeforeEach
    public void setUpBefore() throws Exception
    {
        try
        {
            parserService = (ParserService)this.lookup(ParserService.ROLE);
            parameterParser = parserService.getParser(DefaultParameterParser.class);
            
        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Clean up after each test is run.
     */
    @AfterEach
    public void tearDown()
    {
        parserService.putParser(parameterParser);
        this.release(parserService);
    }
    
    @Test
    public void testRequest() throws Exception
    {
        assertTrue(parameterParser.getRequest() == null);
        HttpServletRequest req = getMockRequest();
        parameterParser.setRequest(req);
        assertTrue(parameterParser.getRequest().getServerPort() == 8080);
        parserService.putParser(parameterParser);
        assertTrue(parameterParser.getRequest() == null);
        
        parameterParser = parserService.getParser(DefaultParameterParser.class);
        assertTrue(parameterParser.getRequest() == null);
    }
    

    @Test
    public void testRequests() throws Exception
    {
        
        assertTrue(parameterParser.getRequest() == null);
        HttpServletRequest req = getMockRequest();
        
        parameterParser.setRequest(req);
        
        parameterParser.add("xy", 35);
        
        assertTrue(parameterParser.getRequest().getServerPort() == 8080);
        assertTrue(parameterParser.getString("xy").equals("35"));
        
        parserService.putParser(parameterParser);
        assertTrue(parameterParser.getRequest() == null);
        assertTrue(parameterParser.getString("xy") == null);

        
        ParameterParser parameterParser2 = parserService.getParser(DefaultParameterParser.class);
        assertTrue(parameterParser2.getRequest() == null);
        parameterParser2.add("ab", 7);
        
        // only one object in pool, which is refetched
        assertTrue(parameterParser.getString("ab").equals("7"));
        assertTrue(parameterParser.getString("xy") == null);
        
        assertTrue(parameterParser2.equals(parameterParser),parameterParser2 + " is not equal to "+ parameterParser);
    }
    
    /**
     * Simple test to verify the current configuration of URL Case Folding
     * 
     * @throws Exception generic exception
     */
    @Test
    public void testConfiguredUrlCaseFolding() throws Exception
    {
        assertTrue(parameterParser.getUrlFolding() == URLCaseFolding.NONE);
    }

    /**
     * Simple test to verify that variations on URL Case Folding work properly
     *
     * @throws Exception if the parameter parser is not found
     */
    @Test
    public void testAlternateCaseFoldings() throws Exception
    {

        assertEquals(parameterParser.convertAndTrim(" TRIMMED_and_Not_Modified ", URLCaseFolding.NONE),"TRIMMED_and_Not_Modified");
        assertEquals(parameterParser.convertAndTrim(" TRIMMED_and_Lower_Case ", URLCaseFolding.LOWER),"trimmed_and_lower_case");
        assertEquals(parameterParser.convertAndTrim(" TRIMMED_and_Upper_Case ", URLCaseFolding.UPPER),"TRIMMED_AND_UPPER_CASE");
    }

    /**
     * This Test method checks the DefaultParameterParser which carries two Sets inside it.
     * The suggested problem was that pp.keySet() returns both Keys, but pp.getStrings("key")
     * only checks for keys which are not Parts.
     *
     * @throws Exception generic exception
     */
    @Test
    public void testAddPathInfo() throws Exception
    {
        assertEquals(0, parameterParser.keySet().size(), "keySet() is not empty!");

        // Push this into the parser using DefaultParameterParser's add() method.
        ((DefaultParameterParser) parameterParser).add("upload-field", mockTest);

        assertEquals(1, parameterParser.keySet().size(), "Part not found in keySet()!");

        Iterator<String> it = parameterParser.keySet().iterator();
        assertTrue(it.hasNext());

        String name = it.next();
        assertEquals( "upload-field", name,"Wrong name found");

        assertFalse(it.hasNext());

        parameterParser.add("other-field", "foo");

        assertEquals( 2, parameterParser.getKeys().length, "Wrong number of fields found ");

        assertTrue(parameterParser.containsKey("upload-field"));
        assertTrue(parameterParser.containsKey("other-field"));

        // The following will actually cause a ClassCastException because getStrings() (and others) are not catering for Parts.
        assertNull(parameterParser.getStrings("upload-field"), "The returned should be null because a Part is not a String");
        assertFalse(parameterParser.containsKey("missing-field"));
        
        // The following will actually cause a ClassCastException because getPart() (and others) are not catering for Non-Parts, e.g String.
        assertNull(parameterParser.getPart( "other-field" ), "The returned should be null because a String is not a Part");
        Part uploadField = parameterParser.getPart( "upload-field" );
        assertEquals("upload-field", uploadField.getName());

    }
    
    /**
     * This Test method checks the DefaultParameterParser which filename convenience mapping from Part.
     *
     * @throws Exception generic exception
     */
    @Test
    public void testFilename4Path() throws Exception
    {
        assertEquals(0, parameterParser.keySet().size(), "keySet() is not empty!");
        
        // Push this into the parser using DefaultParameterParser's add() method.
        ((DefaultParameterParser) parameterParser).add("upload-field", mockTest);
        
        assertTrue(parameterParser.containsKey("upload-field"));
        
        Part uploadField = parameterParser.getPart( "upload-field" );
        assertEquals("upload-field", uploadField.getName());
        
        String fileName = parameterParser.getFileName( uploadField );
        assertEquals("uploadedFileName.ext",fileName);
        
    }

}
