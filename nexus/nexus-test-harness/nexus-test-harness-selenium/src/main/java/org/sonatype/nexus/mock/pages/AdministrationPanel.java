/**
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2007-2012 Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.mock.pages;

import com.thoughtworks.selenium.Selenium;

public class AdministrationPanel
    extends SidePanel
{
    public AdministrationPanel( Selenium selenium )
    {
        super( selenium, "window.Ext.getCmp('st-nexus-config')" );
    }

    public boolean serverAvailable()
    {
        return isLinkAvailable( "Server" );
    }

    public boolean routingAvailable()
    {
        return isLinkAvailable( "Routing" );
    }

    public boolean scheduleTasksAvailable()
    {
        return isLinkAvailable( "Scheduled Tasks" );
    }

    public void clickScheduleTasks()
    {
        clickLink( "Scheduled Tasks" );
    }

    public boolean logAvailable()
    {
        return isLinkAvailable( "Log Configuration" );
    }

    public void serverClick()
    {
        clickLink( "Server" );
    }

    public void logClick()
    {
        clickLink( "Log Configuration" );
    }

    public void routingClick()
    {
        clickLink( "Routing" );
    }

    public boolean logsAndConfigFilesAvailable()
    {
        return isLinkAvailable( "System Files" );
    }

    public void logsAndConfigFilesClick()
    {
        clickLink( "System Files" );
    }
}
