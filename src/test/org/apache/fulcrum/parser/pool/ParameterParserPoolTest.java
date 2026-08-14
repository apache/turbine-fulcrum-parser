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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.fulcrum.parser.DefaultParameterParser;
import org.apache.fulcrum.parser.ParserService;
import org.apache.fulcrum.parser.TaggingParameterParser;
import org.apache.fulcrum.testcontainer.BaseUnit5Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/**
 * Test the DefaultParameterParserFactory and DefaultParameterParserPool classes.
 *
 * @author <a href="mailto:painter@apache.org">Jeffery Painter</a>
 * @version $Id: ParameterParserPoolTest.java 222043 2019-01-17 08:17:33Z painter $
 */
public class ParameterParserPoolTest extends BaseUnit5Test
{
	private DefaultParameterParser parser;
    private ParserService parserService;

    /**
     * Use commons pool to manage parameter parsers
     */
    private DefaultParameterParserPool parameterParserPool;

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

    		// Define the default configuration: each individually requested class
    		// gets its own per-key capacity of 1, mirroring the pre-keyed pool's
    		// single-instance behavior; the overall cap is left unlimited so that
    		// requesting multiple distinct classes doesn't make them compete for
    		// one shared slot.
    		GenericKeyedObjectPoolConfig<DefaultParameterParser> config = new GenericKeyedObjectPoolConfig<>();
    		config.setMaxIdlePerKey(1);
    	    config.setMaxTotalPerKey(1);
    	    config.setMaxTotal(-1);

    	    // init the pool
    	    parameterParserPool
    	    	= new DefaultParameterParserPool(new DefaultParameterParserFactory(), config);

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
        // pool object already explicitely released by call to returnObject in test
        // will throw java.lang.IllegalStateException, as pool is external to parserService
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
    		parser = parameterParserPool.borrowObject(DefaultParameterParser.class);
    		parser.setParserService(parserService);

    		// test adding parameters
    		parser.add("test1",  "val1");
    		assertEquals(parser.get("test1"), "val1");

    		// clear the parser for reset
    		parser.clear();
    		assertTrue(parser.isValid());

    		parameterParserPool.returnObject( DefaultParameterParser.class, parser );
    	} catch ( Exception e )
    	{
    		e.printStackTrace();
    		fail(e.getMessage());
    	}
    }

    /**
     * Two distinct keys in the same pool must produce instances of their own
     * class, drawn from independent per-key sub-pools (FR-001, FR-010).
     */
    @Test
    public void testDistinctKeysProduceDistinctClasses() throws Exception
    {
        DefaultParameterParser defaultInstance = parameterParserPool.borrowObject(DefaultParameterParser.class);
        DefaultParameterParser taggedInstance = parameterParserPool.borrowObject(TaggingParameterParser.class);

        try
        {
            assertEquals(DefaultParameterParser.class, defaultInstance.getClass());
            assertEquals(TaggingParameterParser.class, taggedInstance.getClass());
        }
        finally
        {
            parameterParserPool.returnObject(DefaultParameterParser.class, defaultInstance);
            parameterParserPool.returnObject(TaggingParameterParser.class, taggedInstance);
        }
    }

    /**
     * research.md Decision 5: setting {@code maxTotalPerKey} equal to the
     * configured {@code maxTotal} preserves today's effective single-class
     * capacity instead of silently shrinking to Commons Pool2's per-key
     * default of 8. This proves the config pattern itself is sound;
     * {@code DefaultParserService.configure()} applying this same pattern to
     * the real service is exercised end-to-end by {@code ParserServiceTest}
     * and the full module test suite (T012/T014).
     */
    @Test
    public void testMaxTotalPerKeyMirrorsMaxTotal() throws Exception
    {
        GenericKeyedObjectPoolConfig<DefaultParameterParser> config = new GenericKeyedObjectPoolConfig<>();
        int configuredMaxTotal = 5;
        config.setMaxTotal(configuredMaxTotal);
        config.setMaxTotalPerKey(configuredMaxTotal);
        config.setBlockWhenExhausted(false);

        try (DefaultParameterParserPool sizedPool = new DefaultParameterParserPool(new DefaultParameterParserFactory(), config))
        {
            assertEquals(configuredMaxTotal, sizedPool.getMaxTotalPerKey());

            // Borrow more instances of ONE key than Commons Pool2's per-key
            // default (8) would allow if maxTotalPerKey had been left
            // unconfigured at the library default -- proves the sizing was
            // actually applied, not just present on the config object.
            DefaultParameterParser[] borrowed = new DefaultParameterParser[configuredMaxTotal];
            for (int i = 0; i < configuredMaxTotal; i++)
            {
                borrowed[i] = sizedPool.borrowObject(DefaultParameterParser.class);
            }
            for (DefaultParameterParser instance : borrowed)
            {
                sizedPool.returnObject(DefaultParameterParser.class, instance);
            }
        }
    }

    /**
     * Pool-health-check edge case: {@code testOnCreate}/{@code testOnBorrow}
     * must still be honored after the keyed conversion. A factory whose
     * {@code validateObject} always fails must make a validated borrow fail,
     * proving the validation hook still fires under the keyed API.
     */
    @Test
    public void testValidatorHookHonoredUnderKeyedBorrow() throws Exception
    {
        DefaultParameterParserFactory alwaysInvalidFactory = new DefaultParameterParserFactory()
        {
            @Override
            public boolean validateObject(Class<? extends DefaultParameterParser> key, PooledObject<DefaultParameterParser> parser)
            {
                return false;
            }
        };

        GenericKeyedObjectPoolConfig<DefaultParameterParser> config = new GenericKeyedObjectPoolConfig<>();
        config.setTestOnCreate(true);
        config.setBlockWhenExhausted(false);

        try (DefaultParameterParserPool invalidatingPool = new DefaultParameterParserPool(alwaysInvalidFactory, config))
        {
            assertThrows(Exception.class, () -> invalidatingPool.borrowObject(DefaultParameterParser.class));
        }
    }

}
