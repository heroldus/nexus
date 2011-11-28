/**
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
package org.sonatype.nexus.plugins.capabilities.internal.activation;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sonatype.nexus.plugins.capabilities.api.Capability;
import org.sonatype.nexus.plugins.capabilities.api.CapabilityReference;
import org.sonatype.nexus.plugins.capabilities.api.CapabilityRegistry;
import org.sonatype.nexus.plugins.capabilities.api.activation.ActivationContext;
import org.sonatype.nexus.plugins.capabilities.support.activation.AbstractCondition;

/**
 * A condition that is satisfied when a capability of a specified type exists.
 *
 * @since 1.10.0
 */
public class CapabilityOfTypeExistsCondition
    extends AbstractCondition
    implements CapabilityRegistry.Listener
{

    private final CapabilityRegistry capabilityRegistry;

    final Class<?> type;

    public CapabilityOfTypeExistsCondition( final ActivationContext activationContext,
                                            final CapabilityRegistry capabilityRegistry,
                                            final Class<? extends Capability> type )
    {
        super( activationContext );
        this.capabilityRegistry = checkNotNull( capabilityRegistry );
        this.type = type;
    }

    @Override
    protected void doBind()
    {
        capabilityRegistry.addListener( this );
    }

    @Override
    public void doRelease()
    {
        capabilityRegistry.removeListener( this );
    }

    @Override
    public void onAdd( final CapabilityReference reference )
    {
        if ( !isSatisfied() && type.isAssignableFrom( reference.capability().getClass() ) )
        {
            checkAllCapabilities();
        }
    }

    @Override
    public void onRemove( final CapabilityReference reference )
    {
        if ( isSatisfied() && type.isAssignableFrom( reference.capability().getClass() ) )
        {
            checkAllCapabilities();
        }
    }

    @Override
    public void onActivate( final CapabilityReference reference )
    {
        // ignore
    }

    @Override
    public void onPassivate( final CapabilityReference reference )
    {
        // ignore
    }

    @Override
    public void beforeUpdate( final CapabilityReference reference )
    {
        // ignore
    }

    @Override
    public void afterUpdate( final CapabilityReference reference )
    {
        // ignore
    }

    void checkAllCapabilities()
    {
        for ( final CapabilityReference ref : capabilityRegistry.getAll() )
        {
            if ( isSatisfied( ref ) )
            {
                if ( !isSatisfied() )
                {
                    setSatisfied( true );
                }
                return;
            }
        }
        if ( isSatisfied() )
        {
            setSatisfied( false );
        }
    }

    boolean isSatisfied( final CapabilityReference reference )
    {
        return type.isAssignableFrom( reference.capability().getClass() );
    }

    @Override
    public String toString()
    {
        return type.getSimpleName() + " exists";
    }

}