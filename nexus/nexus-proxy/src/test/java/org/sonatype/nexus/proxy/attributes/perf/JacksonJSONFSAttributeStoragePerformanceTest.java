/*
 * Copyright (c) 2008-2011 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions
 *
 * This program is free software: you can redistribute it and/or modify it only under the terms of the GNU Affero General
 * Public License Version 3 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Affero General Public License Version 3
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License Version 3 along with this program.  If not, see
 * http://www.gnu.org/licenses.
 *
 * Sonatype Nexus (TM) Open Source Version is available from Sonatype, Inc. Sonatype and Sonatype Nexus are trademarks of
 * Sonatype, Inc. Apache Maven is a trademark of the Apache Foundation. M2Eclipse is a trademark of the Eclipse Foundation.
 * All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.proxy.attributes.perf;

import com.carrotsearch.junitbenchmarks.BenchmarkRule;
import com.carrotsearch.junitbenchmarks.annotation.AxisRange;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;
import org.junit.Rule;
import org.junit.rules.MethodRule;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.attributes.AttributeStorage;
import org.sonatype.nexus.proxy.attributes.JacksonJSONFSAttributeStorage;
import org.sonatype.nexus.proxy.attributes.JacksonXMLFSAttributeStorage;
import org.sonatype.plexus.appevents.ApplicationEventMulticaster;

import java.io.File;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Performance test for DefaultFSAttributeStorage
 */
//@BenchmarkHistoryChart()
//@BenchmarkMethodChart()
//@AxisRange(min = 0)
public class JacksonJSONFSAttributeStoragePerformanceTest
    extends AttributeStoragePerformanceTestSupport
{

//    @Rule
//    public MethodRule benchmarkRun = new BenchmarkRule();
    
    public AttributeStorage getAttributeStorage()
    {
        ApplicationEventMulticaster applicationEventMulticaster = mock( ApplicationEventMulticaster.class );
        ApplicationConfiguration applicationConfiguration = mock( ApplicationConfiguration.class );
        when( applicationConfiguration.getWorkingDirectory( eq("proxy/attributes")) ).thenReturn( new File( "target/"+ this.getClass().getSimpleName() +"/attributes" ) );

        JacksonJSONFSAttributeStorage
            attributeStorage =  new JacksonJSONFSAttributeStorage( applicationEventMulticaster, applicationConfiguration );
        attributeStorage.initializeWorkingDirectory();
        return attributeStorage;
    }
}