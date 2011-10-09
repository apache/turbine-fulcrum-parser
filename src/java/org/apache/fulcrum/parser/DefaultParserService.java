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


import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.fulcrum.pool.PoolException;
import org.apache.fulcrum.pool.PoolService;
import org.apache.fulcrum.upload.UploadService;


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
    private int folding = URL_CASE_FOLDING_NONE;

    /** The automaticUpload setting from the configuration */
    private boolean automaticUpload = AUTOMATIC_DEFAULT;

    /**
     * The parameter encoding to use when parsing parameter strings
     */
    private String parameterEncoding = PARAMETER_ENCODING_DEFAULT;

    /**
     * The upload service component to use
     */
    private UploadService uploadService = null;

    /**
     * The pool service component to use
     */
    private PoolService poolService = null;

    /**
     * Get the character encoding that will be used by this ValueParser.
     */
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
     * @return A new String converted to lowercase and trimmed.
     */
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
    public String convertAndTrim(String value, int fold)
    {
        if(value == null) return "";

        String tmp = value.trim();

        switch (fold)
        {
            case URL_CASE_FOLDING_NONE:
            {
                break;
            }

            case URL_CASE_FOLDING_LOWER:
            {
                tmp = tmp.toLowerCase();
                break;
            }

            case URL_CASE_FOLDING_UPPER:
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
    public int getUrlFolding()
    {
        return folding;
    }

    /**
     * Gets the automaticUpload value from the configuration
     *
     * @return The current automaticUpload Value
     */
    public boolean getAutomaticUpload()
    {
        return automaticUpload;
    }

    /**
     * Use the UploadService if available to parse the given request
     * for uploaded files
     *
     * @return A list of {@link org.apache.commons.upload.FileItem}s
     *
     * @throws ServiceException if parsing fails or the UploadService
     * is not available
     */
    public List<FileItem> parseUpload(HttpServletRequest request) throws ServiceException
    {
        if (uploadService == null)
        {
            throw new ServiceException(ParserService.ROLE, "UploadService is not available.");
        }
        else
        {
            return uploadService.parseRequest(request);
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
    public ValueParser getParser(Class<? extends ValueParser> ppClass) throws InstantiationException
    {
        ValueParser vp = null;

        try
        {
            vp = (ValueParser) poolService.getInstance(ppClass);

            if (vp instanceof ParserServiceSupport)
            {
                ((ParserServiceSupport)vp).setParserService(this);
            }

            if (vp instanceof LogEnabled)
            {
                ((LogEnabled)vp).enableLogging(getLogger().getChildLogger(ppClass.getName()));
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
     * Return a used Parser to the service. This allows for
     * pooling and recycling
     *
     * @param parser
     */
    public void putParser(ValueParser parser)
    {
        parser.clear();
        poolService.putInstance(parser);
    }

    /**
     * Avalon component lifecycle method
     */
    public void configure(Configuration conf) throws ConfigurationException
    {
        if (folding == URL_CASE_FOLDING_UNSET)
        {
            String foldString = conf.getChild(URL_CASE_FOLDING_KEY).getValue(URL_CASE_FOLDING_NONE_VALUE).toLowerCase();

            folding = URL_CASE_FOLDING_NONE;

            getLogger().debug("Setting folding from " + foldString);

            if (StringUtils.isNotEmpty(foldString))
            {
                if (foldString.equals(URL_CASE_FOLDING_NONE_VALUE))
                {
                    folding = URL_CASE_FOLDING_NONE;
                }
                else if (foldString.equals(URL_CASE_FOLDING_LOWER_VALUE))
                {
                    folding = URL_CASE_FOLDING_LOWER;
                }
                else if (foldString.equals(URL_CASE_FOLDING_UPPER_VALUE))
                {
                    folding = URL_CASE_FOLDING_UPPER;
                }
                else
                {
                    getLogger().error("Got " + foldString + " from " + URL_CASE_FOLDING_KEY + " property, which is illegal!");
                    throw new ConfigurationException("Value " + foldString + " is illegal!");
                }
            }
        }

        parameterEncoding = conf.getChild(PARAMETER_ENCODING_KEY)
                            .getValue(PARAMETER_ENCODING_DEFAULT).toLowerCase();

        automaticUpload = conf.getAttributeAsBoolean(
                            AUTOMATIC_KEY,
                            AUTOMATIC_DEFAULT);
    }

    // ---------------- Avalon Lifecycle Methods ---------------------
    /**
     * Avalon component lifecycle method
     */
    public void service(ServiceManager manager) throws ServiceException
    {
        if (manager.hasService(UploadService.ROLE))
        {
            uploadService = (UploadService)manager.lookup(UploadService.ROLE);
        }
        else
        {
            /*
             * Automatic parsing of uploaded file items was requested but no
             * UploadService is available
             */
            if (getAutomaticUpload())
            {
                throw new ServiceException(ParserService.ROLE,
                        AUTOMATIC_KEY + " = true requires " +
                        UploadService.ROLE + " to be available");
            }
        }

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
