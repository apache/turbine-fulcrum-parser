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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.lang3.StringUtils;
import org.apache.fulcrum.parser.ValueParser.URLCaseFolding;
import org.apache.fulcrum.pool.PoolException;
import org.apache.fulcrum.pool.PoolService;


/**
 * The DefaultParserService provides the efault implementation
 * of a {@link ParserService}.
 *
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id: BaseValueParser.java 542062 2007-05-28 00:29:43Z seade $
 */
public class DefaultParserService
    extends AbstractLogEnabled
    implements ParserService,
               Configurable, Serviceable
{
    /** The folding from the configuration */
    private URLCaseFolding folding = URLCaseFolding.NONE;

    /** The automaticUpload setting from the configuration */
    private boolean automaticUpload = AUTOMATIC_DEFAULT;

    /**
     * The parameter encoding to use when parsing parameter strings
     */
    private String parameterEncoding = PARAMETER_ENCODING_DEFAULT;

    /**
     * The pool service component to use
     */
    private PoolService poolService = null;

    /**
     * Get the character encoding that will be used by this ValueParser.
     */
    @Override
    public String getParameterEncoding()
    {
        return parameterEncoding;
    }

    /**
     * Trims the string data and applies the conversion specified in
     * the property given by URL_CASE_FOLDING.  It returns a new
     * string so that it does not destroy the value data.
     *
     * @param value A String to be processed.
     * @return A new String converted to the case as specified by URL_CASE_FOLDING and trimmed.
     */
    @Override
    public String convert(String value)
    {
        return convertAndTrim(value);
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
        return convertAndTrim(value, getUrlFolding());
    }

    /**
     * A static version of the convert method, which
     * trims the string data and applies the conversion specified in
     * the property given by URL_CASE_FOLDING.  It returns a new
     * string so that it does not destroy the value data.
     *
     * @param value A String to be processed.
     * @return A new String converted to lowercase and trimmed.
     */
    @Override
    public String convertAndTrim(String value, URLCaseFolding fold)
    {
        if (value == null)
        {
            return "";
        }

        String tmp = value.trim();

        switch (fold)
        {
            case NONE:
            {
                break;
            }

            case LOWER:
            {
                tmp = tmp.toLowerCase();
                break;
            }

            case UPPER:
            {
                tmp = tmp.toUpperCase();
                break;
            }

            default:
            {
                getLogger().error("Passed " + fold + " as fold rule, which is illegal!");
                break;
            }
        }
        return tmp;
    }

    /**
     * Gets the folding value from the configuration
     *
     * @return The current Folding Value
     */
    @Override
    public URLCaseFolding getUrlFolding()
    {
        return folding;
    }

    /**
     * Gets the automaticUpload value from the configuration
     *
     * @return The current automaticUpload Value
     */
    @Override
    public boolean getAutomaticUpload()
    {
        return automaticUpload;
    }

    /**
     * Parse the given request for uploaded files
     *
     * @return A list of {@link javax.servlet.http.Part}s
     *
     * @throws ServiceException if parsing fails
     */
    @Override
    public List<Part> parseUpload(HttpServletRequest request) throws ServiceException
    {
        try
        {
            return new ArrayList<Part>(request.getParts());
        }
        catch (IOException | ServletException e)
        {
            throw new ServiceException(ParserService.ROLE, "Could not parse upload request", e);
        }
    }

    /**
     * Get a {@link ValueParser} instance from the service. Use the
     * given Class to create the object.
     *
     * @return An object that implements ValueParser
     *
     * @throws InstantiationException if the instance could not be created
     */
    @Override
    public <P extends ValueParser> P getParser(Class<P> ppClass) throws InstantiationException
    {
        P vp = null;

        try
        {
            @SuppressWarnings("unchecked") // Until PoolService is generified
            P parserInstance = (P) poolService.getInstance(ppClass);
            vp = parserInstance;

            if (vp instanceof ParserServiceSupport)
            {
                ((ParserServiceSupport)vp).setParserService(this);
            }

            if (vp instanceof LogEnabled)
            {
                ((LogEnabled)vp).enableLogging(getLogger().getChildLogger(ppClass.getSimpleName()));
            }
        }
        catch (PoolException pe)
        {
            throw new InstantiationException("Parser class '" + ppClass + "' is illegal. " + pe.getMessage());
        }
        catch (ClassCastException x)
        {
            throw new InstantiationException("Parser class '" + ppClass + "' is illegal. " + x.getMessage());
        }

        return vp;
    }

    /**
     * Clears the parse and puts it back into
     * the pool service. This allows for pooling 
     * and recycling
     *
     * @param parser The value parser to use
     */
    @Override
    public void putParser(ValueParser parser)
    {
        parser.clear();
        poolService.putInstance(parser);
    }

    /**
     * Avalon component lifecycle method
     * 
     * @param conf the configuration
     * @throws ConfigurationException Generic exception
     */
    @Override
    public void configure(Configuration conf) throws ConfigurationException
    {
        String foldString = conf.getChild(URL_CASE_FOLDING_KEY).getValue(URLCaseFolding.NONE.name()).toLowerCase();

        folding = URLCaseFolding.NONE;

        getLogger().debug("Setting folding from " + foldString);

        if (StringUtils.isNotEmpty(foldString))
        {
            try
            {
                folding = URLCaseFolding.valueOf(foldString.toUpperCase());
            }
            catch (IllegalArgumentException e)
            {
                getLogger().error("Got " + foldString + " from " + URL_CASE_FOLDING_KEY + " property, which is illegal!");
                throw new ConfigurationException("Value " + foldString + " is illegal!", e);
            }
        }

        parameterEncoding = conf.getChild(PARAMETER_ENCODING_KEY)
                            .getValue(PARAMETER_ENCODING_DEFAULT).toLowerCase();

        automaticUpload = conf.getChild(AUTOMATIC_KEY).getValueAsBoolean(AUTOMATIC_DEFAULT);

    }

    // ---------------- Avalon Lifecycle Methods ---------------------
    /**
     * Avalon component lifecycle method
     * 
     * @param manager The service manager instance
     * @throws ServiceException generic exception
     * 
     */
    @Override
    public void service(ServiceManager manager) throws ServiceException
    {
        if (manager.hasService(PoolService.ROLE))
        {
            poolService = (PoolService)manager.lookup(PoolService.ROLE);
        }
        else
        {
            throw new ServiceException(ParserService.ROLE,
                    "Service requires " +
                    PoolService.ROLE + " to be available");
        }
    }
}
