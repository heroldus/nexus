/*
 * Nexus Plugin for Maven
 * Copyright (C) 2009 Sonatype, Inc.                                                                                                                          
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.plugin;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.nexus.restlight.common.RESTLightClientException;
import org.sonatype.nexus.restlight.stage.StageClient;
import org.sonatype.nexus.restlight.stage.StageRepository;

/**
 * Drop a closed Nexus staging repository.
 * 
 * @goal staging-drop
 * @requiresProject false
 * @aggregator
 */
// TODO: Remove aggregator annotation once we have a better solution, but we should only run this once per build.
public class DropStageRepositoryMojo
    extends AbstractStagingMojo
{

    /**
     * If set to <code>true</code>, allow auto-selection of the repository to drop in cases where no repositoryId has
     * been specified during execution, and only one staged repository is available. <br/>
     * <b>NOTE:</b> Use with care! This can be dangerous!
     * 
     * @parameter expression="${nexus.drop.autoSelectOverride}" default-value="false"
     */
    private boolean dropAutoSelectOverride;

    /**
     * @parameter default-value="Staging Dropping ${project.build.finalName}" expression="${description}"
     */
    private String description;

    public void execute()
        throws MojoExecutionException
    {
        fillMissing();

        initLog4j();

        StageClient client = getClient();

        List<StageRepository> repos;
        try
        {
            repos = client.getClosedStageRepositoriesForUser();
        }
        catch ( RESTLightClientException e )
        {
            throw new MojoExecutionException( "Failed to find closed staging repository: " + e.getMessage(), e );
        }
        
        if ( repos != null && !repos.isEmpty() )
        {
            StageRepository repo = select( repos, "Select a repository to drop", isDropAutoSelectOverride() );
            
            StringBuilder builder = new StringBuilder();
            builder.append( "Dropping staged repository: " );

            builder.append( "\n\n-  " );
            builder.append( listRepo( repo ) );

            builder.append( "\n\n" );

            getLog().info( builder.toString() );

            try
            {
                client.dropRepository( repo, description );
            }
            catch ( RESTLightClientException e )
            {
                throw new MojoExecutionException( "Failed to drop staging repository: " + e.getMessage(), e );
            }
        }
        else
        {
            getLog().info( "\n\nNo closed staging repositories found. Nothing to do!\n\n" );
        }

        listRepos( null, null, null, "The following CLOSED staging repositories were found" );
    }

    public boolean isDropAutoSelectOverride()
    {
        return dropAutoSelectOverride;
    }

    public void setDropAutoSelectOverride( final boolean dropAutoSelectOverride )
    {
        this.dropAutoSelectOverride = dropAutoSelectOverride;
    }

}