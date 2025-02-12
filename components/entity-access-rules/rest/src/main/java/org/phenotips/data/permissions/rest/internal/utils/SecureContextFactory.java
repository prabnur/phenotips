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
package org.phenotips.data.permissions.rest.internal.utils;

import org.phenotips.entities.PrimaryEntity;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

import javax.ws.rs.WebApplicationException;

/**
 * Allows to get the current user and entity record securely - the context returned must not allow further interaction
 * if the minimum rights levels are not met.
 *
 * @version $Id$
 * @since 1.3M2
 */
@Unstable
@Role
public interface SecureContextFactory
{
    /**
     * Provides the entity, current user, and access context, making sure that the current user has high enough access
     * level.
     *
     * @param entityId by which an entity is to be found
     * @param entityType the type of entity
     * @param minimumAccessLevel that the current user must have, that maps to one of
     *            {@link org.phenotips.data.permissions.AccessLevel}
     * @return context containing a {@link PrimaryEntity} instance and the current user
     * @throws WebApplicationException if the entity could not be found, or the current user does not have high enough
     *             access level
     */
    EntityAccessContext getContext(String entityId, String entityType, String minimumAccessLevel)
        throws WebApplicationException;

    /**
     * Provides the entity, current user, and access context, for a read operation, making sure that the current user
     * has view access on the entity record.
     *
     * @param entityId by which an entity is to be found
     * @param entityType the type of entity
     * @return context containing a {@link PrimaryEntity} instance and the current user
     * @throws WebApplicationException if the entity could not be found, or the current user does not have high enough
     *             access level
     */
    EntityAccessContext getReadContext(String entityId, String entityType) throws WebApplicationException;

    /**
     * Provides the entity, current user, and access context, for a read operation, making sure that the current user
     * has manage access on the entity record.
     *
     * @param entityId by which an entity is to be found
     * @param entityType the type of entity
     * @return context containing a {@link PrimaryEntity} instance and the current user
     * @throws WebApplicationException if the entity could not be found, or the current user does not have high enough
     *             access level
     */
    EntityAccessContext getWriteContext(String entityId, String entityType) throws WebApplicationException;
}
