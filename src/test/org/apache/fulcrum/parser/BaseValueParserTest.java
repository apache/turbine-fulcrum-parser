package org.apache.fulcrum.parser;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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


import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Locale;

import org.apache.avalon.framework.component.ComponentException;
import org.apache.fulcrum.testcontainer.BaseUnit4Test;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing of the BaseValueParser class
 *
 * @author <a href="mailto:quintonm@bellsouth.net">Quinton McCombs</a>
 * @version $Id: BaseValueParserTest.java 222043 2004-12-06 17:47:33Z painter $
 */
public class BaseValueParserTest extends BaseUnit4Test
{

	private BaseValueParser parser;

    private ParserService parserService;

    /**
     * Performs any initialization that must happen before each test is run.
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception
    {
        try
        {
            parserService = (ParserService)this.lookup(ParserService.ROLE);
            parser = parserService.getParser(BaseValueParser.class);
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
    @After
    public void tearDown()
    {
        parserService.putParser(parser);
        this.release(parserService);
    }
    @Test
    public void testDate()
    {
        parser.clear();
        parser.setLocale(Locale.US);

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", "03/21/2008");

        Calendar cal = Calendar.getInstance(Locale.US);
        cal.clear();
        cal.set(2008, 2, 21, 0, 0, 0);

        assertEquals("Wrong Date value (US)", cal.getTime(), parser.getDate("foo"));

        parser.clear();
        parser.setLocale(Locale.GERMANY);

        parser.add("foo", "21.03.2008");

        cal = Calendar.getInstance(Locale.GERMANY);
        cal.clear();
        cal.set(2008, 2, 21, 0, 0, 0);

        assertEquals("Wrong Date value (German)", cal.getTime(), parser.getDate("foo"));
    }
    @Test
    public void testGetByte()
    {
        // no param
        byte result = parser.getByte("invalid");
        assertEquals(result, 0);

        // default
        result = parser.getByte("default", (byte)3);
        assertEquals(result, 3);

        // param exists
        parser.add("exists", "1");
        result = parser.getByte("exists");
        assertEquals(result, 1);

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getByte("unparsable");
        assertEquals(result, 0);
    }
    @Test
    public void testGetByteObject()
    {
        // no param
        Byte result = parser.getByteObject("invalid");
        assertNull(result);

        // default
        result = parser.getByteObject("default", new Byte((byte)3));
        assertEquals(result, new Byte((byte)3));

        // param exists
        parser.add("exists", "1");
        result = parser.getByteObject("exists");
        assertEquals(result, new Byte((byte)1));

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getByteObject("unparsable");
        assertNull(result);
    }
    @Test
    public void testGetInt()
    {
        // no param
        int result = parser.getInt("invalid");
        assertEquals(result, 0);

        // default
        result = parser.getInt("default", 3);
        assertEquals(result, 3);

        // param exists
        parser.add("exists", "1");
        result = parser.getInt("exists");
        assertEquals(result, 1);

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getInt("unparsable");
        assertEquals(result, 0);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        int arrayResult[] = parser.getInts("array");
        int compare[] = {1,2,3};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i]);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        int arrayResult2[] = parser.getInts("array2");
        int compare2[] = {1,0,3};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i] );
        }
    }
    @Test
    public void testGetIntObject()
    {
        // no param
        Integer result = parser.getIntObject("invalid");
        assertNull(result);

        // default
        result = parser.getIntObject("default", new Integer(3));
        assertEquals(result, new Integer(3));

        // param exists
        parser.add("exists", "1");
        result = parser.getIntObject("exists");
        assertEquals(result, new Integer(1));

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getIntObject("unparsable");
        assertNull(result);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        Integer arrayResult[] = parser.getIntObjects("array");
        Integer compare[] = {new Integer(1), new Integer(2), new Integer(3)};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i]);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        Integer arrayResult2[] = parser.getIntObjects("array2");
        Integer compare2[] = {new Integer(1), null, new Integer(3)};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i] );
        }
    }
    @Test
    public void testGetFloat()
    {
        // no param
        float result = parser.getFloat("invalid");
        assertEquals(result, 0, 0);

        // default
        result = parser.getFloat("default", 3);
        assertEquals(result, 3, 0);

        // param exists
        parser.add("exists", "1");
        result = parser.getFloat("exists");
        assertEquals(result, 1, 0);

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getFloat("unparsable");
        assertEquals(result, 0, 0);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        float arrayResult[] = parser.getFloats("array");
        float compare[] = {1,2,3};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i], 0);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        float arrayResult2[] = parser.getFloats("array2");
        float compare2[] = {1,0,3};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i], 0);
        }
    }
    @Test
    public void testGetFloatObject()
    {
        // no param
        Float result = parser.getFloatObject("invalid");
        assertNull(result);

        // default
        result = parser.getFloatObject("default", new Float(3));
        assertEquals(result, new Float(3));

        // param exists
        parser.add("exists", "1");
        result = parser.getFloatObject("exists");
        assertEquals(result, new Float(1));

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getFloatObject("unparsable");
        assertNull(result);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        Float arrayResult[] = parser.getFloatObjects("array");
        Float compare[] = {new Float(1), new Float(2), new Float(3)};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i]);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        Float arrayResult2[] = parser.getFloatObjects("array2");
        Float compare2[] = {new Float(1), null, new Float(3)};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i] );
        }
    }
    @Test
    public void testGetDouble()
    {
        // no param
        double result = parser.getDouble("invalid");
        assertEquals(result, 0, 0);

        // default
        result = parser.getDouble("default", 3);
        assertEquals(result, 3, 0);

        // param exists
        parser.add("exists", "1");
        result = parser.getDouble("exists");
        assertEquals(result, 1, 0);

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getDouble("unparsable");
        assertEquals(result, 0, 0);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        double arrayResult[] = parser.getDoubles("array");
        double compare[] = {1,2,3};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i], 0);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        double arrayResult2[] = parser.getDoubles("array2");
        double compare2[] = {1,0,3};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i], 0);
        }
    }
    @Test
    public void testGetDoubleObject()
    {
        // no param
        Double result = parser.getDoubleObject("invalid");
        assertNull(result);

        // default
        result = parser.getDoubleObject("default", new Double(3));
        assertEquals(result, new Double(3));

        // param exists
        parser.add("exists", "1");
        result = parser.getDoubleObject("exists");
        assertEquals(result, new Double(1));

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getDoubleObject("unparsable");
        assertNull(result);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        Double arrayResult[] = parser.getDoubleObjects("array");
        Double compare[] = {new Double(1), new Double(2), new Double(3)};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i]);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        Double arrayResult2[] = parser.getDoubleObjects("array2");
        Double compare2[] = {new Double(1), null, new Double(3)};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i] );
        }
    }
    @Test
    public void testGetLong()
    {
        // no param
        long result = parser.getLong("invalid");
        assertEquals(result, 0);

        // default
        result = parser.getLong("default", 3);
        assertEquals(result, 3);

        // param exists
        parser.add("exists", "1");
        result = parser.getLong("exists");
        assertEquals(result, 1);

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getLong("unparsable");
        assertEquals(result, 0);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        long arrayResult[] = parser.getLongs("array");
        long compare[] = {1,2,3};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i]);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        long arrayResult2[] = parser.getLongs("array2");
        long compare2[] = {1,0,3};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i]);
        }
    }
    @Test
    public void testGetLongObject()
    {
        // no param
        Long result = parser.getLongObject("invalid");
        assertNull(result);

        // default
        result = parser.getLongObject("default", new Long(3));
        assertEquals(result, new Long(3));

        // param exists
        parser.add("exists", "1");
        result = parser.getLongObject("exists");
        assertEquals(result, new Long(1));

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getLongObject("unparsable");
        assertNull(result);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        Long arrayResult[] = parser.getLongObjects("array");
        Long compare[] = {new Long(1), new Long(2), new Long(3)};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i]);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        Long arrayResult2[] = parser.getLongObjects("array2");
        Long compare2[] = {new Long(1), null, new Long(3)};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i] );
        }
    }
    @Test
    public void testGetBoolean()
    {
        // no param
        boolean result = parser.getBoolean("invalid");
        assertEquals(result, false);

        // default
        result = parser.getBoolean("default", true);
        assertEquals(result, true);

        // true values - Case is intentional
        parser.add("true1", "trUe");
        result = parser.getBoolean("true1");
        assertEquals(result, true);
        parser.add("true2", "yEs");
        result = parser.getBoolean("true2");
        assertEquals(result, true);
        parser.add("true3", "1");
        result = parser.getBoolean("true3");
        assertEquals(result, true);
        parser.add("true4", "oN");
        result = parser.getBoolean("true4");
        assertEquals(result, true);

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getBoolean("unparsable");
        assertEquals(result, false);

    }
    @Test
    public void testGetBooleanObject()
    {
        // no param
        Boolean result = parser.getBooleanObject("invalid");
        assertNull(result);

        // default
        result = parser.getBooleanObject("default", Boolean.TRUE);
        assertEquals(result, Boolean.TRUE);

        // true values - Case is intentional
        parser.add("true1", "trUe");
        result = parser.getBooleanObject("true1");
        assertEquals(result, Boolean.TRUE);
        parser.add("true2", "yEs");
        result = parser.getBooleanObject("true2");
        assertEquals(result, Boolean.TRUE);
        parser.add("true3", "1");
        result = parser.getBooleanObject("true3");
        assertEquals(result, Boolean.TRUE);
        parser.add("true4", "oN");
        result = parser.getBooleanObject("true4");
        assertEquals(result, Boolean.TRUE);

        // false values - Case is intentional
        parser.add("false1", "falSe");
        result = parser.getBooleanObject("false1");
        assertEquals(result, Boolean.FALSE);
        parser.add("false2", "nO");
        result = parser.getBooleanObject("false2");
        assertEquals(result, Boolean.FALSE);
        parser.add("false3", "0");
        result = parser.getBooleanObject("false3");
        assertEquals(result, Boolean.FALSE);
        parser.add("false4", "oFf");
        result = parser.getBooleanObject("false4");
        assertEquals(result, Boolean.FALSE);


        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getBooleanObject("unparsable");
        assertNull(result);
    }
    @Test
    public void testGetBigDecimal()
    {
        // no param
        BigDecimal result = parser.getBigDecimal("invalid");
        assertNull(result); // object returns NOT new BigDecimal(0)

        // default
        result = parser.getBigDecimal("default", new BigDecimal(3));
        assertEquals(result, new BigDecimal(3));

        // param exists
        parser.add("exists", "1");
        result = parser.getBigDecimal("exists");
        assertEquals(result, new BigDecimal(1));

        // unparsable value
        parser.add("unparsable", "a");
        result = parser.getBigDecimal("unparsable");
        assertNull(result); //assertEquals(new BigDecimal(0), result);

        // array
        parser.add("array", "1");
        parser.add("array", "2");
        parser.add("array", "3");
        BigDecimal arrayResult[] = parser.getBigDecimals("array");
        BigDecimal compare[] = {new BigDecimal(1), new BigDecimal(2),
                                new BigDecimal(3)};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i]);
        }

        // array w/ unparsable element
        parser.add("array2", "1");
        parser.add("array2", "a");
        parser.add("array2", "3");
        BigDecimal arrayResult2[] = parser.getBigDecimals("array2");
        BigDecimal compare2[] = {new BigDecimal(1), null, new BigDecimal(3)};
        assertEquals(arrayResult2.length, compare2.length);
        for( int i=0; i<compare2.length; i++)
        {
            assertEquals(compare2[i], arrayResult2[i] );
        }
    }

    @Test
    public void testGetString()
    {
        // no param
        String result = parser.getString("invalid");
        assertNull(result);

        // default
        result = parser.getString("default", "default");
        assertEquals(result, "default");

        // null value
        parser.add("null", (String) null);
        assertNull( parser.getString("null"));

        // only return the first added
        parser.add("multiple", "test");
        parser.add("multiple", "test2");
        assertEquals("test", parser.getString("multiple"));

        // array
        parser.add("array", "line1");
        parser.add("array", "line2");
        parser.add("array", "line3");
        String arrayResult[] = parser.getStrings("array");
        String compare[] = {"line1","line2","line3"};
        assertEquals(arrayResult.length, compare.length);
        for( int i=0; i<compare.length; i++)
        {
            assertEquals(compare[i], arrayResult[i]);
        }

    }
    @Test
    public void testRecycling() throws Exception {
    		parser.setCharacterEncoding("fake");
    		parser.recycle();
    		assertEquals("US-ASCII",parser.getCharacterEncoding());
    }

    @Test
    public void testSetup()
    {
        try
        {
            BaseValueParser vp = parserService.getParser(BaseValueParser.class);
            assertFalse(vp.isDisposed());
            parserService.putParser(vp);
        }
        catch (InstantiationException e)
        {
            assertTrue("Could not instantiate ValueParser object", false);
        }

        // TODO expose PARAMETER_ENCODING_DEFAULT

//        assertEquals("Wrong Character Encoding", TurbineConstants.PARAMETER_ENCODING_DEFAULT, vp.getCharacterEncoding());
    }

    /**
     * TODO expose the PARAMETER_ENCODING_DEFAULT INSIDE THE VALUEPARSER
     *
     */
//    public void testChangeEncoding()
//    {
//        ValueParser vp = new BaseValueParser();
//
//        assertEquals("Wrong Character Encoding", TurbineConstants.PARAMETER_ENCODING_DEFAULT, vp.getCharacterEncoding());
//
//        String encoding = "ISO-8859-2";
//        vp.setCharacterEncoding(encoding);
//
//        assertEquals("Wrong Character Encoding", encoding, vp.getCharacterEncoding());
//    }
    @Test
    public void testClear()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", "bar");

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());
    }

    @Test
    public void testDispose()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", "bar");

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        parser.dispose();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertTrue(parser.isDisposed());
    }
    @Test
    public void testKeyArray()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("bar", "foo");

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        parser.add("bar", "baz");

        assertEquals("Wrong number of keys", 1, parser.keySet().size());
    }

    public void testDoubleAdd()
    {
        parser.clear();
        parser.setLocale(Locale.US);

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        double testValue = 2.2;

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", "2.2", parser.getString("foo"));
        assertEquals("Wrong double value", testValue, parser.getDouble("foo"), 0.001);
        assertEquals("Wrong Double value", testValue, parser.getDoubleObject("foo").doubleValue(), 0.001);

        double [] doubles = parser.getDoubles("foo");
        assertEquals("Wrong Array Size", 1, doubles.length);

        assertEquals("Wrong double array value", testValue, doubles[0], 0.001);

        Double [] doubleObjs = parser.getDoubleObjects("foo");
        assertEquals("Wrong Array Size", 1, doubleObjs.length);

        assertEquals("Wrong Double array value", testValue, doubleObjs[0].doubleValue(), 0.001);

        parser.clear();
        parser.setLocale(Locale.GERMANY);

        String testDouble = "2,3";
        parser.add("foo", testDouble);
        assertEquals("Wrong double value", 2.3, parser.getDouble("foo"), 0.001);

        parser.add("unparsable2", "1a");
        Double result = parser.getDoubleObject("unparsable2");
        assertNull("Double object should be null", result);
    }
    @Test
    public void testIntAdd()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        int testValue = 123;

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", "123", parser.getString("foo"));
        assertEquals("Wrong int value", testValue, parser.getInt("foo"));
        assertEquals("Wrong Int value", testValue, parser.getIntObject("foo").intValue());

        int [] ints = parser.getInts("foo");
        assertEquals("Wrong Array Size", 1, ints.length);

        assertEquals("Wrong int array value", testValue, ints[0]);

        Integer [] intObjs = parser.getIntObjects("foo");
        assertEquals("Wrong Array Size", 1, intObjs.length);

        assertEquals("Wrong Int array value", testValue, intObjs[0].intValue());
    }
    @Test
    public void testIntegerAdd()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        Integer testValue = new Integer(123);

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", "123", parser.getString("foo"));
        assertEquals("Wrong int value", testValue.intValue(), parser.getInt("foo"));
        assertEquals("Wrong Int value", testValue.intValue(), parser.getIntObject("foo").intValue());

        int [] ints = parser.getInts("foo");
        assertEquals("Wrong Array Size", 1, ints.length);

        assertEquals("Wrong int array value", testValue.intValue(), ints[0]);

        Integer [] intObjs = parser.getIntObjects("foo");
        assertEquals("Wrong Array Size", 1, intObjs.length);

        assertEquals("Wrong Int array value", testValue.intValue(), intObjs[0].intValue());
    }
    @Test
    public void testLongAdd()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        long testValue = 9223372036854775807l;

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", "9223372036854775807", parser.getString("foo"));
        assertEquals("Wrong long value", testValue, parser.getLong("foo"));
        assertEquals("Wrong Long value", testValue, parser.getLongObject("foo").longValue());

        long [] longs = parser.getLongs("foo");
        assertEquals("Wrong Array Size", 1, longs.length);

        assertEquals("Wrong long array value", testValue, longs[0]);

        Long [] longObjs = parser.getLongObjects("foo");
        assertEquals("Wrong Array Size", 1, longObjs.length);

        assertEquals("Wrong Long array value", testValue, longObjs[0].longValue());
    }
    @Test
    public void testLongToInt()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        long testValue = 1234l;

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", "1234", parser.getString("foo"));
        assertEquals("Wrong int value", (int) testValue, parser.getInt("foo"));
        assertEquals("Wrong Int value", (int) testValue, parser.getIntObject("foo").intValue());

        int [] ints = parser.getInts("foo");
        assertEquals("Wrong Array Size", 1, ints.length);

        assertEquals("Wrong int array value", testValue, ints[0]);

        Integer [] intObjs = parser.getIntObjects("foo");
        assertEquals("Wrong Array Size", 1, intObjs.length);

        assertEquals("Wrong Int array value", testValue, intObjs[0].intValue());
    }

    public void testIntToLong()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        int testValue = 123;

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", "123", parser.getString("foo"));
        assertEquals("Wrong long value", testValue, parser.getLong("foo"));
        assertEquals("Wrong Long value", testValue, parser.getLongObject("foo").longValue());

        long [] longs = parser.getLongs("foo");
        assertEquals("Wrong Array Size", 1, longs.length);

        assertEquals("Wrong long array value", testValue, longs[0]);

        Long [] longObjs = parser.getLongObjects("foo");
        assertEquals("Wrong Array Size", 1, longObjs.length);

        assertEquals("Wrong Long array value", testValue, longObjs[0].longValue());
    }
    @Test
    public void testIntToDouble()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        int testValue = 123;

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", "123", parser.getString("foo"));
        assertEquals("Wrong double value", testValue, parser.getDouble("foo"), 0.001);
        assertEquals("Wrong Double value", testValue, parser.getDoubleObject("foo").doubleValue(), 0.001);

        double [] doubles = parser.getDoubles("foo");
        assertEquals("Wrong Array Size", 1, doubles.length);

        assertEquals("Wrong double array value", testValue, doubles[0], 0.001);

        Double [] doubleObjs = parser.getDoubleObjects("foo");
        assertEquals("Wrong Array Size", 1, doubleObjs.length);

        assertEquals("Wrong Double array value", testValue, doubleObjs[0].doubleValue(), 0.001);
    }
    @Test
    public void testLongToDouble()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        long testValue = 9223372036854775807l;

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", "9223372036854775807", parser.getString("foo"));
        assertEquals("Wrong double value", testValue, parser.getDouble("foo"), 0.001);
        assertEquals("Wrong Double value", testValue, parser.getDoubleObject("foo").doubleValue(), 0.001);

        double [] doubles = parser.getDoubles("foo");
        assertEquals("Wrong Array Size", 1, doubles.length);

        assertEquals("Wrong double array value", testValue, doubles[0], 0.001);

        Double [] doubleObjs = parser.getDoubleObjects("foo");
        assertEquals("Wrong Array Size", 1, doubleObjs.length);

        assertEquals("Wrong Double array value", testValue, doubleObjs[0].doubleValue(), 0.001);
    }

    public void testStringAdd()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        String testValue = "the quick brown fox";

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", testValue, parser.getString("foo"));

        String [] Strings = parser.getStrings("foo");
        assertEquals("Wrong Array Size", 1, Strings.length);

        assertEquals("Wrong String array value", testValue, Strings[0]);
    }

    public void testStringToInt()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        String testValue = "123456";

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", testValue, parser.getString("foo"));

        assertEquals("Wrong int value", Integer.parseInt(testValue), parser.getInt("foo"));
        assertEquals("Wrong Int value", Integer.valueOf(testValue).intValue(), parser.getIntObject("foo").intValue());

        int [] ints = parser.getInts("foo");
        assertEquals("Wrong Array Size", 1, ints.length);

        assertEquals("Wrong int array value", Integer.parseInt(testValue), ints[0]);

        Integer [] intObjs = parser.getIntObjects("foo");
        assertEquals("Wrong Array Size", 1, intObjs.length);

        assertEquals("Wrong Int array value", Integer.valueOf(testValue).intValue(), intObjs[0].intValue());
    }

    public void testStringToLong()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        String testValue = "123456";

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", testValue, parser.getString("foo"));

        assertEquals("Wrong long value", Long.parseLong(testValue), parser.getLong("foo"));
        assertEquals("Wrong Long value", Long.valueOf(testValue).longValue(), parser.getLongObject("foo").longValue());

        long [] longs = parser.getLongs("foo");
        assertEquals("Wrong Array Size", 1, longs.length);

        assertEquals("Wrong long array value", Long.parseLong(testValue), longs[0]);

        Long [] longObjs = parser.getLongObjects("foo");
        assertEquals("Wrong Array Size", 1, longObjs.length);

        assertEquals("Wrong Long array value", Long.valueOf(testValue).longValue(), longObjs[0].longValue());
    }
    @Test
    public void testStringArray()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        String [] testValue = new String [] {
            "foo", "bar", "baz"
        };

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        String [] res = parser.getStrings("foo");

        assertEquals("Wrong number of elements", 3, res.length);

        for (int i = 0; i < res.length; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i]);
        }

        assertEquals("Wrong element returned", testValue[0], parser.getString("foo"));

        parser.add("foo", "xxx");

        res = parser.getStrings("foo");

        assertEquals("Wrong number of elements", 4, res.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i]);
        }

        assertEquals(res[3], "xxx");

        // should append at the end.
        assertEquals("Wrong element returned", testValue[0], parser.getString("foo"));
    }
    @Test
    public void testRemove()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        String testValue = "the quick brown fox";

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", testValue, parser.getString("foo"));

        assertNotNull(parser.remove("foo"));

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertNull(parser.getString("foo"));

        // Test non-existing key
        assertNull(parser.remove("baz"));

        // Test removing null value
        assertNull(parser.remove(null));
    }
    @Test
    public void testRemoveArray()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        String testValue = "the quick brown fox";

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        assertEquals("Wrong string value", testValue, parser.getString("foo"));

        String [] res = parser.getStrings("foo");

        assertEquals("Wrong number of elements", 2, res.length);

        for (int i = 0; i < res.length; i++)
        {
            assertEquals("Wrong value", res[i], testValue);
        }

        parser.remove("foo");

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertNull(parser.getString("foo"));
    }
    @Test
    public void testContainsKey()
    {
        parser.clear();

        parser.add("foo", "bar");
        parser.add("bar", new String [] { "foo", "bar" });

        assertTrue(parser.containsKey("foo"));
        assertTrue(parser.containsKey("bar"));
        assertFalse(parser.containsKey("baz"));
    }
    @Test
    public void testBooleanObject()
    {
        parser.clear();

        parser.add("t1", "true");
        parser.add("t2", "yes");
        parser.add("t3", "on");
        parser.add("t4", "1");
        parser.add("t5", 1);

        parser.add("f1", "false");
        parser.add("f2", "no");
        parser.add("f3", "off");
        parser.add("f4", "0");
        parser.add("f5", 0);

        parser.add("e1", "nix");
        parser.add("e2", "weg");
        parser.add("e3", 200);
        parser.add("e4", -2.5);

        assertEquals("Value is not true", Boolean.TRUE, parser.getBooleanObject("t1"));
        assertEquals("Value is not true", Boolean.TRUE, parser.getBooleanObject("t2"));
        assertEquals("Value is not true", Boolean.TRUE, parser.getBooleanObject("t3"));
        assertEquals("Value is not true", Boolean.TRUE, parser.getBooleanObject("t4"));
        assertEquals("Value is not true", Boolean.TRUE, parser.getBooleanObject("t5"));

        assertEquals("Value is not false", Boolean.FALSE, parser.getBooleanObject("f1"));
        assertEquals("Value is not false", Boolean.FALSE, parser.getBooleanObject("f2"));
        assertEquals("Value is not false", Boolean.FALSE, parser.getBooleanObject("f3"));
        assertEquals("Value is not false", Boolean.FALSE, parser.getBooleanObject("f4"));
        assertEquals("Value is not false", Boolean.FALSE, parser.getBooleanObject("f5"));

        assertNull(parser.getBooleanObject("e1"));
        assertNull(parser.getBooleanObject("e2"));
        assertNull(parser.getBooleanObject("e3"));
        assertNull(parser.getBooleanObject("e4"));

        assertNull(parser.getBooleanObject("does-not-exist"));
    }
    @Test
    public void testBoolDefault()
    {
        parser.clear();

        parser.add("t1", "true");
        parser.add("f1", "false");

        assertTrue(parser.getBoolean("t1"));
        assertFalse(parser.getBoolean("f1"));

        assertFalse(parser.getBoolean("does not exist"));

        assertTrue(parser.getBoolean("t1", false));
        assertFalse(parser.getBoolean("f1", true));

        assertFalse(parser.getBoolean("does not exist", false));
        assertTrue(parser.getBoolean("does not exist", true));
    }
    @Test
    public void testBooleanDefault()
    {
        parser.clear();

        parser.add("t1", "true");
        parser.add("f1", "false");

        assertEquals("Value is not true",  Boolean.TRUE, parser.getBooleanObject("t1"));
        assertEquals("Value is not false", Boolean.FALSE, parser.getBooleanObject("f1"));

        assertNull(parser.getBooleanObject("does not exist"));

        assertEquals("Value is not true",  Boolean.TRUE, parser.getBooleanObject("t1", Boolean.FALSE));
        assertEquals("Value is not true",  Boolean.TRUE, parser.getBooleanObject("t1", null));
        assertEquals("Value is not false", Boolean.FALSE, parser.getBooleanObject("f1", Boolean.TRUE));
        assertEquals("Value is not false", Boolean.FALSE, parser.getBooleanObject("f1", null));

        assertNull(parser.getBooleanObject("does not exist", null));
    }
    @Test
    public void testDoubleArray()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        double [] testValue = {
            1.0, 2.0, 3.0
        };

        for (int i = 0; i < testValue.length; i++)
        {
            parser.add("foo", testValue[i]);

            String [] res = parser.getStrings("foo");
            assertEquals("Wrong number of elements", res.length, i + 1);
        }

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        double [] res = parser.getDoubles("foo");

        assertEquals("Wrong number of elements", 3, res.length);

        for (int i = 0; i < res.length; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i], 0.001);
        }

        Double [] resObj = parser.getDoubleObjects("foo");

        assertEquals("Wrong number of elements", 3, resObj.length);

        for (int i = 0; i < resObj.length; i++)
        {
            assertEquals("Wrong value", resObj[i].doubleValue(), testValue[i], 0.001);
        }

        assertEquals("Wrong element returned", testValue[0], parser.getDoubleObject("foo").doubleValue(), 0.001);

        parser.add("foo", 4.0);

        res = parser.getDoubles("foo");

        assertEquals("Wrong number of elements", 4, res.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i], 0.001);
        }

        assertEquals(res[3], 4.0, 0.001);

        resObj = parser.getDoubleObjects("foo");

        assertEquals("Wrong number of elements", 4, resObj.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", resObj[i].doubleValue(), testValue[i], 0.001);
        }

        assertEquals(resObj[3].doubleValue(), 4.0, 0.001);

        // should append at the end.
        assertEquals("Wrong element returned", testValue[0], parser.getDouble("foo"), 0.001);
    }
    @Test
    public void testFloatArray()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        float [] testValue = {
            1.0f, 2.0f, 3.0f
        };

        for (int i = 0; i < testValue.length; i++)
        {
            parser.add("foo", testValue[i]);

            String [] res = parser.getStrings("foo");
            assertEquals("Wrong number of elements", res.length, i + 1);
        }

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        float [] res = parser.getFloats("foo");

        assertEquals("Wrong number of elements", 3, res.length);

        for (int i = 0; i < res.length; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i], 0.001f);
        }

        Float [] resObj = parser.getFloatObjects("foo");

        assertEquals("Wrong number of elements", 3, resObj.length);

        for (int i = 0; i < resObj.length; i++)
        {
            assertEquals("Wrong value", resObj[i].floatValue(), testValue[i], 0.001f);
        }

        assertEquals("Wrong element returned", testValue[0], parser.getFloatObject("foo").floatValue(), 0.001f);

        parser.add("foo", 4.0f);

        res = parser.getFloats("foo");

        assertEquals("Wrong number of elements", 4, res.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i], 0.001f);
        }

        assertEquals(res[3], 4.0f, 0.001f);

        resObj = parser.getFloatObjects("foo");

        assertEquals("Wrong number of elements", 4, resObj.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", resObj[i].floatValue(), testValue[i], 0.001f);
        }

        assertEquals(resObj[3].floatValue(), 4.0f, 0.001f);

        // should append at the end.
        assertEquals("Wrong element returned", testValue[0], parser.getFloat("foo"), 0.001f);
    }
    @Test
    public void testBigDecimalArray()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        long [] testValue = {
            12345678,87654321,1092837465,
        };

        for (int i = 0; i < testValue.length; i++)
        {
            parser.add("foo", testValue[i]);

            String [] res = parser.getStrings("foo");
            assertEquals("Wrong number of elements", res.length, i + 1);
        }

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        BigDecimal [] res = parser.getBigDecimals("foo");

        assertEquals("Wrong number of elements", 3, res.length);

        for (int i = 0; i < res.length; i++)
        {
            assertEquals("Wrong value", res[i].longValue(), testValue[i]);
        }

        assertEquals("Wrong element returned", testValue[0], parser.getBigDecimal("foo").longValue());

        parser.add("foo", 77777777);

        res = parser.getBigDecimals("foo");

        assertEquals("Wrong number of elements", 4, res.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", res[i].longValue(), testValue[i], 0.001);
        }

        assertEquals(res[3].longValue(), 77777777);

        // should append at the end.
        assertEquals("Wrong element returned", testValue[0], parser.getBigDecimal("foo").longValue());
    }
    @Test
    public void testIntegerArray()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        int [] testValue = {
            1, 2, 3
        };

        for (int i = 0; i < testValue.length; i++)
        {
            parser.add("foo", testValue[i]);

            String [] res = parser.getStrings("foo");
            assertEquals("Wrong number of elements", res.length, i + 1);
        }

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        int [] res = parser.getInts("foo");

        assertEquals("Wrong number of elements", 3, res.length);

        for (int i = 0; i < res.length; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i]);
        }

        Integer [] resObj = parser.getIntObjects("foo");

        assertEquals("Wrong number of elements", 3, resObj.length);

        for (int i = 0; i < resObj.length; i++)
        {
            assertEquals("Wrong value", resObj[i].intValue(), testValue[i]);
        }

        assertEquals("Wrong element returned", testValue[0], parser.getIntObject("foo").intValue());

        parser.add("foo", 4);

        res = parser.getInts("foo");

        assertEquals("Wrong number of elements", 4, res.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i]);
        }

        assertEquals(res[3], 4);

        resObj = parser.getIntObjects("foo");

        assertEquals("Wrong number of elements", 4, resObj.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", resObj[i].intValue(), testValue[i]);
        }

        assertEquals(resObj[3].intValue(), 4);

        // should append at the end.
        assertEquals("Wrong element returned", testValue[0], parser.getInt("foo"));
    }
    @Test
    public void testLongArray()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        long [] testValue = {
            1l, 2l, 3l
        };

        for (int i = 0; i < testValue.length; i++)
        {
            parser.add("foo", testValue[i]);

            String [] res = parser.getStrings("foo");
            assertEquals("Wrong number of elements", res.length, i + 1);
        }

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        long [] res = parser.getLongs("foo");

        assertEquals("Wrong number of elements", 3, res.length);

        for (int i = 0; i < res.length; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i]);
        }

        Long [] resObj = parser.getLongObjects("foo");

        assertEquals("Wrong number of elements", 3, resObj.length);

        for (int i = 0; i < resObj.length; i++)
        {
            assertEquals("Wrong value", resObj[i].longValue(), testValue[i]);
        }

        assertEquals("Wrong element returned", testValue[0], parser.getLongObject("foo").longValue());

        parser.add("foo", 4);

        res = parser.getLongs("foo");

        assertEquals("Wrong number of elements", 4, res.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", res[i], testValue[i]);
        }

        assertEquals(res[3], 4);

        resObj = parser.getLongObjects("foo");

        assertEquals("Wrong number of elements", 4, resObj.length);

        for (int i = 0; i < 3; i++)
        {
            assertEquals("Wrong value", resObj[i].longValue(), testValue[i]);
        }

        assertEquals(resObj[3].longValue(), 4);

        // should append at the end.
        assertEquals("Wrong element returned", testValue[0], parser.getLong("foo"));
    }
    @Test
    public void testByteArray()
            throws Exception
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        String  testValue = "abcdefg";

        parser.add("foo", testValue);

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        byte [] res = parser.getBytes("foo");

        assertEquals("Wrong number of elements", 7, res.length);

        for (int i = 0; i < res.length; i++)
        {
            byte [] testByte = testValue.substring(i, i + 1).getBytes(parser.getCharacterEncoding());
            assertEquals("More than one byte for a char!", 1, testByte.length);
            assertEquals("Wrong value", res[i], testByte[0]);
        }
    }
    @Test
    public void testByte()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        String [] testValue = {
            "0", "127", "-1",
            "0", "-127", "100"
        };


        for (int i = 0; i < testValue.length; i++)
        {
            parser.add("foo" + i, testValue[i]);
        }

        assertEquals("Wrong number of keys", 6, parser.keySet().size());

        assertEquals("Wrong value", (byte) 0,    parser.getByte("foo0"));
        assertEquals("Wrong value", (byte) 127,  parser.getByte("foo1"));
        assertEquals("Wrong value", (byte) -1,   parser.getByte("foo2"));
        assertEquals("Wrong value", (byte) 0,    parser.getByte("foo3"));
        assertEquals("Wrong value", (byte) -127, parser.getByte("foo4"));
        assertEquals("Wrong value", (byte) 100,  parser.getByte("foo5"));

        assertEquals("Wrong value", new Byte((byte) 0),    parser.getByteObject("foo0"));
        assertEquals("Wrong value", new Byte((byte) 127),  parser.getByteObject("foo1"));
        assertEquals("Wrong value", new Byte((byte) -1),   parser.getByteObject("foo2"));
        assertEquals("Wrong value", new Byte((byte) 0),    parser.getByteObject("foo3"));
        assertEquals("Wrong value", new Byte((byte) -127), parser.getByteObject("foo4"));
        assertEquals("Wrong value", new Byte((byte) 100),  parser.getByteObject("foo5"));

    }
    @Test
    public void testStringDefault()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", "bar");

        assertEquals("Wrong value found", "bar", parser.getString("foo", "xxx"));
        assertEquals("Wrong value found", "bar", parser.getString("foo", null));

        assertEquals("Wrong value found", "baz", parser.getString("does-not-exist", "baz"));
        assertNull(parser.getString("does-not-exist", null));
    }
    @Test
    public void testSetString()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", "bar");

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        parser.add("bar", "foo");

        assertEquals("Wrong number of keys", 2, parser.keySet().size());

        parser.add("bar", "baz");

        assertEquals("Wrong number of keys", 2, parser.keySet().size());

        String [] res = parser.getStrings("bar");
        assertEquals("Wrong number of values", 2, res.length);
        assertEquals("Wrong value found", "foo", res[0]);
        assertEquals("Wrong value found", "baz", res[1]);

        parser.setString("bar", "xxx");

        assertEquals("Wrong number of keys", 2, parser.keySet().size());

        res = parser.getStrings("bar");
        assertEquals("Wrong number of values", 1, res.length);
        assertEquals("Wrong value found", "xxx", res[0]);
    }
    @Test
    public void testSetStrings()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", "bar");

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

        parser.add("bar", "foo");

        assertEquals("Wrong number of keys", 2, parser.keySet().size());

        parser.add("bar", "baz");

        assertEquals("Wrong number of keys", 2, parser.keySet().size());

        String [] res = parser.getStrings("bar");
        assertEquals("Wrong number of values", 2, res.length);
        assertEquals("Wrong value found", "foo", res[0]);
        assertEquals("Wrong value found", "baz", res[1]);

        String [] newValues = new String [] { "aaa", "bbb", "ccc", "ddd" };

        parser.setStrings("bar", newValues);

        assertEquals("Wrong number of keys", 2, parser.keySet().size());

        res = parser.getStrings("bar");
        assertEquals("Wrong number of values", newValues.length, res.length);

        for (int i = 0 ; i < newValues.length; i++)
        {
            assertEquals("Wrong value found", newValues[i], res[i]);
        }
    }
    @Test
    public void testSetProperties()
            throws Exception
    {
        parser.clear();

        parser.add("longValue", 12345l);
        parser.add("doubleValue", 2.0);
        parser.add("intValue", 200);
        parser.add("stringValue", "foobar");
        parser.add("booleanValue", "true");

        PropertyBean bp = new PropertyBean();
        bp.setDoNotTouchValue("abcdef");

        parser.setProperties(bp);

        assertEquals("Wrong value in bean", "abcdef", bp.getDoNotTouchValue());
        assertEquals("Wrong value in bean", "foobar", bp.getStringValue());
        assertEquals("Wrong value in bean", 200,      bp.getIntValue());
        assertEquals("Wrong value in bean", 2.0,      bp.getDoubleValue(), 0.001);
        assertEquals("Wrong value in bean", 12345l,   bp.getLongValue());
        assertEquals("Wrong value in bean", Boolean.TRUE, bp.getBooleanValue());
    }

    public void testAddNulls()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", (Integer) null);

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", (String) null);

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("bar", "null");

        assertEquals("Wrong number of keys", 1, parser.keySet().size());

    }
    @Test
    public void testAddNullArrays()
    {
        String [] res = null;

        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        parser.add("foo", new String [] { "foo", "bar" });
        res = parser.getStrings("foo");
        assertEquals("Wrong number of keys", 1, parser.keySet().size());
        assertEquals("Wrong number of values", 2, res.length);

        // null value should not change contents
        parser.add("foo", (String) null);
        res = parser.getStrings("foo");
        assertEquals("Wrong number of keys", 1, parser.keySet().size());
        assertEquals("Wrong number of values", 2, res.length);

        // null value should not change contents
        parser.add("foo", (String []) null);
        res = parser.getStrings("foo");
        assertEquals("Wrong number of keys", 1, parser.keySet().size());
        assertEquals("Wrong number of values", 2, res.length);

        // empty String array should not change contents
        parser.add("foo", new String [0]);
        res = parser.getStrings("foo");
        assertEquals("Wrong number of keys", 1, parser.keySet().size());
        assertEquals("Wrong number of values", 2, res.length);

        // String array with null value should not change contents
        parser.add("foo", new String [] { null });
        res = parser.getStrings("foo");
        assertEquals("Wrong number of keys", 1, parser.keySet().size());
        assertEquals("Wrong number of values", 2, res.length);

        // String array with null value should only add non-null values
        parser.add("foo", new String [] { "bla", null, "foo" });
        res = parser.getStrings("foo");
        assertEquals("Wrong number of keys", 1, parser.keySet().size());
        assertEquals("Wrong number of values", 4, res.length);
    }
    @Test
    public void testNonExistingResults()
    {
        parser.clear();

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertEquals("Wrong value for non existing key", 0.0, parser.getDouble("foo"), 0.001);
        assertNull(parser.getDoubles("foo"));
        assertNull(parser.getDoubleObject("foo"));
        assertNull(parser.getDoubleObjects("foo"));

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertNull(parser.getString("foo"));
        assertNull(parser.getStrings("foo"));

        assertEquals("Wrong value for non existing key", 0.0f, parser.getFloat("foo"), 0.001);
        assertNull(parser.getFloats("foo"));
        assertNull(parser.getFloatObject("foo"));
        assertNull(parser.getFloatObjects("foo"));

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertNull(parser.getBigDecimal("foo"));
        assertNull(parser.getBigDecimals("foo"));

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertEquals("Wrong value for non existing key", 0, parser.getInt("foo"));
        assertNull(parser.getInts("foo"));
        assertNull(parser.getIntObject("foo"));
        assertNull(parser.getIntObjects("foo"));

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertEquals("Wrong value for non existing key", 0, parser.getLong("foo"));
        assertNull(parser.getLongs("foo"));
        assertNull(parser.getLongObject("foo"));
        assertNull(parser.getLongObjects("foo"));

        assertEquals("Wrong number of keys", 0, parser.keySet().size());

        assertEquals("Wrong value for non existing key", 0, parser.getByte("foo"));
        assertNull(parser.getByteObject("foo"));

        assertEquals("Wrong number of keys", 0, parser.keySet().size());
    }

    @Test
    public void testBooleanArray() {
        String[] booleanString = {"on", "off", "false", "true", " ", "justaword"};
        parser.add("foo", booleanString);
        boolean[] theArray = parser.getBooleans("foo");

        assertEquals(6, theArray.length);
        assertTrue(theArray[0]);
        assertFalse(theArray[1]);
        assertFalse(theArray[2]);
        assertTrue(theArray[3]);
        assertFalse(theArray[4]);
        assertFalse(theArray[5]);

        assertNull(parser.getBooleans("keydontexist"));
    }
    @Test
    public void testBooleanObjectArray() {
        String[] booleanString = {"on", "off", "false", "true", " ", "justaword"};
        parser.add("foo", booleanString);
        Boolean[] theArray = parser.getBooleanObjects("foo");

        assertEquals(6, theArray.length);
        assertEquals(Boolean.TRUE, theArray[0]);
        assertEquals(Boolean.FALSE, theArray[1]);
        assertEquals(Boolean.FALSE, theArray[2]);
        assertEquals(Boolean.TRUE, theArray[3]);
        assertEquals(null, theArray[4]);
        assertEquals(null, theArray[5]);

        assertNull(parser.getBooleanObjects("keydontexist"));
    }
    @Test
    public void testGet() {

        // no param
        String result = parser.get("invalid");
        assertNull(result);

        // null value
        parser.add("valid", "value");
        assertEquals("value", parser.get("valid"));

        // only return the first added
        parser.add("multiple", "test");
        parser.add("multiple", "test2");
        assertEquals("test", parser.get("multiple"));


    }

}
