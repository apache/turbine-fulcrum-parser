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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
/**
 * Basic test that ParameterParser instantiates.
 *
 * @author <a href="epugh@opensourceconnections.com">Eric Pugh</a>
 * @version $Id: ParameterParserTest.java 1837188 2018-07-31 22:23:27Z tv $
 */
public class ParserServiceTest extends BaseUnit4Test
{

    private ParserService parserService;
    private ParameterParser parameterParser = null;
    private Part test;

    @Before
    public void setUpBefore() throws Exception
    {
        try
        {
            parserService = (ParserService)this.lookup(ParserService.ROLE);
            parameterParser = parserService.getParser(DefaultParameterParser.class);
            
            test = getPart("upload-field");
        }
        catch (ComponentException e)
        {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private Part getPart( String name )
    {
        return new Part()
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
                return name;
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

    @Test
    public void testConfiguredAutomaticUpload() throws Exception {
        assertTrue(parserService.getAutomaticUpload());
    }
    @Test
    public void testConfiguredParameterEncoding() throws Exception {
        assertEquals("utf-8", parserService.getParameterEncoding());
    }
    
    @Test
    public void testUploadParts() throws Exception {
        HttpServletRequest request = getMockRequest();
        // TODO check
        when(request.getContentType()).thenReturn("multipart/form-data; boundary=boundary");
        when(request.getMethod()).thenReturn("post");
        ((DefaultParameterParser) parameterParser).add(test.getName(), test);
        Part secondPart = getPart("second-field");
        ((DefaultParameterParser) parameterParser).add(secondPart.getName(), secondPart);
        
        List<Part> parts = parserService.parseUpload( request );
        assertTrue( !parts.isEmpty() );
        assertTrue( parts.size() == 2 );
    }
    
    @Test
    public void testNoUploadParts() throws Exception {
        HttpServletRequest request = getMockRequest();
        parameterParser.add("other-field", "foo");
        
        List<Part> parts = parserService.parseUpload( request );
        assertTrue( parts.isEmpty() );
        //assertTrue( parts.size() == 2 );
    }
    
    // from Turbine org.apache.turbine.test.BaseTestCase, should be later in Fulcrum Testcontainer BaseUnit4Test
    protected HttpServletRequest getMockRequest() throws Exception
    {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Content-type")).thenReturn("html/text");
        when(request.getHeader("Accept-Language")).thenReturn("en-US");

        Vector<String> v = new Vector<String>();
        when(request.getParameterNames()).thenReturn(v.elements());
        
        when(request.getPart(anyString())).thenAnswer( new Answer<Object>()
         {
             @Override
             public Object answer(InvocationOnMock invocation) throws Throwable
             {
                 String name = (String) invocation.getArguments()[0];
                 return parameterParser.getPart(name);
             }
         } );
        when(request.getParts()).thenAnswer( new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                return ((DefaultParameterParser)parameterParser).getParts();
            }
        } );
        return request;
    }

}
