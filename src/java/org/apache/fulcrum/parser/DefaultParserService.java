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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.fulcrum.parser.ValueParser.URLCaseFolding;
import org.apache.fulcrum.parser.pool.BaseValueParserFactory;
import org.apache.fulcrum.parser.pool.BaseValueParserPool;
import org.apache.fulcrum.parser.pool.CookieParserFactory;
import org.apache.fulcrum.parser.pool.CookieParserPool;
import org.apache.fulcrum.parser.pool.DefaultParameterParserFactory;
import org.apache.fulcrum.parser.pool.DefaultParameterParserPool;
import org.apache.fulcrum.pool.PoolException;
import org.apache.fulcrum.pool.PoolService;


/**
 * The DefaultParserService provides the default implementation
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
     * reintroduced fulcrum, may be used in the case, that 
     * commons pool2 is not configured exactly as needed properly as fast fall back.
     */
    private boolean useFulcrumPool = FULCRUM_POOL_DEFAULT;
    
    
    /**
     * The Fulcrum pool service component to use (optional), by default it is deactivated and commons pool is used.
     */
    private PoolService fulcrumPoolService = null;

    /** 
     * Use commons pool to manage value parsers 
     */
    private BaseValueParserPool valueParserPool;

    /**
     * Use commons pool to manage parameter parsers
     */
    private DefaultParameterParserPool parameterParserPool;

    /**
     * The class instantiated for requests naming the base
     * {@code DefaultParameterParser} class, as configured via
     * {@link ParserService#PARAMETER_PARSER_CLASS_KEY}. Defaults to
     * {@code DefaultParameterParser} itself.
     */
    private Class<? extends DefaultParameterParser> parameterParserClass = DefaultParameterParser.class;

    /** 
     * Use commons pool to manage cookie parsers 
     */
    private CookieParserPool cookieParserPool;


    public DefaultParserService()
    {
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
     * Set the character encoding that will be used by this ValueParser.
     */
    public void setParameterEncoding(String encoding)
    {
        parameterEncoding = encoding;
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
     * @return A list of {@link jakarta.servlet.http.Part}s
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
            if (useFulcrumPool) {
                try
                {
                    P parserInstance = (P) fulcrumPoolService.getInstance(ppClass);
                    vp = parserInstance;
                }
                catch (PoolException pe)
                {
                    throw new InstantiationException("Parser class '" + ppClass + "' is illegal. " + pe.getMessage());
                }
            } else if ( ppClass.equals(BaseValueParser.class) )
            {
                BaseValueParser parserInstance = null;
                try {
                    parserInstance = valueParserPool.borrowObject();
                	vp = (P) parserInstance;
                	if (vp == null) {
                        throw new InstantiationException("Could not borrow object from pool: " + valueParserPool);
                    }
                } catch (Exception e) {
                    try {
                        valueParserPool.invalidateObject(parserInstance);
                        parserInstance = null;
                    } catch (Exception e1) {
                        throw new InstantiationException("Could not invalidate object " + e1.getMessage() + " after exception: " + e.getMessage());
                    }
                }
            } else if ( ppClass.equals(DefaultParameterParser.class) || DefaultParameterParser.class.isAssignableFrom(ppClass) )
            {
                DefaultParameterParser parserInstance = null;
                // Requests naming the base class defer to the configured default
                // implementation (PARAMETER_PARSER_CLASS_KEY); explicit subclass
                // requests are keyed by that subclass itself. Resolving this once,
                // up front, keeps the borrow key and the actually instantiated
                // class in lockstep, since putParser() later returns the object
                // to the pool keyed by parser.getClass().
                Class<? extends DefaultParameterParser> keyClass = ppClass.equals(DefaultParameterParser.class)
                        ? parameterParserClass : ppClass.asSubclass(DefaultParameterParser.class);
                try {
                    // Keyed by the requested class itself, so a configured subclass
                    // is genuinely instantiated and pooled, not silently replaced by
                    // the default class (this is the actual bug fix.
                    parserInstance = parameterParserPool.borrowObject(keyClass);
                	vp = (P) parserInstance;
                	if (vp == null) {
                        throw new InstantiationException("Could not borrow object from pool: " + parameterParserPool);
                    }
                } catch (Exception e) {
                    if (parserInstance == null) {
                        // borrowObject/create(key) itself failed -- there is nothing
                        // to invalidate, and we must not lose the class name behind a
                        // secondary invalidateObject(key, null) failure.
                        throw new InstantiationException("Could not instantiate parameter parser class "
                                + ppClass.getName() + ": " + e.getMessage());
                    }
                    try {
                        parameterParserPool.invalidateObject(keyClass, parserInstance);
                        parserInstance = null;
                    } catch (Exception e1) {
                        throw new InstantiationException("Could not invalidate object " + e1.getMessage()
                                + " after exception on parameter parser class " + ppClass.getName() + ": " + e.getMessage());
                    }
                }
            } else if ( ppClass.equals(DefaultCookieParser.class) || DefaultCookieParser.class.isAssignableFrom(ppClass) )
            {
                DefaultCookieParser parserInstance = null;
                try {
                    parserInstance = cookieParserPool.borrowObject();
                	vp = (P) parserInstance;
                    if (vp == null) {
                          throw new InstantiationException("Could not borrow object from pool: " + cookieParserPool);
                    }
                } catch (Exception e) {
                    try {
                        cookieParserPool.invalidateObject(parserInstance);
                        parserInstance = null;
                    } catch (Exception e1) {
                        throw new InstantiationException("Could not invalidate object " + e1.getMessage() + " after exception: " + e.getMessage());
                    }
                }
            }
            
            if (vp != null && vp instanceof ParserServiceSupport ) {
                ((ParserServiceSupport)vp).setParserService(this);
            } else {
                throw new InstantiationException("Could not set parser");
            }
            if (vp instanceof LogEnabled) 
            {
                ((LogEnabled)vp).enableLogging(getLogger().getChildLogger(ppClass.getSimpleName()));
            }
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
     * As we are not yet using org.apache.fulcrum.pool.Recyclable, we call insteda {@link ValueParser#dispose()}.
     *
     * @param parser The value parser to use
     */
    @Override
    public void putParser(ValueParser parser)
    {
        parser.clear();
        parser.dispose(); 
    
        if (useFulcrumPool) {
            
            fulcrumPoolService.putInstance(parser);
            
        } else if( parser.getClass().equals(BaseValueParser.class) )
        {
            valueParserPool.returnObject( (BaseValueParser) parser );
        
        } else if ( parser.getClass().equals(DefaultParameterParser.class) ||
                parser instanceof DefaultParameterParser)
        {
            DefaultParameterParser parameterParser = (DefaultParameterParser) parser;
            parameterParserPool.returnObject( parameterParser.getClass(), parameterParser );

        } else if ( parser.getClass().equals(DefaultCookieParser.class) ||
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

        // The class instantiated for requests naming the base DefaultParameterParser
        // class. Defaults to DefaultParameterParser itself; explicit requests for a
        // specific subclass are unaffected by this setting.
        parameterParserClass = DefaultParameterParser.class;
        String parameterParserClassName = conf.getChild(PARAMETER_PARSER_CLASS_KEY).getValue(null);
        if (StringUtils.isNotEmpty(parameterParserClassName))
        {
            try
            {
                parameterParserClass = Class.forName(parameterParserClassName).asSubclass(DefaultParameterParser.class);
            }
            catch (ClassNotFoundException | ClassCastException e)
            {
                throw new ConfigurationException("Could not load " + PARAMETER_PARSER_CLASS_KEY
                        + " '" + parameterParserClassName + "': " + e.getMessage(), e);
            }
        }

        useFulcrumPool = conf.getChild(FULCRUM_POOL_KEY).getValueAsBoolean(FULCRUM_POOL_DEFAULT);
        
        if (useFulcrumPool) {
            if (fulcrumPoolService == null) 
            {
                    // only for fulcrum  pool, need to call internal service, if role pool service is set
                    throw new ConfigurationException("Fulcrum Pool is activated", new ServiceException(ParserService.ROLE,
                            "Fulcrum enabled Pool Service requires " +
                            PoolService.ROLE + " to be available"));
            }
            getLogger().info("Using Fulcrum Pool Service: "+ fulcrumPoolService);
        } else {
            // reset not used fulcrum pool
            fulcrumPoolService = null;
            
            // Define the default configuration
            GenericObjectPoolConfig config = new GenericObjectPoolConfig();
            config.setMaxIdle(DEFAULT_MAX_IDLE);
            config.setMaxTotal(DEFAULT_POOL_CAPACITY);

            // Parameter-parser pool is keyed by the requested class (may be a
            // configured subclass), so it needs its own keyed config -- a plain
            // GenericObjectPoolConfig cannot be used with a GenericKeyedObjectPool.
            // maxTotalPerKey is set equal to maxTotal so the default (single-class)
            // case retains today's effective capacity unchanged (research.md
            // Decision 5) instead of silently shrinking to Commons Pool2's
            // per-key default of 8.
            GenericKeyedObjectPoolConfig<DefaultParameterParser> keyedConfig = new GenericKeyedObjectPoolConfig<>();
            keyedConfig.setMaxIdlePerKey(DEFAULT_MAX_IDLE);
            keyedConfig.setMaxTotal(DEFAULT_POOL_CAPACITY);
            keyedConfig.setMaxTotalPerKey(DEFAULT_POOL_CAPACITY);

            // init the pool
            valueParserPool
                = new BaseValueParserPool(new BaseValueParserFactory(), config);

            // init the pool
            parameterParserPool
                = new DefaultParameterParserPool(new DefaultParameterParserFactory(parameterParserClass), keyedConfig);

            // init the pool
            cookieParserPool
                = new CookieParserPool(new CookieParserFactory(), config);
            
            getLogger().info("Init Commons2 Pool Services.." );
            getLogger().info(valueParserPool.getClass().getName());
            getLogger().info(parameterParserPool.getClass().getName());
            getLogger().info(cookieParserPool.getClass().getName());
        }
        
        Configuration[] poolChildren = conf.getChild(POOL_KEY).getChildren();
        if (poolChildren.length > 0) {
            GenericObjectPoolConfig genObjPoolConfig = new GenericObjectPoolConfig();
            genObjPoolConfig.setMaxIdle(DEFAULT_MAX_IDLE);
            genObjPoolConfig.setMaxTotal(DEFAULT_POOL_CAPACITY);

            // Parallel keyed config for the parameter-parser pool, built from the
            // same <pool> XML children as genObjPoolConfig above, so a single
            // <pool> block continues to tune all three parser pools uniformly.
            GenericKeyedObjectPoolConfig<DefaultParameterParser> genKeyedObjPoolConfig = new GenericKeyedObjectPoolConfig<>();
            genKeyedObjPoolConfig.setMaxIdlePerKey(DEFAULT_MAX_IDLE);
            genKeyedObjPoolConfig.setMaxTotal(DEFAULT_POOL_CAPACITY);
            genKeyedObjPoolConfig.setMaxTotalPerKey(DEFAULT_POOL_CAPACITY);

            for (Configuration poolConf : poolChildren) {
                // use common pool2 configuration names
                switch (poolConf.getName()) {
                case "maxTotal":
                    int defaultCapacity = poolConf.getValueAsInteger();
                    genObjPoolConfig.setMaxTotal(defaultCapacity);
                    genKeyedObjPoolConfig.setMaxTotal(defaultCapacity);
                    // research.md Decision 5: mirror maxTotal into maxTotalPerKey so the
                    // default (single-class) case keeps today's effective capacity instead
                    // of silently shrinking to Commons Pool2's per-key default of 8.
                    genKeyedObjPoolConfig.setMaxTotalPerKey(defaultCapacity);
                    break;
                case "maxWaitMillis":
                    int maxWaitMillis = poolConf.getValueAsInteger();
                    Duration maxWaitMillisDuration = Duration.ofMillis( maxWaitMillis );
                    genObjPoolConfig.setMaxWait(maxWaitMillisDuration);
                    genKeyedObjPoolConfig.setMaxWait(maxWaitMillisDuration);
                    break;
                case "blockWhenExhausted":
                    boolean blockWhenExhausted = poolConf.getValueAsBoolean();
                    genObjPoolConfig.setBlockWhenExhausted(blockWhenExhausted);
                    genKeyedObjPoolConfig.setBlockWhenExhausted(blockWhenExhausted);
                    break;
                case "maxIdle":
                    int maxIdle = poolConf.getValueAsInteger();
                    genObjPoolConfig.setMaxIdle(maxIdle);
                    genKeyedObjPoolConfig.setMaxIdlePerKey(maxIdle);
                    break;
                case "minIdle":
                    int minIdle = poolConf.getValueAsInteger();
                    genObjPoolConfig.setMinIdle(minIdle);
                    genKeyedObjPoolConfig.setMinIdlePerKey(minIdle);
                    break;
                case "testOnReturn":
                    boolean testOnReturn = poolConf.getValueAsBoolean();
                    genObjPoolConfig.setTestOnReturn(testOnReturn);
                    genKeyedObjPoolConfig.setTestOnReturn(testOnReturn);
                    break;
                case "testOnBorrow":
                    boolean testOnBorrow = poolConf.getValueAsBoolean();
                    genObjPoolConfig.setTestOnBorrow(testOnBorrow);
                    genKeyedObjPoolConfig.setTestOnBorrow(testOnBorrow);
                    break;
                case "testOnCreate":
                    boolean testOnCreate = poolConf.getValueAsBoolean();
                    genObjPoolConfig.setTestOnCreate(testOnCreate);
                    genKeyedObjPoolConfig.setTestOnCreate(testOnCreate);
                    break;
                default:

                    break;
                }
            }

            if (!genObjPoolConfig.getBlockWhenExhausted() && !genObjPoolConfig.getMaxWaitDuration().isZero()) {
                getLogger().warn( "maxWaitMillis will only be applied, if blockWhenExhausted is set. maxWait: "
                  + genObjPoolConfig.getMaxWaitDuration() );
            }
            // reinit the pools
            valueParserPool.setConfig(genObjPoolConfig);
            parameterParserPool.setConfig(genKeyedObjPoolConfig);
            cookieParserPool.setConfig(genObjPoolConfig);

            getLogger().info("Update Config Commons2 Pools with " + genObjPoolConfig );
            getLogger().debug(valueParserPool.toString());
            getLogger().debug(parameterParserPool.toString());
            getLogger().debug(cookieParserPool.toString());
        }
                
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
        // only for fulcrum  pool, need to call internal service, if role pool service is set
        if (manager.hasService(PoolService.ROLE))
        {
            fulcrumPoolService = (PoolService)manager.lookup(PoolService.ROLE);
        } 
        // no check here, until configuration is read
    }
}
