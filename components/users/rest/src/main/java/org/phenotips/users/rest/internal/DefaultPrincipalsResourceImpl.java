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
package org.phenotips.users.rest.internal;

import org.phenotips.groups.internal.UsersAndGroups;
import org.phenotips.users.rest.PrincipalsResource;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONObject;

/**
 * Default implementation for {@link PrincipalsResource} using XWiki's support for REST resources.
 *
 * @version $Id$
 * @since 1.4
 */
@Component
@Named("org.phenotips.users.rest.internal.DefaultPrincipalsResourceImpl")
@Singleton
public class DefaultPrincipalsResourceImpl extends XWikiResource implements PrincipalsResource
{
    @Inject
    private UsersAndGroups usersAndGroups;

    @Override
    public Response getAllUsersAndGroups()
    {
        JSONObject json = this.usersAndGroups.getAll();
        return Response.ok(json, MediaType.APPLICATION_JSON_TYPE).build();
    }
}
