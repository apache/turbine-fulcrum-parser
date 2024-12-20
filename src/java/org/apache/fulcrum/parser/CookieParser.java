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


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * CookieParser is an interface to a utility to to get and set values
 * of Cookies on the Client Browser. You can use CookieParser to convert
 * Cookie values to various types or to set Bean values with setParameters().
 * Servlet Spec for more information on Cookies.
 * <p>
 * Use set() or unset() to Create or Destroy Cookies.
 * <p>
 * NOTE: The name= portion of a name=value pair may be converted
 * to lowercase or uppercase when the object is initialized and when
 * new data is added.  This behaviour is determined by the url.case.folding
 * property in TurbineResources.properties.  Adding a name/value pair may
 * overwrite existing name=value pairs if the names match:
 *
 * <pre>
 * CookieParser cp = data.getCookies();
 * cp.add("ERROR",1);
 * cp.add("eRrOr",2);
 * int result = cp.getInt("ERROR");
 * </pre>
 *
 * In the above example, result is 2.
 *
 * @author <a href="mailto:ilkka.priha@simsoft.fi">Ilkka Priha</a>
 * @author <a href="mailto:leon@opticode.co.za">Leon Messerschmidt</a>
 * @author <a href="mailto:tv@apache.org">Thomas Vandahl</a>
 * @version $Id$
 */
public interface CookieParser
    extends ValueParser
{
    static final int AGE_SESSION = -1;
    static final int AGE_DELETE = 0;

    /**
     * Gets the servlet request.
     *
     * @return the servlet request object or null.
     */
    HttpServletRequest getRequest();

    /**
     * Sets the servlet request and response to be parsed.
     * All previous cookies will be cleared.
     *
     * @param request the servlet request object.
     * @param response the servlet response object
     */
    void setData (HttpServletRequest request,
                  HttpServletResponse response);

    /**
     * Set a cookie that will be stored on the client for
     * the duration of the session.
     * 
     * @param name The name of the cookie
     * @param value The value of the cooking
     */
    void set (String name, String value);

    /**
     * Set a persistent cookie on the client that will expire
     * after a maximum age (given in seconds).
     * 
     * @param name A String for the name 
     * @param value  A String for the value
     * @param seconds_age An int for the age in seconds
     */
    void set (String name, String value, int seconds_age);

    /**
     * Remove a previously set cookie from the client machine.
     * 
     * @param name the name of the cooking to unset
     */
    void unset (String name);

	boolean isValid();
}
