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


import java.net.URLDecoder;
import java.util.StringTokenizer;

/**
 * An extension that parses a String for name/value pairs.
 *
 * @author <a href="mailto:jmcnally@collab.net">John McNally</a>
 * @version $Id$
 */
public class StringValueParser
    extends BaseValueParser
{
    public StringValueParser() {}

    /**
     * Parses a String using a single delimiter.
     *
     * @param s a <code>String</code> value
     * @param delim a <code>char</code> value
     * @param urlDecode a <code>boolean</code> value
     * @exception Exception Error decoding name/value pairs.
     */
    public void parse(String s, char delim, boolean urlDecode)
        throws Exception
    {
        String delimChar = String.valueOf(delim);
        StringTokenizer st = new StringTokenizer(s, delimChar);
        boolean isNameTok = true;
        String pathPart = null;
        String key = null;
        while (st.hasMoreTokens())
        {
            String tok = st.nextToken();
            if ( urlDecode )
            {
                tok = URLDecoder.decode(tok, getCharacterEncoding());
            }

            if (isNameTok)
            {
                key = tok;
                isNameTok = false;
            }
            else
            {
                pathPart = tok;
                if (key.length() > 0)
                {
                    add (convert(key), pathPart);
                }
                isNameTok = true;
            }
        }
    }

    public void parse(String s, char paramDelim, char pairDelim,
                      boolean urlDecode)
        throws Exception
    {
        if ( paramDelim == pairDelim )
        {
            parse(s, paramDelim, urlDecode);
        }
        else
        {
            String delimChar = String.valueOf(paramDelim);
            StringTokenizer st = new StringTokenizer(s, delimChar);

            while (st.hasMoreTokens())
            {
                String pair = st.nextToken();
                int pos = pair.indexOf(pairDelim);
                String name = pair.substring(0, pos);
                String value = pair.substring(pos+1);

                if ( urlDecode )
                {
                    name = URLDecoder.decode(name, getCharacterEncoding());
                    value = URLDecoder.decode(value, getCharacterEncoding());
                }

                if (name.length() > 0)
                {
                    add (convert(name), value);
                }
            }
        }
    }
}
