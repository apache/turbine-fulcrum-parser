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
import javax.servlet.http.Part;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.fulcrum.parser.ValueParser.URLCaseFolding;

/**
 * ParserService defines the methods which are needed by the parser objects
 * to get their necessities.
 *
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id: ValueParser.java 535465 2007-05-05 06:58:06Z tv $
 */
public interface ParserService
{
    /** Avalon Identifier **/
    String ROLE = ParserService.class.getName();

    /** Default Encoding for Parameter Parser */
    String PARAMETER_ENCODING_DEFAULT = "ISO-8859-1";

    /** Key for the Parameter Parser Encoding */
    String PARAMETER_ENCODING_KEY = "parameterEncoding";

    /** Property for setting the URL folding value */
    String URL_CASE_FOLDING_KEY = "urlCaseFolding";

    /** Parse file upload items automatically */
    String AUTOMATIC_KEY = "automaticUpload";
    
    /** commons pool2 parameters */
    String POOL_KEY = "pool2";

    /**
     * <p> The default value of 'automaticUpload' property
     * (<code>false</code>).  If set to <code>true</code>, parsing the
     * multipart request will be performed automatically by {@link
     * org.apache.fulcrum.parser.ParameterParser}.  Otherwise, an 
     * org.apache.turbine.modules.Action may decide to parse the
     * request by calling {@link #parseUpload(HttpServletRequest)
     * parseRequest} manually.
     */
    boolean AUTOMATIC_DEFAULT = false;
    
    /**
     * <p> The default value of 'maxTotal' property in 'pool'
     * (<code>1024</code>). The default pool capacity.
     */
    int DEFAULT_POOL_CAPACITY = 1024;
    
    /**
     * <p> The default value of 'maxIdle' property in 'pool'
     * (<code>2</code>). The default maximum idle object.
     */
    int DEFAULT_MAX_IDLE = 2;

    /**
     * Get the parameter encoding that has been configured as default for
     * the ParserService.
     * 
     * @return A String for the parameter encoding
     */
    String getParameterEncoding();

    /**
     * Trims the string data and applies the conversion specified in
     * the property given by URL_CASE_FOLDING. It returns a new
     * string so that it does not destroy the value data.
     *
     * @param value A String to be processed.
     * @return A new String converted to lowercase and trimmed.
     */
    String convert(String value);

    /**
     * Convert a String value according to the url-case-folding property.
     *
     * @param value the String to convert
     * @return a new String.
     *
     */
    String convertAndTrim(String value);

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
    String convertAndTrim(String value, URLCaseFolding fold);

    /**
     * Gets the folding value from the configuration
     *
     * @return The current Folding Value
     */
    URLCaseFolding getUrlFolding();

    /**
     * Gets the automaticUpload value from the configuration
     *
     * @return The current automaticUpload Value
     */
    boolean getAutomaticUpload();

    /**
     * Parse the given request for uploaded files
     *
     * @param request the HttpServletRequest object
     * @return A list of {@link javax.servlet.http.Part}s
     * @throws ServiceException if parsing fails
     */
    List<Part> parseUpload(HttpServletRequest request) throws ServiceException;

    /**
     * Get a {@link ValueParser} instance from the service. Use the
     * default implementation.
     *
     * @param <P> The ValueParser we are using
     * @param ppClass parameter parser class
     * @return An object that implements ValueParser
     * @throws InstantiationException if the instance could not be created
     */
    <P extends ValueParser> P getParser(Class<P> ppClass) throws InstantiationException;

    /**
     * Put the parser into service
     *
     * @param parser The value parser to be used 
     */
    void putParser(ValueParser parser);
}

