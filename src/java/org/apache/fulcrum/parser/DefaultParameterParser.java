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


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.ArrayUtils;
import org.apache.fulcrum.upload.UploadService;

/**
 * DefaultParameterParser is a utility object to handle parsing and
 * retrieving the data passed via the GET/POST/PATH_INFO arguments.
 *
 * <p>NOTE: The name= portion of a name=value pair may be converted
 * to lowercase or uppercase when the object is initialized and when
 * new data is added.  This behaviour is determined by the url.case.folding
 * property in TurbineResources.properties.  Adding a name/value pair may
 * overwrite existing name=value pairs if the names match:
 *
 * <pre>
 * ParameterParser pp = data.getParameters();
 * pp.add("ERROR",1);
 * pp.add("eRrOr",2);
 * int result = pp.getInt("ERROR");
 * </pre>
 *
 * In the above example, result is 2.
 *
 * @author <a href="mailto:ilkka.priha@simsoft.fi">Ilkka Priha</a>
 * @author <a href="mailto:jon@clearink.com">Jon S. Stevens</a>
 * @author <a href="mailto:sean@informage.net">Sean Legassick</a>
 * @author <a href="mailto:jh@byteaction.de">J&#252;rgen Hoffmann</a>
 * @version $Id$
 */
public class DefaultParameterParser
    extends BaseValueParser
    implements ParameterParser,
               Serviceable
{
    /**
     * The servlet request to parse.
     */
    private HttpServletRequest request = null;

    /**
     * The raw data of a file upload.
     */
    private byte[] uploadData = null;

    /**
     * The upload service component to use
     */
    private UploadService uploadService = null;

    /**
     * Create a new empty instance of ParameterParser.  Uses the
     * default character encoding (US-ASCII).
     *
     * <p>To add name/value pairs to this set of parameters, use the
     * <code>add()</code> methods.
     *
     */
    public DefaultParameterParser()
    {
        super();
    }

    /**
     * Create a new empty instance of ParameterParser. Takes a
     * character encoding name to use when converting strings to
     * bytes.
     *
     * <p>To add name/value pairs to this set of parameters, use the
     * <code>add()</code> methods.
     *
     * @param characterEncoding The character encoding of strings.
     */
    public DefaultParameterParser(String characterEncoding)
    {
        super (characterEncoding);
    }

    /**
     * Disposes the parser.
     */
    public void dispose()
    {
        this.request = null;
        this.uploadData = null;
        super.dispose();
    }

    /**
     * Gets the parsed servlet request.
     *
     * @return the parsed servlet request or null.
     */
    public HttpServletRequest getRequest()
    {
        return request;
    }

    /**
     * Sets the servlet request to the parser.  This requires a
     * valid HttpServletRequest object.  It will attempt to parse out
     * the GET/POST/PATH_INFO data and store the data into a Map.
     * There are convenience methods for retrieving the data as a
     * number of different datatypes.  The PATH_INFO data must be a
     * URLEncoded() string.
     * <p>
     * To add name/value pairs to this set of parameters, use the
     * <code>add()</code> methods.
     *
     * @param request An HttpServletRequest.
     */
    public void setRequest(HttpServletRequest request)
    {
        clear();

        uploadData = null;

        String enc = request.getCharacterEncoding();
        setCharacterEncoding(enc != null
                ? enc
                : parameterEncoding);

        String contentType = request.getHeader("Content-type");

        if (uploadService != null
                && getAutomaticUpload()
                && contentType != null
                && contentType.startsWith("multipart/form-data"))
        {
            if (getLogger().isDebugEnabled())
            {
                getLogger().debug("Running the Fulcrum Upload Service");
            }

            try
            {
                List fileItems = uploadService.parseRequest(request);

                if (fileItems != null)
                {
                    for (Iterator it = fileItems.iterator(); it.hasNext();)
                    {
                        FileItem fi = (FileItem) it.next();
                        if (fi.isFormField())
                        {
                            getLogger().debug("Found an simple form field: " + fi.getFieldName() +", adding value " + fi.getString());

                            String value = null;
                            try
                            {
                                value = fi.getString(getCharacterEncoding());
                            }
                            catch (UnsupportedEncodingException e)
                            {
                                getLogger().error(getCharacterEncoding()
                                        + " encoding is not supported."
                                        + "Used the default when reading form data.");
                                value = fi.getString();
                            }
                            add(fi.getFieldName(), value);
                        }
                        else
                        {
                            getLogger().debug("Found an uploaded file: " + fi.getFieldName());
                            getLogger().debug("It has " + fi.getSize() + " Bytes and is " + (fi.isInMemory() ? "" : "not ") + "in Memory");
                            getLogger().debug("Adding FileItem as " + fi.getFieldName() + " to the params");
                            add(fi.getFieldName(), fi);
                        }
                    }
                }
            }
            catch (ServiceException e)
            {
                getLogger().error("File upload failed", e);
            }
        }

        for (Enumeration names = request.getParameterNames();
             names.hasMoreElements();)
        {
            String paramName = (String) names.nextElement();
            add(paramName,
                    request.getParameterValues(paramName));
        }

        // Also cache any pathinfo variables that are passed around as
        // if they are query string data.
        try
        {
            boolean isNameTok = true;
            String paramName = null;
            String paramValue = null;

            for ( StringTokenizer st =
                          new StringTokenizer(request.getPathInfo(), "/");
                  st.hasMoreTokens();)
            {
                if (isNameTok)
                {
                    paramName = URLDecoder.decode(st.nextToken(), getCharacterEncoding());
                    isNameTok = false;
                }
                else
                {
                    paramValue = URLDecoder.decode(st.nextToken(), getCharacterEncoding());
                    if (paramName.length() > 0)
                    {
                        add(paramName, paramValue);
                    }
                    isNameTok = true;
                }
            }
        }
        catch (Exception e)
        {
            // If anything goes wrong above, don't worry about it.
            // Chances are that the path info was wrong anyways and
            // things that depend on it being right will fail later
            // and should be caught later.
        }

        this.request = request;

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("Parameters found in the Request:");
            for (Iterator it = keySet().iterator(); it.hasNext();)
            {
                String key = (String) it.next();
                getLogger().debug("Key: " + key + " -> " + getString(key));
            }
        }
    }

    /**
     * Sets the uploadData byte[]
     *
     * @param uploadData A byte[] with data.
     */
    public void setUploadData ( byte[] uploadData )
    {
        this.uploadData = uploadData;
    }

    /**
     * Gets the uploadData byte[]
     *
     * @returns uploadData A byte[] with data.
     */
    public byte[] getUploadData ()
    {
        return this.uploadData;
    }


    /**
     * Add a FileItem object as a parameters.  If there are any
     * FileItems already associated with the name, append to the
     * array.  The reason for this is that RFC 1867 allows multiple
     * files to be associated with single HTML input element.
     *
     * @param name A String with the name.
     * @param value A FileItem with the value.
     * @deprecated Use add(String name, FileItem item)
     */
    public void append(String name, FileItem value)
    {
        add(name, value);
    }


    /**
     * Add a FileItem object as a parameters.  If there are any
     * FileItems already associated with the name, append to the
     * array.  The reason for this is that RFC 1867 allows multiple
     * files to be associated with single HTML input element.
     *
     * @param name A String with the name.
     * @param value A FileItem with the value.
     */
    public void add(String name, FileItem value)
    {
        FileItem[] items = this.getFileItems(name);
        items = (FileItem []) ArrayUtils.add(items, value);
        parameters.put(convert(name), items);
    }


    /**
     * Return a FileItem object for the given name.  If the name does
     * not exist or the object stored is not a FileItem, return null.
     *
     * @param name A String with the name.
     * @return A FileItem.
     */
    public FileItem getFileItem(String name)
    {
        try
        {
            FileItem value = null;
            Object object = parameters.get(convert(name));
            if (object != null)
                value = ((FileItem[])object)[0];
            return value;
        }
        catch ( ClassCastException e )
        {
            return null;
        }
    }

    /**
     * Return an array of FileItem objects for the given name.  If the
     * name does not exist or the object stored is not a FileItem
     * array, return null.
     *
     * @param name A String with the name.
     * @return A FileItem[].
     */
    public FileItem[] getFileItems(String name)
    {
        try
        {
            return (FileItem[])parameters.get(convert(name));
        }
        catch ( ClassCastException e )
        {
            return null;
        }
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
                throw new ServiceException(ParameterParser.ROLE,
                        AUTOMATIC_KEY + " = true requires " +
                        UploadService.ROLE + " to be available");
            }
        }
    }
}
