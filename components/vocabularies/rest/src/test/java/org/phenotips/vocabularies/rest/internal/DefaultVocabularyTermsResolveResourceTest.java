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
package org.phenotips.vocabularies.rest.internal;

import org.phenotips.rest.Autolinker;
import org.phenotips.vocabularies.rest.VocabularyTermsResolveResource;
import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyManager;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link DefaultVocabularyTermsResolveResource} class.
 */
public class DefaultVocabularyTermsResolveResourceTest
{
    private static final String ID_FIELD = "id";

    private static final String TERM_1_ID = "HP:term1";

    private static final String TERM_2_ID = "HP:term2";

    private static final String TERM_3_ID = "OMIM:term3";

    private static final String INVALID = "noprefix";

    private static final String TERM_ID = "term-id";

    private static final String LINKS_FIELD = "links";

    private static final String ROWS_FIELD = "rows";

    private static final String OMIM_PREFIX = "OMIM";

    private static final String HP_PREFIX = "HP";

    @Rule
    public MockitoComponentMockingRule<VocabularyTermsResolveResource> mocker =
        new MockitoComponentMockingRule<>(DefaultVocabularyTermsResolveResource.class);

    @Mock
    private UriInfo uriInfo;

    @Mock
    private Request request;

    @Mock
    private VocabularyTerm term1;

    @Mock
    private VocabularyTerm term2;

    @Mock
    private VocabularyTerm term3;

    @Mock
    private Vocabulary vocabularyHpo;

    @Mock
    private Vocabulary vocabularyOmim;

    private VocabularyTermsResolveResource component;

    private VocabularyManager vm;

    private Logger logger;

    @Before
    public void setUp() throws ComponentLookupException
    {
        MockitoAnnotations.initMocks(this);

        final Execution execution = mock(Execution.class);
        final ExecutionContext executionContext = mock(ExecutionContext.class);
        final ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(Execution.class)).thenReturn(execution);
        when(execution.getContext()).thenReturn(executionContext);
        when(executionContext.getProperty("xwikicontext")).thenReturn(mock(XWikiContext.class));

        ReflectionUtils.setFieldValue(this.mocker.getComponentUnderTest(), "uriInfo", this.uriInfo);

        this.component = this.mocker.getComponentUnderTest();
        this.vm = this.mocker.getInstance(VocabularyManager.class);
        this.logger = this.mocker.getMockedLogger();

        final Autolinker autolinker = this.mocker.getInstance(Autolinker.class);

        when(autolinker.forSecondaryResource(any(Class.class), eq(this.uriInfo))).thenReturn(autolinker);
        when(autolinker.forResource(any(Class.class), eq(this.uriInfo))).thenReturn(autolinker);
        when(autolinker.withActionableResources(any(Class.class))).thenReturn(autolinker);
        when(autolinker.withExtraParameters(anyString(), anyString())).thenReturn(autolinker);
        when(autolinker.build()).thenReturn(Collections.emptyList());

        when(this.vm.getVocabulary(HP_PREFIX)).thenReturn(this.vocabularyHpo);
        when(this.vm.getVocabulary(OMIM_PREFIX)).thenReturn(this.vocabularyOmim);

        final Set<String> termIdsHpo = new HashSet<>();
        termIdsHpo.add(TERM_1_ID);
        termIdsHpo.add(TERM_2_ID);

        final Set<VocabularyTerm> termsHpo = new HashSet<>();
        termsHpo.add(this.term1);
        termsHpo.add(this.term2);

        final Set<VocabularyTerm> termsOmim = new HashSet<>();
        termsOmim.add(this.term3);

        final Set<String> termIdsOmim = new HashSet<>();
        termIdsOmim.add(TERM_3_ID);

        when(this.vocabularyHpo.getTerms(eq(termIdsHpo))).thenReturn(termsHpo);
        when(this.vocabularyOmim.getTerms(eq(termIdsOmim))).thenReturn(termsOmim);

        when(this.term1.toJSON()).thenReturn(new JSONObject().put(ID_FIELD, TERM_1_ID));
        when(this.term2.toJSON()).thenReturn(new JSONObject().put(ID_FIELD, TERM_2_ID));
        when(this.term3.toJSON()).thenReturn(new JSONObject().put(ID_FIELD, TERM_3_ID));

        final Container container = this.mocker.getInstance(Container.class);

