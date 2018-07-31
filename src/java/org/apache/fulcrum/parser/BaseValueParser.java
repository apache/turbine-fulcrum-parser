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

import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.fulcrum.pool.Recyclable;

/**
 * BaseValueParser is a base class for classes that need to parse
 * name/value Parameters, for example GET/POST data or Cookies
 * (DefaultParameterParser and DefaultCookieParser)
 *
 * <p>It can also be used standalone, for an example see DataStreamParser.
 *
 * <p>NOTE: The name= portion of a name=value pair may be converted
 * to lowercase or uppercase when the object is initialized and when
 * new data is added.  This behavior is determined by the url.case.folding
 * property in TurbineResources.properties.  Adding a name/value pair may
 * overwrite existing name=value pairs if the names match:
 *
 * <pre>
 * ValueParser vp = new BaseValueParser();
 * vp.add("ERROR",1);
 * vp.add("eRrOr",2);
 * int result = vp.getInt("ERROR");
 * </pre>
 *
 * In the above example, result is 2.
 *
 * @author <a href="mailto:ilkka.priha@simsoft.fi">Ilkka Priha</a>
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:sean@informage.net">Sean Legassick</a>
 * @author <a href="mailto:jvanzyl@periapt.com">Jason van Zyl</a>
 * @author <a href="mailto:jh@byteaction.de">J&#252;rgen Hoffmann</a>
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public class BaseValueParser
    implements ValueParser,
               Recyclable, ParserServiceSupport, LogEnabled
{
    /** The ParserService instance to query for conversion and configuration */
    protected ParserService parserService;

    /** A convenience logger */
    private Logger logger;

    /** String values which would evaluate to Boolean.TRUE */
    private static String[] trueValues = {"TRUE","T","YES","Y","1","ON"};

    /** String values which would evaluate to Boolean.FALSE */
    private static String[] falseValues = {"FALSE","F","NO","N","0","OFF"};

    /**
     * The character encoding to use when converting to byte arrays
     */
    private String characterEncoding = DEFAULT_CHARACTER_ENCODING;

    /**
     * Random access storage for parameter data.
     */
    protected Hashtable<String, Object> parameters = new Hashtable<String, Object>();

    /** The locale to use when converting dates, floats and decimals */
    private Locale locale = Locale.getDefault();

    /** The DateFormat to use for converting dates */
    private FastDateFormat dateFormat = FastDateFormat.getDateInstance(FastDateFormat.SHORT, locale);

    /** The NumberFormat to use when converting floats and decimals */
    private NumberFormat numberFormat = NumberFormat.getNumberInstance(locale);

    public BaseValueParser()
    {
        this(DEFAULT_CHARACTER_ENCODING);
    }

    /**
     * Constructor that takes a character encoding
     */
    public BaseValueParser(String characterEncoding)
    {
        this(characterEncoding, Locale.getDefault());
    }

    /**
     * Constructor that takes a character encoding and a locale
     */
    public BaseValueParser(String characterEncoding, Locale locale)
    {
        super();
        recycle(characterEncoding);
        setLocale(locale);
    }

    /**
     * Set a ParserService instance
     */
    @Override
    public void setParserService(ParserService parserService)
    {
        this.parserService = parserService;
    }

    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    @Override
    public void enableLogging(Logger logger)
    {
        this.logger = logger;
    }

    /**
     * Provide an Avalon logger to the derived classes
     *
     * @return An Avalon logger instance
     */
    protected Logger getLogger()
    {
        return logger;
    }

    /**
     * Recycles the parser.
     */
    @Override
    public void recycle()
    {
        recycle(DEFAULT_CHARACTER_ENCODING);
    }

    /**
     * Recycles the parser with a character encoding.
     *
     * @param characterEncoding the character encoding.
     */
    public void recycle(String characterEncoding)
    {
        setCharacterEncoding(characterEncoding);
    }

    /**
     * Disposes the parser.
     */
    @Override
    public void dispose()
    {
        clear();
        disposed = true;
    }

    /**
     * Clear all name/value pairs out of this object.
     */
    @Override
    public void clear()
    {
        parameters.clear();
    }

    /**
     * Set the character encoding that will be used by this ValueParser.
     */
    @Override
    public void setCharacterEncoding(String s)
    {
        characterEncoding = s;
    }

    /**
     * Get the character encoding that will be used by this ValueParser.
     */
    @Override
    public String getCharacterEncoding()
    {
        return characterEncoding;
    }

    /**
     * Set the locale that will be used by this ValueParser.
     */
    @Override
    public void setLocale(Locale l)
    {
        locale = l;
        setDateFormat(FastDateFormat.getDateInstance(FastDateFormat.SHORT, locale));
        setNumberFormat(NumberFormat.getNumberInstance(locale));
    }

    /**
     * Get the locale that will be used by this ValueParser.
     */
    @Override
    public Locale getLocale()
    {
        return locale;
    }

    /**
     * Set the date format that will be used by this ValueParser.
     */
    @Override
    public void setDateFormat(FastDateFormat df)
    {
        dateFormat = df;
    }

    /**
     * Get the date format that will be used by this ValueParser.
     */
    @Override
    public FastDateFormat getDateFormat()
    {
        return dateFormat;
    }

    /**
     * Set the number format that will be used by this ValueParser.
     */
    @Override
    public void setNumberFormat(NumberFormat nf)
    {
        numberFormat = nf;
    }

    /**
     * Get the number format that will be used by this ValueParser.
     */
    @Override
    public NumberFormat getNumberFormat()
    {
        return numberFormat;
    }

    /**
     * Add a name/value pair into this object.
     *
     * @param name A String with the name.
     * @param value A double with the value.
     */
    @Override
    public void add(String name, double value)
    {
        NumberFormat nf = (NumberFormat) numberFormat.clone();
        add(name, nf.format(value));
    }

    /**
     * Add a name/value pair into this object.
     *
     * @param name A String with the name.
     * @param value An int with the value.
     */
    @Override
    public void add(String name, int value)
    {
        add(name, (long)value);
    }

    /**
     * Add a name/value pair into this object.
     *
     * @param name A String with the name.
     * @param value An Integer with the value.
     */
    @Override
    public void add(String name, Integer value)
    {
        if (value != null)
        {
            add(name, value.intValue());
        }
    }

    /**
     * Add a name/value pair into this object.
     *
     * @param name A String with the name.
     * @param value A long with the value.
     */
    @Override
    public void add(String name, long value)
    {
        add(name, Long.toString(value));
    }

    /**
     * Add a name/value pair into this object.
     *
     * @param name A String with the name.
     * @param value A long with the value.
     */
    @Override
    public void add(String name, String value)
    {
        if (value != null)
        {
            String [] items = getParam(name);
            items = ArrayUtils.add(items, value);
            putParam(name, items);
        }
    }

    /**
     * Add an array of Strings for a key. This
     * is simply adding all the elements in the
     * array one by one.
     *
     * @param name A String with the name.
     * @param value A String Array.
     */
    @Override
    public void add(String name, String [] value)
    {
        // ArrayUtils.addAll() looks promising but it would also add
        // null values into the parameters array, so we can't use that.
        if (value != null)
        {
            for (int i = 0 ; i < value.length; i++)
            {
                if (value[i] != null)
                {
                    add(name, value[i]);
                }
            }
        }
    }

    /**
     * Removes the named parameter from the contained hashtable. Wraps to the
     * contained <code>Map.remove()</code>.
     *
     * @return The value that was mapped to the key (a <code>String[]</code>)
     *         or <code>null</code> if the key was not mapped.
     */
    @Override
    public Object remove(String name)
    {
        return parameters.remove(convert(name));
    }

    /**
     * Trims the string data and applies the conversion specified in
     * the property given by URL_CASE_FOLDING.  It returns a new
     * string so that it does not destroy the value data.
     *
     * @param value A String to be processed.
     * @return A new String converted to lowercase and trimmed.
     */
    @Override
    public String convert(String value)
    {
        return convertAndTrim(value);
    }

    /**
     * Determine whether a given key has been inserted.  All keys are
     * stored in lowercase strings, so override method to account for
     * this.
     *
     * @param key An Object with the key to search for.
     * @return True if the object is found.
     */
    @Override
    public boolean containsKey(Object key)
    {
        return parameters.containsKey(convert(String.valueOf(key)));
    }

    /**
     * Gets the set of keys
     *
     * @return A <code>Set</code> of the keys.
     */
    @Override
    public Set<String> keySet()
    {
        return parameters.keySet();
    }

    /**
     * Returns all the available parameter names.
     *
     * @return A object array with the keys.
     */
    @Override
    public String[] getKeys()
    {
        return keySet().toArray(new String[0]);
    }

    /**
     * Gets an iterator over the set of keys
     *
     * @return An <code>Iterator</code> over the keys.
     */
    @Override
    public Iterator<String> iterator()
    {
        return parameters.keySet().iterator();
    }

    /**
     * Returns a Boolean object for the given string. If the value
     * can not be parsed as a boolean, null is returned.
     * <p>
     * Valid values for true: true, t, on, 1, yes, y<br>
     * Valid values for false: false, f, off, 0, no, n<br>
     * <p>
     * The string is compared without reguard to case.
     *
     * @param string A String with the value.
     * @return A Boolean.
     */
    private Boolean parseBoolean(String string)
    {
        Boolean result = null;
        String value = StringUtils.trim(string);

        if (StringUtils.isNotEmpty(value))
        {
            for (int cnt = 0;
            cnt < Math.max(trueValues.length, falseValues.length); cnt++)
            {
                // Short-cut evaluation or bust!
                if ((cnt < trueValues.length) &&
                   value.equalsIgnoreCase(trueValues[cnt]))
                {
                    result = Boolean.TRUE;
                    break;
                }

                if ((cnt < falseValues.length) &&
                   value.equalsIgnoreCase(falseValues[cnt]))
                {
                    result = Boolean.FALSE;
                    break;
                }
            }

            if (result == null)
            {
                if (getLogger().isWarnEnabled())
                {
                    getLogger().warn("Parameter with value of ("
                            + value + ") could not be converted to a Boolean");
                }
            }
        }

        return result;
    }

    /**
     * Return a boolean for the given name.  If the name does not
     * exist, return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A boolean.
     */
    @Override
    public boolean getBoolean(String name, boolean defaultValue)
    {
        Boolean result = getBooleanObject(name);
        return (result == null ? defaultValue : result.booleanValue());
    }

    /**
     * Return a boolean for the given name.  If the name does not
     * exist, return false.
     *
     * @param name A String with the name.
     * @return A boolean.
     */
    @Override
    public boolean getBoolean(String name)
    {
        return getBoolean(name, false);
    }

    /**
     * Return an array of booleans for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A boolean[].
     */
    @Override
    public boolean[] getBooleans(String name)
    {
        boolean[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new boolean[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Boolean bool = parseBoolean(value[i]);
                result[i] = (bool == null ? false : bool.booleanValue());
            }
        }
        return result;
    }

    /**
     * Returns a Boolean object for the given name.  If the parameter
     * does not exist or can not be parsed as a boolean, null is returned.
     * <p>
     * Valid values for true: true, on, 1, yes<br>
     * Valid values for false: false, off, 0, no<br>
     * <p>
     * The string is compared without reguard to case.
     *
     * @param name A String with the name.
     * @return A Boolean.
     */
    @Override
    public Boolean getBooleanObject(String name)
    {
        return parseBoolean(getString(name));
    }

    /**
     * Returns a Boolean object for the given name.  If the parameter
     * does not exist or can not be parsed as a boolean, null is returned.
     * <p>
     * Valid values for true: true, on, 1, yes<br>
     * Valid values for false: false, off, 0, no<br>
     * <p>
     * The string is compared without reguard to case.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A Boolean.
     */
    @Override
    public Boolean getBooleanObject(String name, Boolean defaultValue)
    {
        Boolean result = getBooleanObject(name);
        return (result == null ? defaultValue : result);
    }

    /**
     * Return an array of Booleans for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A Boolean[].
     */
    @Override
    public Boolean[] getBooleanObjects(String name)
    {
        Boolean[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new Boolean[value.length];
            for (int i = 0; i < value.length; i++)
            {
                result[i] = parseBoolean(value[i]);
            }
        }
        return result;
    }

    /**
     * Return a {@link Number} for the given string.
     *
     * @param string A String with the value.
     * @return A Number.
     *
     */
    private Number parseNumber(String string)
    {
        Number result = null;
        String value = StringUtils.trim(string);

        if (StringUtils.isNotEmpty(value))
        {
            NumberFormat nf = (NumberFormat) numberFormat.clone();
            ParsePosition pos = new ParsePosition(0);
            Number number = nf.parse(value, pos);

            if (pos.getIndex() == value.length())
            {
                // completely parsed
                result = number;
            }
            else
            {
                if (getLogger().isWarnEnabled())
                {
                    getLogger().warn("Parameter with value of ("
                            + value + ") could not be converted to a Number at position " + pos.getIndex());
                }
            }
        }

        return result;
    }

    /**
     * Return a {@link Number} for the given name.  If the name does not
     * exist, return null. This is the base function for all numbers.
     *
     * @param name A String with the name.
     * @return A Number.
     *
     */
    private Number getNumber(String name)
    {
        return parseNumber(getString(name));
    }

    /**
     * Return a double for the given name.  If the name does not
     * exist, return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A double.
     */
    @Override
    public double getDouble(String name, double defaultValue)
    {
        Number number = getNumber(name);
        return (number == null ? defaultValue : number.doubleValue());
    }

    /**
     * Return a double for the given name.  If the name does not
     * exist, return 0.0.
     *
     * @param name A String with the name.
     * @return A double.
     */
    @Override
    public double getDouble(String name)
    {
        return getDouble(name, 0.0);
    }

    /**
     * Return an array of doubles for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A double[].
     */
    @Override
    public double[] getDoubles(String name)
    {
        double[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new double[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = (number == null ? 0.0 : number.doubleValue());
            }
        }
        return result;
    }

    /**
     * Return a Double for the given name.  If the name does not
     * exist, return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A double.
     */
    @Override
    public Double getDoubleObject(String name, Double defaultValue)
    {
        Number result = getNumber(name);
        return (result == null ? defaultValue : new Double(result.doubleValue()));
    }

    /**
     * Return a Double for the given name.  If the name does not
     * exist, return null.
     *
     * @param name A String with the name.
     * @return A double.
     */
    @Override
    public Double getDoubleObject(String name)
    {
        return getDoubleObject(name, null);
    }

    /**
     * Return an array of doubles for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A double[].
     */
    @Override
    public Double[] getDoubleObjects(String name)
    {
        Double[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new Double[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = (number == null ? null : new Double(number.doubleValue()));
            }
        }
        return result;
    }

    /**
     * Return a float for the given name.  If the name does not
     * exist, return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A float.
     */
    @Override
    public float getFloat(String name, float defaultValue)
    {
        Number number = getNumber(name);
        return (number == null ? defaultValue : number.floatValue());
    }

    /**
     * Return a float for the given name.  If the name does not
     * exist, return 0.0.
     *
     * @param name A String with the name.
     * @return A float.
     */
    @Override
    public float getFloat(String name)
    {
        return getFloat(name, 0.0f);
    }

    /**
     * Return an array of floats for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A float[].
     */
    @Override
    public float[] getFloats(String name)
    {
        float[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new float[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = (number == null ? 0.0f : number.floatValue());
            }
        }
        return result;
    }

    /**
     * Return a Float for the given name.  If the name does not
     * exist, return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A Float.
     */
    @Override
    public Float getFloatObject(String name, Float defaultValue)
    {
        Number result = getNumber(name);
        return (result == null ? defaultValue : new Float(result.floatValue()));
    }

    /**
     * Return a float for the given name.  If the name does not
     * exist, return null.
     *
     * @param name A String with the name.
     * @return A Float.
     */
    @Override
    public Float getFloatObject(String name)
    {
        return getFloatObject(name, null);
    }

    /**
     * Return an array of floats for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A float[].
     */
    @Override
    public Float[] getFloatObjects(String name)
    {
        Float[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new Float[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = (number == null ? null : new Float(number.floatValue()));
            }
        }
        return result;
    }

    /**
     * Return a BigDecimal for the given name.  If the name does not
     * exist, return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A BigDecimal.
     */
    @Override
    public BigDecimal getBigDecimal(String name, BigDecimal defaultValue)
    {
        Number result = getNumber(name);
        return (result == null ? defaultValue : new BigDecimal(result.doubleValue()));
    }

    /**
     * Return a BigDecimal for the given name.  If the name does not
     * exist, return null.
     *
     * @param name A String with the name.
     * @return A BigDecimal.
     */
    @Override
    public BigDecimal getBigDecimal(String name)
    {
        return getBigDecimal(name, null);
    }

    /**
     * Return an array of BigDecimals for the given name.  If the name
     * does not exist, return null.
     *
     * @param name A String with the name.
     * @return A BigDecimal[].
     */
    @Override
    public BigDecimal[] getBigDecimals(String name)
    {
        BigDecimal[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new BigDecimal[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = (number == null ? null : new BigDecimal(number.doubleValue()));
            }
        }
        return result;
    }

    /**
     * Return an int for the given name.  If the name does not exist,
     * return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return An int.
     */
    @Override
    public int getInt(String name, int defaultValue)
    {
        Number result = getNumber(name);
        return ((result == null || result instanceof Double) ? defaultValue : result.intValue());
    }

    /**
     * Return an int for the given name.  If the name does not exist,
     * return 0.
     *
     * @param name A String with the name.
     * @return An int.
     */
    @Override
    public int getInt(String name)
    {
        return getInt(name, 0);
    }

    /**
     * Return an array of ints for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return An int[].
     */
    @Override
    public int[] getInts(String name)
    {
        int[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new int[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = ((number == null || number instanceof Double) ? 0 : number.intValue());
            }
        }
        return result;
    }

    /**
     * Return an Integer for the given name.  If the name does not exist,
     * return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return An Integer.
     */
    @Override
    public Integer getIntObject(String name, Integer defaultValue)
    {
        Number result = getNumber(name);
        return ((result == null || result instanceof Double) ? defaultValue : Integer.valueOf(result.intValue()));
    }

    /**
     * Return an Integer for the given name.  If the name does not exist,
     * return null.
     *
     * @param name A String with the name.
     * @return An Integer.
     */
    @Override
    public Integer getIntObject(String name)
    {
        return getIntObject(name, null);
    }

    /**
     * Return an array of Integers for the given name.  If the name
     * does not exist, return null.
     *
     * @param name A String with the name.
     * @return An Integer[].
     */
    @Override
    public Integer[] getIntObjects(String name)
    {
        Integer[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new Integer[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = ((number == null || number instanceof Double) ? null : Integer.valueOf(number.intValue()));
            }
        }
        return result;
    }

    /**
     * Return a long for the given name.  If the name does not exist,
     * return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A long.
     */
    @Override
    public long getLong(String name, long defaultValue)
    {
        Number result = getNumber(name);
        return ((result == null || result instanceof Double) ? defaultValue : result.longValue());
    }

    /**
     * Return a long for the given name.  If the name does not exist,
     * return 0.
     *
     * @param name A String with the name.
     * @return A long.
     */
    @Override
    public long getLong(String name)
    {
        return getLong(name, 0);
    }

    /**
     * Return an array of longs for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A long[].
     */
    @Override
    public long[] getLongs(String name)
    {
        long[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new long[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = ((number == null || number instanceof Double) ? 0L : number.longValue());
            }
        }
        return result;
    }

    /**
     * Return an array of Longs for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A Long[].
     */
    @Override
    public Long[] getLongObjects(String name)
    {
        Long[] result = null;
        String value[] = getParam(name);
        if (value != null)
        {
            result = new Long[value.length];
            for (int i = 0; i < value.length; i++)
            {
                Number number = parseNumber(value[i]);
                result[i] = ((number == null || number instanceof Double) ? null : Long.valueOf(number.longValue()));
            }
        }
        return result;
    }

    /**
     * Return a Long for the given name.  If the name does
     * not exist, return null.
     *
     * @param name A String with the name.
     * @return A Long.
     */
    @Override
    public Long getLongObject(String name)
    {
        return getLongObject(name, null);
    }

    /**
     * Return a Long for the given name.  If the name does
     * not exist, return the default value.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A Long.
     */
    @Override
    public Long getLongObject(String name, Long defaultValue)
    {
        Number result = getNumber(name);
        return ((result == null || result instanceof Double) ? defaultValue : Long.valueOf(result.longValue()));
    }

    /**
     * Return a byte for the given name.  If the name does not exist,
     * return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A byte.
     */
    @Override
    public byte getByte(String name, byte defaultValue)
    {
        Number result = getNumber(name);
        return ((result == null || result instanceof Double) ? defaultValue : result.byteValue());
    }

    /**
     * Return a byte for the given name.  If the name does not exist,
     * return 0.
     *
     * @param name A String with the name.
     * @return A byte.
     */
    @Override
    public byte getByte(String name)
    {
        return getByte(name, (byte) 0);
    }

    /**
     * Return an array of bytes for the given name.  If the name does
     * not exist, return null. The array is returned according to the
     * HttpRequest's character encoding.
     *
     * @param name A String with the name.
     * @return A byte[].
     * @exception UnsupportedEncodingException
     */
    @Override
    public byte[] getBytes(String name)
            throws UnsupportedEncodingException
    {
        byte result[] = null;
        String value = getString(name);
        if (value != null)
        {
            result = value.getBytes(getCharacterEncoding());
        }
        return result;
    }

    /**
     * Return a byte for the given name.  If the name does not exist,
     * return defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A byte.
     */
    @Override
    public Byte getByteObject(String name, Byte defaultValue)
    {
        Number result = getNumber(name);
        return ((result == null || result instanceof Double) ? defaultValue : Byte.valueOf(result.byteValue()));
    }

    /**
     * Return a byte for the given name.  If the name does not exist,
     * return 0.
     *
     * @param name A String with the name.
     * @return A byte.
     */
    @Override
    public Byte getByteObject(String name)
    {
        return getByteObject(name, null);
    }

    /**
     * Return a String for the given name.  If the name does not
     * exist, return null.
     *
     * @param name A String with the name.
     * @return A String or null if the key is unknown.
     */
    @Override
    public String getString(String name)
    {
        String [] value = getParam(name);

        return (value == null
                || value.length == 0)
                ? null : value[0];
    }

    /**
     * Return a String for the given name.  If the name does not
     * exist, return null. It is the same as the getString() method
     * however has been added for simplicity when working with
     * template tools such as Velocity which allow you to do
     * something like this:
     *
     * <code>$data.Parameters.form_variable_name</code>
     *
     * @param name A String with the name.
     * @return A String.
     */
    @Override
    public String get(String name)
    {
        return getString(name);
    }

    /**
     * Return a String for the given name.  If the name does not
     * exist, return the defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A String.
     */
    @Override
    public String getString(String name, String defaultValue)
    {
        String value = getString(name);

        return (StringUtils.isEmpty(value) ? defaultValue : value );
    }

    /**
     * Set a parameter to a specific value.
     *
     * This is useful if you want your action to override the values
     * of the parameters for the screen to use.
     * @param name The name of the parameter.
     * @param value The value to set.
     */
    @Override
    public void setString(String name, String value)
    {
        if (value != null)
        {
            putParam(name, new String[]{value});
        }
    }

    /**
     * Return an array of Strings for the given name.  If the name
     * does not exist, return null.
     *
     * @param name A String with the name.
     * @return A String[].
     */
    @Override
    public String[] getStrings(String name)
    {
        return getParam(name);
    }

    /**
     * Return an array of Strings for the given name.  If the name
     * does not exist, return the defaultValue.
     *
     * @param name A String with the name.
     * @param defaultValue The default value.
     * @return A String[].
     */
    @Override
    public String[] getStrings(String name, String[] defaultValue)
    {
        String[] value = getParam(name);

        return (value == null || value.length == 0)
            ? defaultValue : value;
    }

    /**
     * Set a parameter to a specific value.
     *
     * This is useful if you want your action to override the values
     * of the parameters for the screen to use.
     * @param name The name of the parameter.
     * @param values The value to set.
     */
    @Override
    public void setStrings(String name, String[] values)
    {
        if (values != null)
        {
            putParam(name, values);
        }
    }

    /**
     * Return an Object for the given name.  If the name does not
     * exist, return null.
     *
     * @param name A String with the name.
     * @return An Object.
     */
    @Override
    public Object getObject(String name)
    {
        return getString(name);
    }

    /**
     * Return an array of Objects for the given name.  If the name
     * does not exist, return null.
     *
     * @param name A String with the name.
     * @return An Object[].
     */
    @Override
    public Object[] getObjects(String name)
    {
        return getParam(name);
    }

    /**
     * Returns a {@link java.util.Date} object.  String is parsed by supplied
     * DateFormat.  If the name does not exist or the value could not be
     * parsed into a date return the defaultValue.
     *
     * @param name A String with the name.
     * @param df A DateFormat.
     * @param defaultValue The default value.
     * @return A Date.
     */
    @Override
    public Date getDate(String name, DateFormat df, Date defaultValue)
    {
        Date result = defaultValue;
        String value = StringUtils.trim(getString(name));

        if (StringUtils.isNotEmpty(value))
        {
            try
            {
                // Reject invalid dates.
                df.setLenient(false);
                result = df.parse(value);
            }
            catch (ParseException e)
            {
                logConversionFailure(name, value, "Date");
            }
        }

        return result;
    }

    /**
     * Returns a {@link java.util.Date} object.  If there are DateSelector or
     * TimeSelector style parameters then these are used.  If not and there
     * is a parameter 'name' then this is parsed by DateFormat.  If the
     * name does not exist, return null.
     *
     * @param name A String with the name.
     * @return A Date.
     */
    @Override
    public Date getDate(String name)
    {
        Date result = null;
        String value = StringUtils.trim(getString(name));

        if (StringUtils.isNotEmpty(value))
        {
            try
            {
                // Reject invalid dates.
                result = dateFormat.parse(value);
            }
            catch (ParseException e)
            {
                logConversionFailure(name, value, "Date");
            }
        }

        return result;
    }

    /**
     * Returns a {@link java.util.Date} object.  String is parsed by supplied
     * DateFormat.  If the name does not exist, return null.
     *
     * @param name A String with the name.
     * @param df A DateFormat.
     * @return A Date.
     */
    @Override
    public Date getDate(String name, DateFormat df)
    {
        return getDate(name, df, null);
    }

    /**
     * Uses bean introspection to set writable properties of bean from
     * the parameters, where a (case-insensitive) name match between
     * the bean property and the parameter is looked for.
     *
     * @param bean An Object.
     * @exception Exception a generic exception.
     */
    @Override
    public void setProperties(Object bean) throws Exception
    {
        Class<?> beanClass = bean.getClass();
        PropertyDescriptor[] props
                = Introspector.getBeanInfo(beanClass).getPropertyDescriptors();

        for (int i = 0; i < props.length; i++)
        {
            String propname = props[i].getName();
            Method setter = props[i].getWriteMethod();
            if (setter != null && containsKey(propname))
            {
                setProperty(bean, props[i]);
            }
        }
    }

    /**
     * Simple method that attempts to get a textual representation of
     * this object's name/value pairs.  String[] handling is currently
     * a bit rough.
     *
     * @return A textual representation of the parsed name/value pairs.
     */
    @Override
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        for (String name : keySet())
        {
            sb.append('{');
            sb.append(name);
            sb.append('=');
            Object [] params = getToStringParam(name);

            if (params == null)
            {
                sb.append("unknown?");
            }
            else if (params.length == 0)
            {
                sb.append("empty");
            }
            else
            {
                sb.append('[');
                sb.append(StringUtils.join(params, ", "));
                sb.append(']');
            }
            sb.append("}\n");
        }

        return sb.toString();
    }

    /**
     * This method is only used in toString() and can be used by
     * derived classes to add their local parameters to the toString()

     * @param name A string with the name
     *
     * @return the value object array or null if not set
     */
    protected Object [] getToStringParam(final String name)
    {
        return getParam(name);
    }

    /**
     * Set the property 'prop' in the bean to the value of the
     * corresponding parameters.  Supports all types supported by
     * getXXX methods plus a few more that come for free because
     * primitives have to be wrapped before being passed to invoke
     * anyway.
     *
     * @param bean An Object.
     * @param prop A PropertyDescriptor.
     * @exception Exception a generic exception.
     */
    protected void setProperty(Object bean,
                               PropertyDescriptor prop)
            throws Exception
    {
        if (prop instanceof IndexedPropertyDescriptor)
        {
            throw new Exception(prop.getName() +
                    " is an indexed property (not supported)");
        }

        Method setter = prop.getWriteMethod();
        if (setter == null)
        {
            throw new Exception(prop.getName() +
                    " is a read only property");
        }

        Class<?> propclass = prop.getPropertyType();
        Object arg = null;

        if (propclass == String.class)
        {
            arg = getString(prop.getName());
        }
        else if (propclass == Byte.class || propclass == Byte.TYPE)
        {
            arg = getByteObject(prop.getName());
        }
        else if (propclass == Integer.class || propclass == Integer.TYPE)
        {
            arg = getIntObject(prop.getName());
        }
        else if (propclass == Long.class || propclass == Long.TYPE)
        {
            arg = getLongObject(prop.getName());
        }
        else if (propclass == Boolean.class || propclass == Boolean.TYPE)
        {
            arg = getBooleanObject(prop.getName());
        }
        else if (propclass == Double.class || propclass == Double.TYPE)
        {
            arg = getDoubleObject(prop.getName());
        }
        else if (propclass == Float.class || propclass == Float.TYPE)
        {
            arg = getFloatObject(prop.getName());
        }
        else if (propclass == BigDecimal.class)
        {
            arg = getBigDecimal(prop.getName());
        }
        else if (propclass == String[].class)
        {
            arg = getStrings(prop.getName());
        }
        else if (propclass == Object.class)
        {
            arg = getObject(prop.getName());
        }
        else if (propclass == int[].class)
        {
            arg = getInts(prop.getName());
        }
        else if (propclass == Integer[].class)
        {
            arg = getIntObjects(prop.getName());
        }
        else if (propclass == Date.class)
        {
            arg = getDate(prop.getName());
        }
        else
        {
            throw new Exception("property "
                    + prop.getName()
                    + " is of unsupported type "
                    + propclass.toString());
        }

        setter.invoke(bean, arg);
    }

    /**
     * Puts a key into the parameters map. Makes sure that the name is always
     * mapped correctly. This method also enforces the usage of arrays for the
     * parameters.
     *
     * @param name A String with the name.
     * @param value An array of Objects with the values.
     *
     */
    protected void putParam(final String name, final String [] value)
    {
        String key = convert(name);
        if (key != null)
        {
            parameters.put(key, value);
        }
    }

    /**
     * fetches a key from the parameters map. Makes sure that the name is
     * always mapped correctly.
     *
     * @param name A string with the name
     *
     * @return the value object array or null if not set
     */
    protected String [] getParam(final String name)
    {
        String key = convert(name);
        Object value = parameters.get(key);

        // todo sgoeschl 20070405 quick fix for Scott's test case - need to
        // be reworked for proper functioning according to TV
        if(value instanceof String[])
        {
            return (String []) parameters.get(key);
        }
        else
        {
            return null;
        }
    }


    /** recyclable support **/

    /**
     * The disposed flag.
     */
    private boolean disposed;

    /**
     * Checks whether the object is disposed.
     *
     * @return true, if the object is disposed.
     */
    @Override
    public boolean isDisposed()
    {
        return disposed;
    }

    /**
     * Writes a log message about a conversion failure.
     *
     * @param paramName name of the parameter which could not be converted
     * @param value value of the parameter
     * @param type target data type.
     */
    private void logConversionFailure(String paramName,
                                      String value, String type)
    {
        getLogger().warn("Parameter (" + paramName
                + ") with value of ("
                + value + ") could not be converted to a " + type);
    }

    /**
     * Convert a String value according to the url-case-folding property.
     *
     * @param value the String to convert
     *
     * @return a new String.
     *
     */
    @Override
    public String convertAndTrim(String value)
    {
        return parserService.convertAndTrim(value);
    }

    /**
     * A convert method, which trims the string data and applies the
     * conversion specified in the parameter given. It returns a new
     * string so that it does not destroy the value data.
     *
     * @param value A String to be processed.
     * @param fold The parameter folding to be applied
     * (see {@link ParserService})
     * @return A new String converted to the correct case and trimmed.
     */
    @Override
    public String convertAndTrim(String value, URLCaseFolding fold)
    {
        return parserService.convertAndTrim(value, fold);
    }

    /**
     * Gets the folding value from the ParserService configuration
     *
     * @return The current Folding Value
     */
    @Override
    public URLCaseFolding getUrlFolding()
    {
        return parserService.getUrlFolding();
    }
}
