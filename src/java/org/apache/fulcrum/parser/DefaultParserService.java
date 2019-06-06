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
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.fulcrum.parser.ValueParser.URLCaseFolding;
import org.apache.fulcrum.parser.pool.BaseValueParserFactory;
import org.apache.fulcrum.parser.pool.BaseValueParserPool;
import org.apache.fulcrum.parser.pool.CookieParserFactory;
import org.apache.fulcrum.parser.pool.CookieParserPool;
import org.apache.fulcrum.parser.pool.DefaultParameterParserFactory;
import org.apache.fulcrum.parser.pool.DefaultParameterParserPool;


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
     * Use commons pool to manage value parsers 
     */
    private BaseValueParserPool valueParserPool;

    /** 
     * Use commons pool to manage parameter parsers 
     */
    private DefaultParameterParserPool parameterParserPool;

    /** 
     * Use commons pool to manage cookie parsers 
     */
    private CookieParserPool cookieParserPool;

    public DefaultParserService() 
    {
		// Define the default configuration
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		config.setMaxIdle(DEFAULT_MAX_IDLE);
	    config.setMaxTotal(DEFAULT_POOL_CAPACITY);

	    // init the pool
	    valueParserPool 
    		= new BaseValueParserPool(new BaseValueParserFactory(), config);

	    // init the pool
	    parameterParserPool 
	    	= new DefaultParameterParserPool(new DefaultParameterParserFactory(), config);
	    
	    // init the pool
	    cookieParserPool 
	    	= new CookieParserPool(new CookieParserFactory(), config);
    }
    
    public DefaultParserService(GenericObjectPoolConfig<?> config) 
    {
	    // init the pool
	    valueParserPool 
    		= new BaseValueParserPool(new BaseValueParserFactory(), config);

	    parameterParserPool 
	    	= new DefaultParameterParserPool(new DefaultParameterParserFactory(), config);
    }

    
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
    @SuppressWarnings("unchecked")
	@Override
    public <P extends ValueParser> P getParser(Class<P> ppClass) throws InstantiationException
    {
        P vp = null;

        try
        {
            if ( ppClass.equals(BaseValueParser.class) )
            {
            	BaseValueParser parserInstance;
				try {
					parserInstance = valueParserPool.borrowObject();
					vp = (P) parserInstance;
				} catch (Exception e) {
				}
            }

            if ( ppClass.equals(DefaultParameterParser.class) )
            {
				try {
	            	DefaultParameterParser parserInstance = parameterParserPool.borrowObject();
	            	vp = (P) parserInstance;
				} catch (Exception e) {
				}
            }
            
            if ( ppClass.equals(DefaultCookieParser.class) )
            {
				try {
					DefaultCookieParser parserInstance = cookieParserPool.borrowObject();
	            	vp = (P) parserInstance;
				} catch (Exception e) {
				}
            }
            
            
            
            ((ParserServiceSupport)vp).setParserService(this);
            ((LogEnabled)vp).enableLogging(getLogger().getChildLogger(ppClass.getSimpleName()));
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
        
        if ( parser.getClass().isInstance(BaseValueParser.class) )
        {
			valueParserPool.returnObject( (BaseValueParser) parser );
			
        } else if ( parser.getClass().isInstance(DefaultParameterParser.class) ||
                parser instanceof DefaultParameterParser)
        {
        	parameterParserPool.returnObject( (DefaultParameterParser) parser );
        	
        } else if ( parser.getClass().isInstance(DefaultCookieParser.class) ||
                parser instanceof DefaultCookieParser)
        {
        	cookieParserPool.returnObject( (DefaultCookieParser) parser );
        	
        } else {
            // log
            getLogger().warn(parser.getClass() + " could not be put back into any pool exhausting some pool");
            // log even borrowed count of each pool?: cookieParserPool.getBorrowedCount())
        }
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
       
        
        Configuration[] poolChildren = conf.getChild(POOL_KEY).getChildren();
        if (poolChildren.length > 0) {
            GenericObjectPoolConfig genObjPoolConfig = new GenericObjectPoolConfig();
            genObjPoolConfig.setMaxIdle(DEFAULT_MAX_IDLE);
            genObjPoolConfig.setMaxTotal(DEFAULT_POOL_CAPACITY);
            for (Configuration poolConf : poolChildren) {
                // use common pool2 configuration names
                switch (poolConf.getName()) {
                case "maxTotal":
                    int defaultCapacity = poolConf.getValueAsInteger();
                    genObjPoolConfig.setMaxTotal(defaultCapacity);
                    break;
                case "maxWaitMillis":
                    int maxWaitMillis = poolConf.getValueAsInteger();
                    genObjPoolConfig.setMaxWaitMillis(maxWaitMillis);
                    break;
                case "blockWhenExhausted":
                    boolean blockWhenExhausted = poolConf.getValueAsBoolean();
                    genObjPoolConfig.setBlockWhenExhausted(blockWhenExhausted);
                    break;
                case "maxIdle":
                    int maxIdle = poolConf.getValueAsInteger();
                    genObjPoolConfig.setMaxIdle(maxIdle);
                    break;
                case "minIdle":
                    int minIdle = poolConf.getValueAsInteger();
                    genObjPoolConfig.setMinIdle(minIdle);
                    break;
                default:
                    break;
                }  
            }    
            // reinit the pools
            valueParserPool.setConfig(genObjPoolConfig);
            parameterParserPool.setConfig(genObjPoolConfig);
            cookieParserPool.setConfig(genObjPoolConfig);
        }
        
        getLogger().debug(valueParserPool.toString());
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
        // no need to call internal service
//        if (manager.hasService(PoolService.ROLE))
//        {
//            poolService = (PoolService)manager.lookup(PoolService.ROLE);
//        }
//        else
//        {
//            throw new ServiceException(ParserService.ROLE,
//                    "Service requires " +
//                    PoolService.ROLE + " to be available");
//        }
    }
}
