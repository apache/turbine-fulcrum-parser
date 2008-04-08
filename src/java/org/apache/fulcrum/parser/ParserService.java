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

import org.apache.avalon.framework.service.ServiceException;


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

    /** No folding */
    String URL_CASE_FOLDING_NONE_VALUE  = "none";

    /** Fold to lower case */
    String URL_CASE_FOLDING_LOWER_VALUE = "lower";

    /** Fold to upper case */
    String URL_CASE_FOLDING_UPPER_VALUE = "upper";

    /** No folding set */
    int URL_CASE_FOLDING_UNSET = 0;

    /** Folding set to "no folding" */
    int URL_CASE_FOLDING_NONE  = 1;

    /** Folding set to "lowercase" */
    int URL_CASE_FOLDING_LOWER = 2;

    /** Folding set to "uppercase" */
    int URL_CASE_FOLDING_UPPER = 3;

    /** Parse file upload items automatically */
    String AUTOMATIC_KEY = "automaticUpload";

    /**
     * <p> The default value of 'automaticUpload' property
     * (<code>false</code>).  If set to <code>true</code>, parsing the
     * multipart request will be performed automatically by {@link
     * org.apache.fulcrum.parser.ParameterParser}.  Otherwise, an {@link
     * org.apache.turbine.modules.Action} may decide to parse the
     * request by calling {@link #parseRequest(HttpServletRequest, String)
     * parseRequest} manually.
     */
    boolean AUTOMATIC_DEFAULT = false;

    /**
     * Get the parameter encoding that has been configured as default for
     * the ParserService.
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
     *
     * @return a new String.
     *
     */
    String convertAndTrim(String s);

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
    String convertAndTrim(String value, int fold);
    
    /**
     * Gets the folding value from the configuration
     *
     * @return The current Folding Value
     */
    int getUrlFolding();

    /**
     * Gets the automaticUpload value from the configuration
     *
     * @return The current automaticUpload Value
     */
    boolean getAutomaticUpload();

    /**
     * Use the UploadService if available to parse the given request 
     * for uploaded files
     *
     * @return A list of {@link org.apache.commons.upload.FileItem}s
     * 
     * @throws ServiceException if parsing fails or the UploadService 
     * is not available
     */
    List parseUpload(HttpServletRequest request) throws ServiceException;

    /**
     * Get a {@link ValueParser} instance from the service. Use the
     * default imlementation.
     * 
     * @return An object that implements ValueParser
     * 
     * @throws InstantiationException if the instance could not be created
     */
    ValueParser getParser(Class ppClass) throws InstantiationException;

    /**
     * Return a used Parser to the service. This allows for
     * pooling and recycling
     * 
     * @param parser
     */
    void putParser(ValueParser parser);
}

