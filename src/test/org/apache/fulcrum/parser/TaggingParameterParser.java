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

/**
 * Test fixture proving that {@link ParserService#getParser(Class)} actually
 * instantiates and returns a configured {@link DefaultParameterParser} subclass,
 * instead of always returning the pooled default class.
 */
public class TaggingParameterParser extends DefaultParameterParser
{
    /** Marker appended by {@link #getString(String)} so tests can detect this class ran */
    public static final String TAG = "tagged-by-custom-parser";

    @Override
    public String getString(String name)
    {
        String value = super.getString(name);
        return value == null ? null : value + "[" + TAG + "]";
    }
}
