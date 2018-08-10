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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Part;

/**
 * ParameterParser is an interface to a utility to handle parsing and
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
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public interface ParameterParser
    extends ValueParser
{
    /**
     * Gets the parsed servlet request.
     *
     * @return the parsed servlet request or null.
     */
    HttpServletRequest getRequest();

    /**
     * Sets the servlet request to be parser.  This requires a
     * valid HttpServletRequest object.  It will attempt to parse out
     * the GET/POST/PATH_INFO data and store the data into a Hashtable.
     * There are convenience methods for retrieving the data as a
     * number of different datatypes.  The PATH_INFO data must be a
     * URLEncoded() string.
     *
     * <p>To add name/value pairs to this set of parameters, use the
     * <code>add()</code> methods.
     *
     * @param req An HttpServletRequest.
     */
    void setRequest(HttpServletRequest req);

    /**
     * Sets the uploadData byte[]
     *
     * @param uploadData A byte[] with data.
     */
    void setUploadData ( byte[] uploadData );

    /**
     * Gets the uploadData byte[]
     *
     * @return uploadData A byte[] with data.
     */
    byte[] getUploadData();


    /**
     * Add a Part object as a parameters.  If there are any
     * Parts already associated with the name, append to the
     * array.  The reason for this is that RFC 1867 allows multiple
     * files to be associated with single HTML input element.
     *
     * @param name A String with the name.
     * @param value A Part with the value.
     */
    void add( String name, Part value );

    /**
     * Return a Part object for the given name.  If the name does
     * not exist or the object stored is not a Part, return null.
     *
     * @param name A String with the name.
     * @return A Part.
     */
    Part getPart(String name);

    /**
     * Return an array of Part objects for the given name.  If the
     * name does not exist or the object stored is not a Part
     * array, return null.
     *
     * @param name A String with the name.
     * @return A Part[].
     */
    Part[] getParts(String name);
    
    /**
     * Return an array of all Part objects.  If no parts
     * exist or the object stored is not a Part
     * array, return an empty list.

     * @return Collection<Part>
     */
    Collection<Part> getParts();
}
