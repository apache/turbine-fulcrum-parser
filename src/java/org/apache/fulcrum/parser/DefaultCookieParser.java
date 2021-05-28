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


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * CookieParser is used to get and set values of Cookies on the Client
 * Browser.  You can use CookieParser to convert Cookie values to
 * various types or to set Bean values with setParameters(). See the
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
public class DefaultCookieParser
    extends BaseValueParser
    implements CookieParser

{
    /**
     * The servlet request objects to parse.
     */
    private HttpServletRequest request;
    private HttpServletResponse response;

    /**
     * Constructs a new CookieParser.
     */
    public DefaultCookieParser()
    {
        super();
    }

    /**
     * Disposes the parser.
     */
    public void dispose()
    {
        this.request = null;
        this.response = null;
        super.dispose();
    }

    /**
     * Gets the servlet request.
     *
     * @return the servlet request object or null.
     */
    public HttpServletRequest getRequest()
    {
        return this.request;
    }

    /**
     * Sets the servlet request and response to be parsed.
     * All previous cookies will be cleared.
     *
     * @param request the servlet request object.
     * @param response the servlet response object
     */
    public void setData (HttpServletRequest request,
                         HttpServletResponse response)
    {
        clear();

        String enc = request.getCharacterEncoding();
        setCharacterEncoding(enc != null ? enc : "US-ASCII");

        Cookie[] cookies = request.getCookies();
        if ( cookies != null )
        {
	        getLogger().debug ("Number of Cookies "+cookies.length);
	
	        for (Cookie cookie : cookies)
	        {
	            String name = convert(cookie.getName());
	            String value = cookie.getValue();
	            getLogger().debug ("Adding " + name + "=" + value);
	            add(name, value);
	        }
        }

        this.request = request;
        this.response = response;
    }

    /**
     * Set a cookie that will be stored on the client for
     * the duration of the session.
     */
    public void set (String name, String value)
    {
        set(name, value, AGE_SESSION);
    }

    /* (non-Javadoc)
     * @see org.apache.fulcrum.parser.CookieParser#set(java.lang.String, java.lang.String, int)
	 *
     * Set a persistent cookie on the client that will expire
     * after a maximum age (given in seconds).
     */
    public void set(String name, String value, int secondsAge)
    {
        if (response == null)
        {
            throw new IllegalStateException("Servlet response not available");
        }

        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(secondsAge);
        cookie.setPath(request.getServletPath());
        response.addCookie(cookie);
    }
    

    /* (non-Javadoc)
     * @see org.apache.fulcrum.parser.CookieParser#unset(java.lang.String)
     * 
     * Remove a previously set cookie from the client machine.
     * 
     */
    public void unset(String name)
    {
        set(name, " ", AGE_DELETE);
    }
    
    /* (non-Javadoc)
     * @see org.apache.fulcrum.parser.BaseValueParser#isValid()
     */
    public boolean isValid() 
    {
    	if ( this.parameters.size() == 0 )
    	{
    		return true;
    	}
    	return false;
    }

}
