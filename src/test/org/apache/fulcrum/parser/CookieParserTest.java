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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;



/**
 * Test the DefaultCookieParser class.  Since CookieParser
 * extends ValueParser, we only need to add test cases to insure
 * that the CookieParser can be loaded and test setting and un-setting cookies
 *
 * @author <a href="mailto:painter@apache.org">Jeffery Painter</a>
 * @version $Id: CookieParserTest.java 222043 2019-01-17 08:17:33Z painter $
 */
public class CookieParserTest extends BaseUnit5Test
{

	private DefaultCookieParser parser;
    private ParserService parserService;

    /**
     * Performs any initialization that must happen before each test is run.
     * @throws Exception if parser service not found
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        try
        {
            parserService = (ParserService) this.lookup(ParserService.ROLE);
            parser = parserService.getParser(DefaultCookieParser.class);

            // Populate parser with mock servlet data
            HttpServletRequest request = getMockRequest();
            HttpServletResponse response = mock(HttpServletResponse.class);
            parser.setData(request, response);
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
        parserService.putParser(parser);
        this.release(parserService);
    }
    
    /**
     * The API allows us to add a cookie to the response
     * but we cannot read it back unless the request is dispatched
     * 
     * Test set and unset of cookie
     */
    @Test
    public void testCookieOperations()
    {
    	try
    	{
	    	// set a cookie
	        parser.set("cookie1",  "test");
	        
	        // unset a cookie
	        parser.unset("cookie1");
	        
    	} catch ( Exception e ) {
            e.printStackTrace();
            fail(e.getMessage());    		
    	}
    }
    
}
