/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.proxy.router;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.nexus.configuration.ConfigurationChangeEvent;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.IllegalOperationException;
import org.sonatype.nexus.proxy.IllegalRequestException;
import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.LoggingComponent;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.events.AbstractEvent;
import org.sonatype.nexus.proxy.item.AbstractStorageItem;
import org.sonatype.nexus.proxy.item.DefaultStorageCollectionItem;
import org.sonatype.nexus.proxy.item.RepositoryItemUid;
import org.sonatype.nexus.proxy.item.StorageCollectionItem;
import org.sonatype.nexus.proxy.item.StorageFileItem;
import org.sonatype.nexus.proxy.item.StorageItem;
import org.sonatype.nexus.proxy.item.StorageLinkItem;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.registry.RepositoryTypeDescriptor;
import org.sonatype.nexus.proxy.registry.RepositoryTypeRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.storage.UnsupportedStorageOperationException;
import org.sonatype.nexus.proxy.target.TargetSet;
import org.sonatype.nexus.util.ItemPathUtils;

/**
 * The simplest re-implementation for RepositoryRouter that does only routing.
 * 
 * @author cstamas
 */
@Component( role = RepositoryRouter.class )
public class DefaultRepositoryRouter
    extends LoggingComponent
    implements RepositoryRouter, Initializable
{
    @Requirement
    private ApplicationConfiguration applicationConfiguration;

    @Requirement
    private RepositoryRegistry repositoryRegistry;

    @Requirement
    private RepositoryTypeRegistry repositoryTypeRegistry;

    /** Should links be resolved? */
    private boolean followLinks;

    public boolean isFollowLinks()
    {
        return followLinks;
    }

    public void setFollowLinks( boolean followLinks )
    {
        this.followLinks = followLinks;
    }

    public StorageItem dereferenceLink( StorageLinkItem link )
        throws AccessDeniedException,
            ItemNotFoundException,
            IllegalOperationException,
            StorageException
    {
        if ( getLogger().isDebugEnabled() )
        {
            getLogger().debug( "Dereferencing link " + link.getTarget() );
        }

        ResourceStoreRequest req = new ResourceStoreRequest( link.getTarget(), false );

        req.getRequestContext().putAll( link.getItemContext() );

        return link.getTarget().getRepository().retrieveItem( req );
    }

    public StorageItem retrieveItem( ResourceStoreRequest request )
        throws ItemNotFoundException,
            IllegalOperationException,
            StorageException,
            AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            request.pushRequestPath( route.getRepositoryPath() );

            StorageItem item = route.getTargetedRepository().retrieveItem( request );

            request.popRequestPath();

            return mangle( false, request, route, item );
        }
        else
        {
            // this is "above" repositories
            return retrieveVirtualPath( request, route );
        }
    }

    public void storeItem( ResourceStoreRequest request, InputStream is, Map<String, String> userAttributes )
        throws UnsupportedStorageOperationException,
            ItemNotFoundException,
            IllegalOperationException,
            StorageException,
            AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            request.pushRequestPath( route.getRepositoryPath() );

            route.getTargetedRepository().storeItem( request, is, userAttributes );

            request.popRequestPath();
        }
        else
        {
            // this is "above" repositories
            throw new IllegalRequestException( request, "The path '" + request.getRequestPath()
                + "' does not points to any repository!" );
        }
    }

    public void copyItem( ResourceStoreRequest from, ResourceStoreRequest to )
        throws UnsupportedStorageOperationException,
            ItemNotFoundException,
            IllegalOperationException,
            StorageException,
            AccessDeniedException
    {
        RequestRoute fromRoute = getRequestRouteForRequest( from );

        RequestRoute toRoute = getRequestRouteForRequest( to );

        if ( fromRoute.isRepositoryHit() && toRoute.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            from.pushRequestPath( fromRoute.getRepositoryPath() );
            to.pushRequestPath( toRoute.getRepositoryPath() );

            if ( fromRoute.getTargetedRepository() == toRoute.getTargetedRepository() )
            {
                fromRoute.getTargetedRepository().copyItem( from, to );
            }
            else
            {
                StorageItem item = fromRoute.getTargetedRepository().retrieveItem( from );

                if ( item instanceof StorageFileItem )
                {
                    try
                    {
                        toRoute.getTargetedRepository().storeItem(
                            to,
                            ( (StorageFileItem) item ).getInputStream(),
                            item.getAttributes() );
                    }
                    catch ( IOException e )
                    {
                        // XXX: this is nonsense, to box IOException into subclass of IOException!
                        throw new StorageException( e );
                    }
                }
                else if ( item instanceof StorageCollectionItem )
                {
                    toRoute.getTargetedRepository().createCollection( to, item.getAttributes() );
                }
                else
                {
                    throw new IllegalRequestException( from, "Cannot copy item of class='" + item.getClass().getName()
                        + "' over multiple repositories." );
                }

            }

            from.popRequestPath();
            to.popRequestPath();
        }
        else
        {
            // this is "above" repositories
            if ( !fromRoute.isRepositoryHit() )
            {
                throw new IllegalRequestException( from, "The path '" + from.getRequestPath()
                    + "' does not points to any repository!" );
            }
            else
            {
                throw new IllegalRequestException( to, "The path '" + to.getRequestPath()
                    + "' does not points to any repository!" );
            }
        }
    }

    public void moveItem( ResourceStoreRequest from, ResourceStoreRequest to )
        throws UnsupportedStorageOperationException,
            ItemNotFoundException,
            IllegalOperationException,
            StorageException,
            AccessDeniedException
    {
        copyItem( from, to );

        deleteItem( from );
    }

    public Collection<StorageItem> list( ResourceStoreRequest request )
        throws ItemNotFoundException,
            IllegalOperationException,
            StorageException,
            AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            request.pushRequestPath( route.getRepositoryPath() );

            Collection<StorageItem> items = route.getTargetedRepository().list( request );

            request.popRequestPath();

            ArrayList<StorageItem> result = new ArrayList<StorageItem>( items.size() );

            for ( StorageItem item : items )
            {
                result.add( mangle( true, request, route, item ) );
            }

            return result;
        }
        else
        {
            // this is "above" repositories
            return listVirtualPath( request, route );
        }
    }

    public void createCollection( ResourceStoreRequest request, Map<String, String> userAttributes )
        throws UnsupportedStorageOperationException,
            ItemNotFoundException,
            IllegalOperationException,
            StorageException,
            AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            request.pushRequestPath( route.getRepositoryPath() );

            route.getTargetedRepository().createCollection( request, userAttributes );

            request.popRequestPath();
        }
        else
        {
            // this is "above" repositories
            throw new IllegalRequestException( request, "The path '" + request.getRequestPath()
                + "' does not points to any repository!" );
        }
    }

    public void deleteItem( ResourceStoreRequest request )
        throws UnsupportedStorageOperationException,
            ItemNotFoundException,
            IllegalOperationException,
            StorageException,
            AccessDeniedException
    {
        RequestRoute route = getRequestRouteForRequest( request );

        if ( route.isRepositoryHit() )
        {
            // it hits a repository, mangle path and call it

            request.pushRequestPath( route.getRepositoryPath() );

            route.getTargetedRepository().deleteItem( request );

            request.popRequestPath();
        }
        else
        {
            // this is "above" repositories
            throw new IllegalRequestException( request, "The path '" + request.getRequestPath()
                + "' does not points to any repository!" );
        }
    }

    public TargetSet getTargetsForRequest( ResourceStoreRequest request )
    {
        TargetSet result = new TargetSet();

        try
        {
            RequestRoute route = getRequestRouteForRequest( request );

            if ( route.isRepositoryHit() )
            {
                // it hits a repository, mangle path and call it

                request.pushRequestPath( route.getRepositoryPath() );

                result.addTargetSet( route.getTargetedRepository().getTargetsForRequest( request ) );

                request.popRequestPath();
            }
        }
        catch ( ItemNotFoundException e )
        {
            // nothing, empty set will be returned
        }

        return result;
    }

    public void initialize()
    {
        applicationConfiguration.addProximityEventListener( this );
    }

    public void onProximityEvent( AbstractEvent evt )
    {
        if ( ConfigurationChangeEvent.class.isAssignableFrom( evt.getClass() ) )
        {
            followLinks = applicationConfiguration.getConfiguration().getRouting().isFollowLinks();
        }
    }

    // ===
    // Private
    // ===

    protected StorageItem mangle( boolean isList, ResourceStoreRequest request, RequestRoute route, StorageItem item )
        throws AccessDeniedException,
            ItemNotFoundException,
            IllegalOperationException,
            StorageException
    {
        if ( isList )
        {
            ( (AbstractStorageItem) item ).setPath( ItemPathUtils.concatPaths( route.getOriginalRequestPath(), item
                .getName() ) );
        }
        else
        {
            ( (AbstractStorageItem) item ).setPath( route.getOriginalRequestPath() );
        }

        if ( isFollowLinks() && item instanceof StorageLinkItem )
        {
            return dereferenceLink( (StorageLinkItem) item );
        }
        else
        {
            return item;
        }
    }

    // XXX: a todo here is to make the "aliases" ("groups" for GroupRepository.class) dynamic,
    // and even think about new layout: every kind should have it's own "folder", you don't want to see
    // maven2 and P2 repositories along each other...
    public RequestRoute getRequestRouteForRequest( ResourceStoreRequest request )
        throws ItemNotFoundException
    {
        RequestRoute result = new RequestRoute();

        result.setOriginalRequestPath( request.getRequestPath() );

        result.setResourceStoreRequest( request );

        String correctedPath = request.getRequestPath().startsWith( RepositoryItemUid.PATH_SEPARATOR ) ? request
            .getRequestPath().substring( 1, request.getRequestPath().length() ) : request.getRequestPath();

        String[] explodedPath = null;

        if ( StringUtils.isEmpty( correctedPath ) )
        {
            explodedPath = new String[0];
        }
        else
        {
            explodedPath = correctedPath.split( RepositoryItemUid.PATH_SEPARATOR );
        }

        Class<? extends Repository> kind = null;

        result.setRequestDepth( explodedPath.length );

        if ( explodedPath.length >= 1 )
        {
            // we have kind information ("repositories" vs "groups" etc)
            for ( RepositoryTypeDescriptor rtd : repositoryTypeRegistry.getRepositoryTypeDescriptors() )
            {
                try
                {
                    if ( rtd.getPrefix().equals( explodedPath[0] ) )
                    {
                        kind = (Class<Repository>) Class.forName( rtd.getRole() );

                        break;
                    }
                }
                catch ( ClassNotFoundException e )
                {
                    getLogger().info(
                        "RepositoryType with role='" + rtd.getRole()
                            + "' is registered, but not found on classpath, skipping it.",
                        e );
                }
            }

            if ( kind == null )
            {
                // unknown explodedPath[0]
                throw new ItemNotFoundException( request.getRequestPath() );
            }

            result.setStrippedPrefix( ItemPathUtils.concatPaths( explodedPath[0] ) );
        }

        if ( explodedPath.length >= 2 )
        {
            // we have repoId information in path
            Repository repository = null;

            try
            {
                repository = repositoryRegistry.getRepositoryWithFacet( explodedPath[1], kind );

                if ( !repository.isExposed() )
                {
                    // the repo is not exposed
                    throw new ItemNotFoundException( request.getRequestPath() );
                }
            }
            catch ( NoSuchRepositoryException e )
            {
                // obviously, the repoId (explodedPath[1]) points to some nonexistent repoID
                throw new ItemNotFoundException( request.getRequestPath() );
            }

            result.setStrippedPrefix( ItemPathUtils.concatPaths( explodedPath[0], explodedPath[1] ) );

            result.setTargetedRepository( repository );

            String repoPath = "";

            for ( int i = 2; i < explodedPath.length; i++ )
            {
                repoPath = ItemPathUtils.concatPaths( repoPath, explodedPath[i] );
            }

            if ( result.getOriginalRequestPath().endsWith( RepositoryItemUid.PATH_SEPARATOR ) )
            {
                repoPath = repoPath + RepositoryItemUid.PATH_SEPARATOR;
            }

            result.setRepositoryPath( repoPath );
        }

        return result;
    }

    protected StorageItem retrieveVirtualPath( ResourceStoreRequest request, RequestRoute route )
        throws ItemNotFoundException
    {
        DefaultStorageCollectionItem result = new DefaultStorageCollectionItem(
            this,
            route.getOriginalRequestPath(),
            true,
            false );

        result.getItemContext().putAll( request.getRequestContext() );

        return result;
    }

    protected Collection<StorageItem> listVirtualPath( ResourceStoreRequest request, RequestRoute route )
        throws ItemNotFoundException
    {
        if ( route.getRequestDepth() == 0 )
        {
            // 1st level
            ArrayList<StorageItem> result = new ArrayList<StorageItem>();

            for ( RepositoryTypeDescriptor rtd : repositoryTypeRegistry.getRepositoryTypeDescriptors() )
            {
                try
                {
                    // check is there any repo registered
                    if ( !repositoryRegistry.getRepositoriesWithFacet( Class.forName( rtd.getRole() ) ).isEmpty() )
                    {
                        DefaultStorageCollectionItem repositories = new DefaultStorageCollectionItem(
                            this,
                            ItemPathUtils.concatPaths( request.getRequestPath(), rtd.getPrefix() ),
                            true,
                            false );

                        repositories.getItemContext().putAll( request.getRequestContext() );

                        result.add( repositories );
                    }
                }
                catch ( ClassNotFoundException e )
                {
                    getLogger().info(
                        "RepositoryType with role='" + rtd.getRole()
                            + "' is registered, but not found on classpath, skipping it.",
                        e );
                }
            }

            return result;
        }
        else if ( route.getRequestDepth() == 1 )
        {
            // 2nd level
            List<? extends Repository> repositories = null;

            for ( RepositoryTypeDescriptor rtd : repositoryTypeRegistry.getRepositoryTypeDescriptors() )
            {
                try
                {
                    if ( route.getStrippedPrefix().startsWith( "/" + rtd.getPrefix() ) )
                    {
                        repositories = repositoryRegistry.getRepositoriesWithFacet( (Class<Repository>) Class
                            .forName( rtd.getRole() ) );

                        break;
                    }
                }
                catch ( ClassNotFoundException e )
                {
                    getLogger().info(
                        "RepositoryType with role='" + rtd.getRole()
                            + "' is registered, but not found on classpath, skipping it.",
                        e );
                }
            }

            // if no prefix matched, Item not found
            if ( repositories == null )
            {
                throw new ItemNotFoundException( request.getRequestPath() );
            }

            ArrayList<StorageItem> result = new ArrayList<StorageItem>( repositories.size() );

            for ( Repository repository : repositories )
            {
                if ( repository.isExposed() && repository.isBrowseable() )
                {
                    DefaultStorageCollectionItem repoItem = new DefaultStorageCollectionItem( this, ItemPathUtils
                        .concatPaths( request.getRequestPath(), repository.getId() ), true, false );

                    repoItem.getItemContext().putAll( request.getRequestContext() );

                    result.add( repoItem );
                }
            }

            return result;
        }
        else
        {
            throw new ItemNotFoundException( request.getRequestPath() );
        }
    }

}
