package org.apache.fulcrum.parser.pool;

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

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.fulcrum.parser.DefaultParameterParser;


/**
 * Factory to create {@link org.apache.fulcrum.parser.DefaultParameterParser} objects,
 * or instances of a configured subclass thereof. Keyed by the requested class so that
 * different callers can request different {@code DefaultParameterParser} subclasses
 * from the same pool (see {@code DefaultParameterParserPool}).
 * <p>
 * The class actually instantiated when the base {@code DefaultParameterParser}
 * class is requested (rather than an explicit subclass) can be configured via
 * {@link #DefaultParameterParserFactory(Class)}, so that callers which ask for
 * the base class transparently receive a configured subclass instead.
 *
 * @author <a href="mailto:painter@apache.org">Jeffery Painter</a>
 * @version $Id: DefaultParameterParserFactory.java 1851080 2019-01-16 12:07:00Z painter $
 */
public class DefaultParameterParserFactory
	extends BaseKeyedPooledObjectFactory<Class<? extends DefaultParameterParser>, DefaultParameterParser>
{
	/** The class instantiated for requests naming the base {@code DefaultParameterParser} class. */
	private final Class<? extends DefaultParameterParser> defaultParserClass;

	/**
	 * Creates a factory that instantiates the plain {@code DefaultParameterParser}
	 * class for base-class requests.
	 */
	public DefaultParameterParserFactory()
	{
		this(DefaultParameterParser.class);
	}

	/**
	 * Creates a factory that instantiates {@code defaultParserClass} whenever the
	 * base {@code DefaultParameterParser} class is requested. Requests naming a
	 * specific subclass are unaffected and always instantiate that subclass.
	 *
	 * @param defaultParserClass the {@code DefaultParameterParser} subclass to use
	 * for base-class requests; must not be {@code null}
	 */
	public DefaultParameterParserFactory(Class<? extends DefaultParameterParser> defaultParserClass)
	{
		this.defaultParserClass = Objects.requireNonNull(defaultParserClass, "defaultParserClass must not be null");
	}

	/**
	 * @return the class instantiated for requests naming the base
	 * {@code DefaultParameterParser} class
	 */
	public Class<? extends DefaultParameterParser> getDefaultParserClass()
	{
		return defaultParserClass;
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool2.BaseKeyedPooledObjectFactory#create(java.lang.Object)
	 */
	@Override
	public DefaultParameterParser create(Class<? extends DefaultParameterParser> key) throws Exception
	{
		Class<? extends DefaultParameterParser> classToInstantiate =
				(key == null || key.equals(DefaultParameterParser.class)) ? defaultParserClass : key;

		try
		{
			return classToInstantiate.getDeclaredConstructor().newInstance();
		}
		catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e)
		{
			throw new InstantiationException("Could not instantiate parameter parser class "
					+ classToInstantiate.getName() + ": " + e.getMessage());
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.commons.pool2.BaseKeyedPooledObjectFactory#wrap(java.lang.Object)
	 */
	@Override
	public PooledObject<DefaultParameterParser> wrap(DefaultParameterParser obj)
	{
		return new DefaultPooledObject<DefaultParameterParser>(obj);
	}

   /**
     * When an object is returned to the pool, clear the buffer.
     */
    @Override
    public void passivateObject(Class<? extends DefaultParameterParser> key, PooledObject<DefaultParameterParser> pooledObject)
    {
        pooledObject.getObject().clear();
    }

    /* (non-Javadoc)
     * @see org.apache.commons.pool2.BaseKeyedPooledObjectFactory#validateObject(java.lang.Object, org.apache.commons.pool2.PooledObject)
     */
    @Override
    public boolean validateObject(Class<? extends DefaultParameterParser> key, PooledObject<DefaultParameterParser> parser)
    {
        return parser.getObject().isValid();
    }

}
