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
package org.phenotips.data.permissions.internal.access;

import org.phenotips.data.permissions.AccessLevel;
import org.phenotips.translation.TranslationManager;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link EditAccessLevel edit access level}.
 *
 * @version $Id$
 */
public class ViewAccessLevelTest
{
    @Rule
    public final MockitoComponentMockingRule<AccessLevel> mocker =
        new MockitoComponentMockingRule<>(ViewAccessLevel.class);

    @Before
    public void setup() throws ComponentLookupException
    {
        TranslationManager tm = this.mocker.getInstance(TranslationManager.class);
        when(tm.translate(Matchers.anyString())).thenReturn("");
    }

    /** Basic test for {@link AccessLevel#getName()}. */
    @Test
    public void getName() throws ComponentLookupException
    {
        Assert.assertEquals("view", this.mocker.getComponentUnderTest().getName());
    }

    /** Basic test for {@link AccessLevel#isAssignable()}. */
    @Test
    public void isAssignable() throws ComponentLookupException
    {
        Assert.assertTrue(this.mocker.getComponentUnderTest().isAssignable());
    }

    @Test
    public void grantsViewRight() throws ComponentLookupException
    {
        Assert.assertSame(Right.VIEW, this.mocker.getComponentUnderTest().getGrantedRight());
    }

    /** Basic test for {@link AccessLevel#getLabel()}. */
    @Test
    public void getLabel() throws ComponentLookupException
    {
        TranslationManager tm = this.mocker.getInstance(TranslationManager.class);
        when(tm.translate("phenotips.permissions.accessLevels.view.label")).thenReturn("Viewer");
        Assert.assertEquals("Viewer", this.mocker.getComponentUnderTest().getLabel());
    }

    /** {@link AccessLevel#getLabel()} returns the name when a translation isn't found. */
    @Test
    public void getLabelWithoutTranslation() throws ComponentLookupException
    {
        Assert.assertEquals("view", this.mocker.getComponentUnderTest().getLabel());
    }

    /** Basic test for {@link AccessLevel#getDescription()}. */
    @Test
    public void getDescription() throws ComponentLookupException
    {
        TranslationManager tm = this.mocker.getInstance(TranslationManager.class);
        when(tm.translate("phenotips.permissions.accessLevels.view.description"))
            .thenReturn("Can view the patient data");
        Assert.assertEquals("Can view the patient data", this.mocker.getComponentUnderTest().getDescription());
    }

    /** {@link AccessLevel#getDescription()} returns an empty string when a translation isn't found. */
    @Test
    public void getDescriptionWithoutTranslation() throws ComponentLookupException
    {
        Assert.assertEquals("", this.mocker.getComponentUnderTest().getDescription());
    }

    /** Basic test for {@link AccessLevel#toString()}. */
    @Test
    public void toStringTest() throws ComponentLookupException
    {
        Assert.assertEquals("view", this.mocker.getComponentUnderTest().toString());
    }

    /** Basic test for {@link AccessLevel#equals(Object)}. */
    @Test
    public void equalsTest() throws ComponentLookupException
    {
        // Equals itself
        Assert.assertTrue(this.mocker.getComponentUnderTest().equals(this.mocker.getComponentUnderTest()));
        // Never equals null
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(null));
        // Equals another level with the same name
        AccessLevel other = mock(AccessLevel.class);
        when(other.getName()).thenReturn("view", "edit");
        Assert.assertTrue(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal a level with a different name
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals(other));
        // Doesn't equal other types of objects
        Assert.assertFalse(this.mocker.getComponentUnderTest().equals("view"));
    }

    /** Basic test for {@link AccessLevel#compareTo(Object)}. */
    @Test
    public void compareToTest() throws ComponentLookupException
    {
        // Equals itself
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().compareTo(this.mocker.getComponentUnderTest()));
        // Nulls come after
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(null) > 0);
        // Equals another level with the same permissiveness
        Assert.assertEquals(0, this.mocker.getComponentUnderTest().compareTo(new MockAccessLevel("read", 10, true)));
        // Respects the permissiveness order
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(new MockAccessLevel("none", 0, true)) > 0);
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(new MockAccessLevel("edit", 20, true)) < 0);
        // Other types of levels are placed after
        Assert.assertTrue(this.mocker.getComponentUnderTest().compareTo(mock(AccessLevel.class)) > 0);
    }
}
