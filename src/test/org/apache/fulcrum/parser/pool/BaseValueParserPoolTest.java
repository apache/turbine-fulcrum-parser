package org.apache.fulcrum.parser.pool;

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
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.fulcrum.parser.BaseValueParser;
import org.apache.fulcrum.parser.ParserService;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Test the BaseValueParserFactory and BaseValueParserPool classes.
 *
 * @author <a href="mailto:painter@apache.org">Jeffery Painter</a>
 * @version $Id: BaseValueParserPoolTest.java 222043 2019-01-17 08:17:33Z painter $
 */
public class BaseValueParserPoolTest extends BaseUnit5Test 
{

	private BaseValueParser parser;
    private ParserService parserService;
    
    /** 
     * Use commons pool to manage value parsers 
     */
    private BaseValueParserPool valueParserPool;


    /**
     * Performs any initialization that must happen before each test is run.
     * @throws Exception if parser service not found
     */
    @BeforeEach
    public void setUp() throws Exception
    {
        try
        {
            parserService = (ParserService)this.lookup(ParserService.ROLE);
            
    		// Define the default configuration
    		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
    		config.setMaxIdle(1);
    	    config.setMaxTotal(1);

    	    // init the pool
    	    valueParserPool 
        		= new BaseValueParserPool(new BaseValueParserFactory(), config);

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
        // parser is explicitely returned from valueParserPool.
        //parserService.putParser(parser);
        this.release(parserService);
    }
    
    /**
     * @throws Exception generic exception
     */
    @Test
    public void testFactoryMethods() throws Exception 
    {
    
    	try
    	{
    		// borrow a new parser and assign it to the parser service
    		parser = valueParserPool.borrowObject();
    		parser.setParserService(parserService);
    		
    		// test adding parameters
    		parser.add("test1",  "val1");
    		assertEquals(parser.get("test1"), "val1");

    		// clear the parser for reset
    		parser.clear();
    		assertTrue(parser.isValid());
    		
    		valueParserPool.returnObject( parser );
    		
    	} catch ( Exception e )
    	{
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    	
    	
    }

}
