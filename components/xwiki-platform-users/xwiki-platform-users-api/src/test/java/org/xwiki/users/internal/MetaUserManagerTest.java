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
package org.xwiki.users.internal;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the meta user manager.
 *
 * @version $Id$
 * @since 1.0M9
 */
public class MetaUserManagerTest
{
    @Rule
    public final MockitoComponentMockingRule<UserManager> mocker =
        new MockitoComponentMockingRule<UserManager>(MetaUserManager.class);

    private final String adminUsername = "Admin";

    private final String userSpace = "XWiki";

    private final String mainWiki = "xwiki";

    private UserManager userManager;

    private EntityReferenceResolver<String> resolver;

    private EntityReferenceSerializer<String> serializer;

    private ConfigurationSource configuration;

    private ModelConfiguration modelConfiguration;

    private ComponentManager cm;

    @Before
    public void setUp() throws Exception
    {
        this.resolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "explicit");
        this.serializer = this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);
        this.configuration = this.mocker.getInstance(ConfigurationSource.class, "xwikiproperties");
        this.modelConfiguration = this.mocker.getInstance(ModelConfiguration.class);
        this.userManager = this.mocker.getComponentUnderTest();
        this.cm = this.mocker.getInstance(ComponentManager.class);
    }

    @AfterComponent
    public void setupMockComponentManager() throws Exception
    {
        this.mocker.registerMockComponent(ComponentManager.class);
    }

    @Test
    public void getUserFromUsernameWithNoManagers() throws Exception
    {
        DocumentReference admin = new DocumentReference(this.mainWiki, this.userSpace, this.adminUsername);
        when(this.cm.getInstanceMap(UserManager.class)).thenReturn(Collections.<String, Object>emptyMap());
        when(this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn(this.mainWiki);
        when(this.configuration.getProperty("users.defaultWiki", this.mainWiki)).thenReturn(this.mainWiki);
        when(this.resolver.resolve(this.adminUsername, EntityType.DOCUMENT,
            new EntityReference(this.userSpace, EntityType.SPACE, new WikiReference(this.mainWiki))))
                .thenReturn(admin);
        when(this.serializer.serialize(admin, new Object[0])).thenReturn("xwiki:XWiki.Admin");

        User u = this.userManager.getUser(this.adminUsername);
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void getUserFromFullReferenceWithNoManagers() throws Exception
    {
        final DocumentReference timmy = new DocumentReference("playground", "SouthPark", "Timmy");
        when(this.cm.getInstanceMap(UserManager.class)).thenReturn(Collections.<String, Object>emptyMap());
        when(this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn(this.mainWiki);
        when(this.configuration.getProperty("users.defaultWiki", this.mainWiki)).thenReturn(this.mainWiki);
        when(this.resolver.resolve("playground:SouthPark.Timmy", EntityType.DOCUMENT,
            new EntityReference(this.userSpace, EntityType.SPACE, new WikiReference(this.mainWiki)))).thenReturn(timmy);
        when(this.serializer.serialize(timmy, new Object[0])).thenReturn("playground:SouthPark.Timmy");

        User u = this.userManager.getUser("playground:SouthPark.Timmy");
        Assert.assertEquals("playground:SouthPark.Timmy", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void differentUsersWikiWithNoManagers() throws Exception
    {
        final DocumentReference admin = new DocumentReference("sandbox", this.userSpace, this.adminUsername);
        when(this.cm.getInstanceMap(UserManager.class)).thenReturn(Collections.<String, Object>emptyMap());
        when(this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn(this.mainWiki);
        when(this.configuration.getProperty("users.defaultWiki", this.mainWiki)).thenReturn("sandbox");
        when(this.resolver.resolve(this.adminUsername, EntityType.DOCUMENT,
            new EntityReference(this.userSpace, EntityType.SPACE, new WikiReference("sandbox")))).thenReturn(admin);
        when(this.serializer.serialize(admin, new Object[0])).thenReturn("sandbox:XWiki.Admin");

        User u = this.userManager.getUser(this.adminUsername);
        Assert.assertEquals("sandbox:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void differentGlobalWikiWithNoManagers() throws Exception
    {
        final DocumentReference admin = new DocumentReference("test", this.userSpace, this.adminUsername);
        when(this.cm.getInstanceMap(UserManager.class)).thenReturn(Collections.<String, Object>emptyMap());
        when(this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn("test");
        when(this.configuration.getProperty("users.defaultWiki", "test")).thenReturn("test");
        when(this.resolver.resolve(this.adminUsername, EntityType.DOCUMENT,
            new EntityReference(this.userSpace, EntityType.SPACE, new WikiReference("test")))).thenReturn(admin);
        when(this.serializer.serialize(admin, new Object[0])).thenReturn("test:XWiki.Admin");

        User u = this.userManager.getUser(this.adminUsername);
        Assert.assertEquals("test:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void managerIteration() throws Exception
    {
        final User mockUser = mock(User.class);
        final UserManager mockManager1 = mock(UserManager.class, "um1");
        final UserManager mockManager2 = mock(UserManager.class, "um2");
        final UserManager mockManager3 = mock(UserManager.class, "um3");
        final Map<String, UserManager> managers = new LinkedHashMap<>();
        managers.put("1", mockManager1);
        managers.put("2", mockManager2);
        managers.put("3", mockManager3);
        when(this.cm.<UserManager>getInstanceMap(UserManager.class)).thenReturn(managers);
        when(mockManager1.getUser(this.adminUsername)).thenReturn(null);
        when(mockManager2.getUser(this.adminUsername)).thenReturn(mockUser);

        User u = this.userManager.getUser(this.adminUsername);
        Assert.assertSame(mockUser, u);
    }

    @Test
    public void managerIterationWithNoResults() throws Exception
    {
        final User mockUser = mock(User.class);
        final UserManager mockManager1 = mock(UserManager.class, "ldap");
        final UserManager mockManager2 = mock(UserManager.class, "wiki");
        final Map<String, UserManager> managers = new LinkedHashMap<>();
        managers.put("ldap", mockManager1);
        managers.put("wiki", mockManager2);
        when(this.cm.<UserManager>getInstanceMap(UserManager.class)).thenReturn(managers);
        when(this.cm.getInstance(UserManager.class, "wiki")).thenReturn(mockManager2);
        when(mockManager1.getUser(this.adminUsername)).thenReturn(null);
        when(mockManager2.getUser(this.adminUsername)).thenReturn(null);

        when(this.configuration.getProperty("users.defaultUserManager", "wiki")).thenReturn("wiki");

        when(mockManager2.getUser(this.adminUsername, true)).thenReturn(mockUser);
        User u = this.userManager.getUser(this.adminUsername, true);
        Assert.assertSame(mockUser, u);
    }

    @Test
    public void managerIterationWithNoResultsAndNoDefaultValue() throws Exception
    {
        final UserManager mockManager1 = mock(UserManager.class, "1");
        final Map<String, UserManager> managers = new LinkedHashMap<>();
        final DocumentReference admin = new DocumentReference("test", this.userSpace, this.adminUsername);
        managers.put("1", mockManager1);
        when(this.cm.<UserManager>getInstanceMap(UserManager.class)).thenReturn(managers);
        when(this.cm.getInstance(UserManager.class, "wiki")).thenThrow(
            new ComponentLookupException("Missing Implementation"));

        when(mockManager1.getUser(this.adminUsername)).thenReturn(null);
        when(this.configuration.getProperty("users.defaultUserManager", "wiki")).thenReturn("wiki");
        when(this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn("test");
        when(this.configuration.getProperty("users.defaultWiki", "test")).thenReturn("test");
        when(this.configuration.getProperty("users.defaultUserManager", "wiki")).thenReturn("wiki");
        when(this.serializer.serialize(admin, new Object[0])).thenReturn("test:XWiki.Admin");
        when(this.resolver.resolve(this.adminUsername, EntityType.DOCUMENT,
            new EntityReference(this.userSpace, EntityType.SPACE, new WikiReference("test")))).thenReturn(admin);

        User u = this.userManager.getUser(this.adminUsername, true);
        Assert.assertEquals("test:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void lookupExceptions() throws Exception
    {
        final DocumentReference admin = new DocumentReference("test", this.userSpace, this.adminUsername);
        when(this.cm.getInstanceMap(UserManager.class)).thenThrow(new ComponentLookupException("No Implementations"));
        when(this.configuration.getProperty("users.defaultUserManager", "wiki")).thenReturn("wiki");
        when(this.cm.getInstance(UserManager.class, "wiki")).thenThrow(
            new ComponentLookupException("Missing Implementation"));
        when(this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn(this.mainWiki);
        when(this.configuration.getProperty("users.defaultWiki", this.mainWiki)).thenReturn(this.mainWiki);
        when(this.resolver.resolve(this.adminUsername, EntityType.DOCUMENT,
            new EntityReference(this.userSpace, EntityType.SPACE, new WikiReference(this.mainWiki)))).thenReturn(admin);
        when(this.serializer.serialize(admin, new Object[0])).thenReturn("xwiki:XWiki.Admin");

        User u = this.userManager.getUser(this.adminUsername, true);
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void differentDefaultUserManager() throws Exception
    {
        final DocumentReference admin = new DocumentReference("test", this.userSpace, this.adminUsername);
        when(this.cm.getInstanceMap(UserManager.class)).thenReturn(Collections.<String, Object>emptyMap());
        when(this.configuration.getProperty("users.defaultUserManager", "wiki")).thenReturn("ldap");
        when(this.cm.getInstance(UserManager.class, "ldap")).thenThrow(
            new ComponentLookupException("Missing Implementation"));
        when(this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn(this.mainWiki);
        when(this.configuration.getProperty("users.defaultWiki", this.mainWiki)).thenReturn(this.mainWiki);
        when(this.resolver.resolve(this.adminUsername, EntityType.DOCUMENT,
            new EntityReference(this.userSpace, EntityType.SPACE, new WikiReference(this.mainWiki)))).thenReturn(admin);
        when(this.serializer.serialize(admin, new Object[0])).thenReturn("xwiki:XWiki.Admin");

        User u = this.userManager.getUser(this.adminUsername, true);
        Assert.assertEquals("xwiki:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void nullIdentifierReturnsInvalidUser() throws Exception
    {
        User u = this.userManager.getUser(null);
        Assert.assertEquals("", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void emptyIdentifierReturnsInvalidUser() throws Exception
    {
        User u = this.userManager.getUser("");
        Assert.assertEquals("", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void blankIdentifierReturnsInvalidUser() throws Exception
    {
        User u = this.userManager.getUser("\n \t");
        Assert.assertEquals("", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void blankIdentifierAndForceReturnsInvalidUser() throws Exception
    {
        User u = this.userManager.getUser("\n \t", true);
        Assert.assertEquals("", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }

    @Test
    public void forceWithMissingDefaultManager() throws Exception
    {
        final DocumentReference admin = new DocumentReference("test", this.userSpace, this.adminUsername);
        when(this.cm.getInstanceMap(UserManager.class)).thenReturn(Collections.<String, Object>emptyMap());
        when(this.cm.getInstance(UserManager.class, "wiki")).thenThrow(
            new ComponentLookupException("Missing implementation"));
        when(this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)).thenReturn("test");
        when(this.configuration.getProperty("users.defaultWiki", "test")).thenReturn("test");
        when(this.configuration.getProperty("users.defaultUserManager", "wiki")).thenReturn("wiki");
        when(this.serializer.serialize(admin, new Object[0])).thenReturn("test:XWiki.Admin");
        when(this.resolver.resolve(this.adminUsername, EntityType.DOCUMENT,
            new EntityReference(this.userSpace, EntityType.SPACE, new WikiReference("test")))).thenReturn(admin);

        User u = this.userManager.getUser(this.adminUsername);
        Assert.assertEquals("test:XWiki.Admin", u.getId());
        Assert.assertTrue(u instanceof InvalidUser);
    }
}
