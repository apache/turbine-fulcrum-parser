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

import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.fulcrum.parser.DefaultCookieParser;

/**
 * Pool manager for {@link org.apache.fulcrum.parser.BaseValueParser} objects
 *
 * @author <a href="mailto:painter@apache.org">Jeffery Painter</a>
 * @version $Id: CookieParserPool.java 1851080 2019-01-16 12:07:00Z painter $
 */
public class CookieParserPool extends GenericObjectPool<DefaultCookieParser> 
{

	/**
	 * Constructor.
	 * 
	 * @param factory the factory
	 */
	public CookieParserPool(PooledObjectFactory<DefaultCookieParser> factory) 
	{
		super(factory);
	}

	/**
	 * Constructor.
	 * 
	 * This can be used to have full control over the pool using configuration
	 * object.
	 * 
	 * @param factory the factory
	 * @param config user defined configuration
	 */
	public CookieParserPool(PooledObjectFactory<DefaultCookieParser> factory, GenericObjectPoolConfig config) 
	{
		super(factory, config);
	}

}
