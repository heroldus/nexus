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

import org.sonatype.nexus.mock.components.Component;
import org.sonatype.nexus.mock.components.TextArea;

import com.thoughtworks.selenium.Selenium;

public class RepositorySummary
    extends Component
{

    private TextArea distributionManagement;

    private TextArea repositoryInformation;

    public RepositorySummary( Selenium selenium, String expression )
    {
        super( selenium, expression );

        this.repositoryInformation = new TextArea( selenium, expression + ".find('name', 'informationField')[0]" );
        this.distributionManagement = new TextArea( selenium, expression + ".find('name', 'distMgmtField')[0]" );
    }

    public final TextArea getDistributionManagement()
    {
        return distributionManagement;
    }

    public final TextArea getRepositoryInformation()
    {
        return repositoryInformation;
    }

}
