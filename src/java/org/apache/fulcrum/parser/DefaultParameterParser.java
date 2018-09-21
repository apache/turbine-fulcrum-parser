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
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.commons.lang3.ArrayUtils;

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
    implements ParameterParser
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
    @Override
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
    @Override
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
     * Sets the request character encoding to the parser. 
     * <p>
     * Sets the request encoding, if it is not set and {@link ParserService#getParameterEncoding()} 
     * is set to a non-default value {@link ParserService#PARAMETER_ENCODING_DEFAULT} 
     * (if {@link HttpServletRequest#getCharacterEncoding()} returns null, 
     * it has the default set to ISO-8859-1, cft. Servlet 2.4, 2.5, 3.0, 3.1 Specs).
     * This will only succeed, if no data was read yet, cft. spec.
     * <p>
     * To add name/value pairs to this set of parameters, use the
     * <code>add()</code> methods.
     *
     * @param request An HttpServletRequest.
     */
    @Override
    public void setRequest(HttpServletRequest request)
    {
        clear();

        uploadData = null;

        String enc = request.getCharacterEncoding();
        
        if (enc == null && !parserService.getParameterEncoding().equals(ParserService.PARAMETER_ENCODING_DEFAULT )) {
            try
            {  
                // no-op if data was read (parameter, POST..) 
                request.setCharacterEncoding( parserService.getParameterEncoding() );
                enc = request.getCharacterEncoding();
                if (enc != null) {
                    getLogger().debug("Set the request encoding successfully to parameterEncoding of parser: "+enc );
                } else {
                    getLogger().warn("Unsuccessfully (data read happened) tried to set the request encoding to "+ parserService.getParameterEncoding()  );
                }
            }
            catch ( UnsupportedEncodingException e )
            {
                getLogger().error("Found only unsupported encoding "+ e.getMessage());
            }
        }
        
        setCharacterEncoding(enc != null
                ? enc
                : parserService.getParameterEncoding());

        String contentType = request.getHeader("Content-type");

        if (parserService.getAutomaticUpload()
                && contentType != null
                && contentType.startsWith("multipart/form-data"))
        {
            try
            {
                List<Part> parts = parserService.parseUpload(request);

                if (parts != null)
                {
                    for (Part p : parts)
                    {
                        getLogger().debug("Found an uploaded file: " + p.getName());
                        getLogger().debug("It has " + p.getSize() + " Bytes");
                        getLogger().debug("Adding Part as " + p.getName() + " to the params");
                        add(p.getName(), p);
                    }
                }
            }
            catch (ServiceException e)
            {
                getLogger().error("File upload failed", e);
            }
        }

        for (Enumeration<?> names = request.getParameterNames();
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
                    if (paramName != null && paramName.length() > 0)
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
            for (String key : keySet())
            {
                getLogger().debug("Key: " + key + " -> " + getString(key));
            }
        }
    }

    /**
     * Sets the uploadData byte[]
     *
     * @param uploadData A byte[] with data.
     */
    @Override
    public void setUploadData ( byte[] uploadData )
    {
        this.uploadData = uploadData;
    }

    /**
     * Gets the uploadData byte[]
     *
     * @return uploadData A byte[] with data.
     */
    @Override
    public byte[] getUploadData ()
    {
        return this.uploadData;
    }

    /**
     * Add a Part object as a parameters.  If there are any
     * Parts already associated with the name, append to the
     * array.  The reason for this is that RFC 1867 allows multiple
     * files to be associated with single HTML input element.
     *
     * @param name A String with the name.
     * @param value A Part with the value.
     */
    @Override
    public void add( String name, Part value )
    {
        Part[] items = this.getParts(name);
        items = ArrayUtils.add(items, value);
        parameters.put(convert(name), items);
    }

    /**
     * Return a Part object for the given name.  If the name does
     * not exist or the object stored is not a Part, return null.
     *
     * @param name A String with the name.
     * @return A Part.
     */
    @Override
    public Part getPart(String name)
    {
        try
        {
            Part value = null;
            Object object = parameters.get(convert(name));
            if (object != null)
            {
                value = ((Part[])object)[0];
            }
            return value;
        }
        catch ( ClassCastException e )
        {
            return null;
        }
    }

    /**
     * Return an array of Part objects for the given name.  If the
     * name does not exist or the object stored is not a Part
     * array, return null.
     *
     * @param name A String with the name.
     * @return A Part[].
     */
    @Override
    public Part[] getParts(String name)
    {
        try
        {
            return (Part[])parameters.get(convert(name));
        }
        catch ( ClassCastException e )
        {
            return null;
        }
    }
    
    @Override
    public Collection<Part> getParts()
    {
        return parameters.values().stream().
                            filter( p-> p instanceof Part[]).
                            flatMap(c -> Arrays.stream( (Part[]) c )).
                            collect( Collectors.toList() );

    }
}