        when(container.getRequest()).thenReturn(this.request);
        when(this.request.getProperties(TERM_ID)).thenReturn(Arrays.asList(TERM_1_ID, TERM_2_ID, TERM_3_ID));
    }

    @Test
    public void resolveTermsNoTermIdsIncluded()
    {
        when(this.request.getProperties(TERM_ID)).thenReturn(Collections.emptyList());
        final Response response = this.component.resolveTerms();
        Assert.assertEquals(Response.Status.NO_CONTENT.getStatusCode(), response.getStatus());

        verify(this.logger, times(1)).info("No content provided.");
    }

    @Test
    public void resolveTermsRetrievesCorrectData()
    {
        final Response response = this.component.resolveTerms();
        final JSONObject expected = new JSONObject()
            .put(LINKS_FIELD, new JSONArray())
            .put(ROWS_FIELD, new JSONArray()
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_1_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_2_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_3_ID)));

        final JSONObject actual = (JSONObject) response.getEntity();
        Assert.assertEquals(expected.length(), actual.length());
        Assert.assertTrue(expected.getJSONArray(LINKS_FIELD).similar(actual.getJSONArray(LINKS_FIELD)));
        // Rows are unordered since getTerms returns a set.
        Assert.assertEquals(new HashSet<>(expected.getJSONArray(ROWS_FIELD).toList()),
            new HashSet<>(actual.getJSONArray(ROWS_FIELD).toList()));

        verify(this.logger, never()).info(anyString());
        verify(this.logger, never()).warn(anyString());
        verify(this.logger, never()).error(anyString());
    }

    @Test
    public void resolveTermsIgnoresUnprefixedTerms()
    {
        when(this.request.getProperties(TERM_ID)).thenReturn(Arrays.asList(TERM_1_ID, INVALID, TERM_2_ID, TERM_3_ID));
        final Response response = this.component.resolveTerms();
        final JSONObject expected = new JSONObject()
            .put(LINKS_FIELD, new JSONArray())
            .put(ROWS_FIELD, new JSONArray()
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_1_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_2_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_3_ID)));

        final JSONObject actual = (JSONObject) response.getEntity();
        Assert.assertEquals(expected.length(), actual.length());
        Assert.assertTrue(expected.getJSONArray(LINKS_FIELD).similar(actual.getJSONArray(LINKS_FIELD)));
        // Rows are unordered since getTerms returns a set.
        Assert.assertEquals(new HashSet<>(expected.getJSONArray(ROWS_FIELD).toList()),
            new HashSet<>(actual.getJSONArray(ROWS_FIELD).toList()));

        verify(this.logger, times(1)).warn("Term [{}] does not begin with a valid prefix", INVALID);
    }

    @Test
    public void resolveTermsFiltersNullIds()
    {
        when(this.request.getProperties(TERM_ID)).thenReturn(Arrays.asList(TERM_1_ID, null, TERM_2_ID, TERM_3_ID));
        final Response response = this.component.resolveTerms();
        final JSONObject expected = new JSONObject()
            .put(LINKS_FIELD, new JSONArray())
            .put(ROWS_FIELD, new JSONArray()
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_1_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_2_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_3_ID)));

        final JSONObject actual = (JSONObject) response.getEntity();
        Assert.assertEquals(expected.length(), actual.length());
        Assert.assertTrue(expected.getJSONArray(LINKS_FIELD).similar(actual.getJSONArray(LINKS_FIELD)));
        // Rows are unordered since getTerms returns a set.
        Assert.assertEquals(new HashSet<>(expected.getJSONArray(ROWS_FIELD).toList()),
            new HashSet<>(actual.getJSONArray(ROWS_FIELD).toList()));

        verify(this.logger, never()).info(anyString());
        verify(this.logger, never()).warn(anyString());
        verify(this.logger, never()).error(anyString());
    }

    @Test
    public void resolveTermsIgnoresInvalidVocabularies()
    {
        final Set<String> termIdsOmim = new HashSet<>();
        termIdsOmim.add(TERM_3_ID);

        when(this.vm.getVocabulary(OMIM_PREFIX)).thenReturn(null);
        final Response response = this.component.resolveTerms();
        final JSONObject expected = new JSONObject()
            .put(LINKS_FIELD, new JSONArray())
            .put(ROWS_FIELD, new JSONArray()
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_1_ID))
                .put(new JSONObject()
                    .put(LINKS_FIELD, new JSONArray())
                    .put(ID_FIELD, TERM_2_ID)));

        final JSONObject actual = (JSONObject) response.getEntity();
        Assert.assertEquals(expected.length(), actual.length());
        Assert.assertTrue(expected.getJSONArray(LINKS_FIELD).similar(actual.getJSONArray(LINKS_FIELD)));
        // Rows are unordered since getTerms returns a set.
        Assert.assertEquals(new HashSet<>(expected.getJSONArray(ROWS_FIELD).toList()),
            new HashSet<>(actual.getJSONArray(ROWS_FIELD).toList()));

        verify(this.logger, times(1)).warn("Could not resolve terms [{}]. No matching vocabulary found.", termIdsOmim);
    }
}
