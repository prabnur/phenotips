/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.data.permissions.rest;

import org.phenotips.data.permissions.rest.model.VisibilityRepresentation;
import org.phenotips.data.rest.PatientResource;
import org.phenotips.rest.ParentResource;
import org.phenotips.rest.RelatedResources;
import org.phenotips.rest.Relation;
import org.phenotips.rest.RequiredAccess;

import org.xwiki.component.annotation.Role;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Resource for working with visibility of entity records, where the entity record is identified by its internal
 * identifier.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Role
@Path("/{entity-type}/{entity-id}/permissions/visibility")
@Relation("https://phenotips.org/rel/visibility")
@ParentResource(PermissionsResource.class)
@RelatedResources(PatientResource.class)
public interface VisibilityResource
{
    /**
     * Retrieve the {@link org.phenotips.data.permissions.Visibility} of an entity identified by `entityId`. If the
     * indicated entity record doesn't exist, or if the user sending the request doesn't have the right to view the
     * target entity record, an error is returned.
     *
     * @param entityId identifier of the entity whose visibility to retrieve
     * @param entityType the type of entity (either "patients" or "families")
     * @return a representation of the visibility of the entity
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequiredAccess("view")
    VisibilityRepresentation getVisibility(@PathParam("entity-id") String entityId,
        @PathParam("entity-type") String entityType);

    /**
     * Update the visibility of an entity. If the indicated entity record doesn't exist, or if the user sending the
     * request doesn't have the right to edit the target entity record, no change is performed and an error is
     * returned.
     *
     * @param visibility which must contain "level" parameter, with a valid visibility level name as the value
     * @param entityId identifier of the entity whose visibility should be changed
     * @param entityType the type of entity (either "patients" or "families")
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_JSON)
    Response setVisibility(VisibilityRepresentation visibility, @PathParam("entity-id") String entityId,
        @PathParam("entity-type") String entityType);

    /**
     * Update the visibility of an entity. If the indicated entity record doesn't exist, or if the user sending the
     * request doesn't have the right to edit the target entity record, no change is performed and an error is
     * returned. The request must contain a "visibility" parameter, with a valid visibility level name as the value.
     *
     * @param entityId identifier of the entity whose visibility should be changed
     * @param entityType the type of entity (either "patients" or "families")
     * @return a status message
     */
    @PUT
    @RequiredAccess("manage")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response setVisibility(@PathParam("entity-id") String entityId, @PathParam("entity-type") String entityType);
}
