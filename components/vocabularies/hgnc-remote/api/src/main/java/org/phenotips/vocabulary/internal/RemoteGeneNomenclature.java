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
package org.phenotips.vocabulary.internal;

import org.phenotips.vocabulary.Vocabulary;
import org.phenotips.vocabulary.VocabularyExtension;
import org.phenotips.vocabulary.VocabularyTerm;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.solr.client.solrj.util.ClientUtils;
import org.apache.solr.common.params.CommonParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

/**
 * Provides access to the HUGO Gene Nomenclature Committee's GeneNames nomenclature. The prefix is {@code HGNC}.
 *
 * @version $Id$
 * @since 1.0RC1
 */
@Component
@Named("hgnc")
@Singleton
public class RemoteGeneNomenclature implements Vocabulary, Initializable
{
    /** The list of supported categories for this vocabulary. */
    private static final Collection<String> SUPPORTED_CATEGORIES = Collections.singletonList("gene");

    /**
     * Object used to mark in the cache that a term doesn't exist, since null means that the cache doesn't contain the
     * requested entry.
     */
    private static final VocabularyTerm EMPTY_MARKER = new JSONOntologyTerm(null, null);

    private static final String RESPONSE_KEY = "response";

    private static final String DATA_KEY = "docs";

    private static final String LABEL_KEY = "symbol";

    private static final String WILDCARD = "*";

    private static final String DEFAULT_OPERATOR = "AND";

    private static final Map<String, String> QUERY_OPERATORS = new HashMap<>();

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    private String baseServiceURL;

    private String searchServiceURL;

    private String infoServiceURL;

    private String fetchServiceURL;

    /** Performs HTTP requests to the remote REST service. */
    private final CloseableHttpClient client = HttpClients.createSystem();

    @Inject
    private Logger logger;

    /**
     * Cache for the recently accessed terms; useful since the vocabulary rarely changes, so a search should always
     * return the same thing.
     */
    private Cache<VocabularyTerm> cache;

    /** Cache for vocabulary metadata. */
    private Cache<JSONObject> infoCache;

    /** Cache factory needed for creating the term cache. */
    @Inject
    private CacheManager cacheFactory;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.baseServiceURL =
                this.configuration.getProperty("phenotips.ontologies.hgnc.serviceURL", "http://rest.genenames.org/");
            this.searchServiceURL = this.baseServiceURL + "search/";
            this.infoServiceURL = this.baseServiceURL + "info";
            this.fetchServiceURL = this.baseServiceURL + "fetch/";
            this.cache = this.cacheFactory.createNewLocalCache(new CacheConfiguration());
            EntryEvictionConfiguration infoConfig = new LRUEvictionConfiguration(1);
            infoConfig.setTimeToLive(300);
            this.infoCache = this.cacheFactory.createNewLocalCache(new CacheConfiguration(infoConfig));
        } catch (final CacheException ex) {
            throw new InitializationException("Cannot create cache: " + ex.getMessage());
        }
        QUERY_OPERATORS.put("OR", "");
        QUERY_OPERATORS.put(DEFAULT_OPERATOR, DEFAULT_OPERATOR + ' ');
        QUERY_OPERATORS.put("NOT", "-");
    }

    @Override
    public VocabularyTerm getTerm(String id)
    {
        VocabularyTerm result = this.cache.get(id);
        String safeID;
        if (result == null) {
            try {
                safeID = URLEncoder.encode(id, Consts.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                safeID = id.replaceAll("\\s", "");
                this.logger.warn("Could not find the encoding: {}", Consts.UTF_8.name());
            }
            HttpGet method = new HttpGet(this.fetchServiceURL + "symbol/" + safeID);
            method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                JSONObject responseJSON = new JSONObject(response);
                JSONArray docs = responseJSON.getJSONObject(RESPONSE_KEY).getJSONArray(DATA_KEY);
                if (docs.length() == 1) {
                    result = new JSONOntologyTerm(docs.getJSONObject(0), this);
                    this.cache.set(id, result);
                } else {
                    this.cache.set(id, EMPTY_MARKER);
                }
            } catch (IOException | JSONException ex) {
                this.logger.warn("Failed to fetch gene definition: {}", ex.getMessage());
            }
        }
        return (result == EMPTY_MARKER) ? null : result;
    }

    @Override
    public Set<VocabularyTerm> getTerms(Collection<String> ids)
    {
        // FIXME Reimplement with a bunch of async connections fired in parallel
        Set<VocabularyTerm> result = new LinkedHashSet<>();
        for (String id : ids) {
            VocabularyTerm term = getTerm(id);
            if (term != null) {
                result.add(term);
            }
        }
        return result;
    }

    @Override
    public List<VocabularyTerm> search(Map<String, ?> fieldValues)
    {
        return search(fieldValues, Collections.<String, String>emptyMap());
    }

    @Override
    public List<VocabularyTerm> search(Map<String, ?> fieldValues, Map<String, String> queryOptions)
    {
        try {
            HttpGet method =
                new HttpGet(this.searchServiceURL + URLEncoder.encode(generateQuery(fieldValues), Consts.UTF_8.name()));
            method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                JSONObject responseJSON = new JSONObject(response);
                JSONArray docs = responseJSON.getJSONObject(RESPONSE_KEY).getJSONArray(DATA_KEY);
                if (docs.length() >= 1) {
                    List<VocabularyTerm> result = new LinkedList<>();
                    // The remote service doesn't offer any query control, manually select the right range
                    int start = 0;
                    if (queryOptions.containsKey(CommonParams.START)
                        && StringUtils.isNumeric(queryOptions.get(CommonParams.START))) {
                        start = Math.max(0, Integer.parseInt(queryOptions.get(CommonParams.START)));
                    }
                    int end = docs.length();
                    if (queryOptions.containsKey(CommonParams.ROWS)
                        && StringUtils.isNumeric(queryOptions.get(CommonParams.ROWS))) {
                        end = Math.min(end, start + Integer.parseInt(queryOptions.get(CommonParams.ROWS)));
                    }

                    for (int i = start; i < end; ++i) {
                        result.add(new JSONOntologyTerm(docs.getJSONObject(i), this));
                    }
                    return result;
                    // This is too slow, for the moment only return summaries
                    // return getTerms(ids);
                }
            } catch (IOException | JSONException ex) {
                this.logger.warn("Failed to search gene names: {}", ex.getMessage());
            }
        } catch (UnsupportedEncodingException ex) {
            // This will not happen, UTF-8 is always available
        }
        return Collections.emptyList();
    }

    @Override
    public List<VocabularyTerm> search(String input)
    {
        return search(input, 10, null, null);
    }

    @Override
    public List<VocabularyTerm> search(String input, int maxResults, String sort, String customFilter)
    {
        // ignoring sort and customFq
        String formattedQuery = String.format("%s*", input);
        Map<String, Object> fieldValues = new HashMap<>();
        Map<String, String> queryMap = new HashMap<>();
        Map<String, String> rowsMap = new HashMap<>();
        queryMap.put(LABEL_KEY, formattedQuery);
        queryMap.put("alias_symbol", formattedQuery);
        queryMap.put("prev_symbol", formattedQuery);
        fieldValues.put("status", "Approved");
        fieldValues.put(DEFAULT_OPERATOR, queryMap);
        rowsMap.put("rows", Integer.toString(maxResults));

        return this.search(fieldValues, rowsMap);
    }

    @Override
    public long count(Map<String, ?> fieldValues)
    {
        try {
            HttpGet method =
                new HttpGet(this.searchServiceURL + URLEncoder.encode(generateQuery(fieldValues), Consts.UTF_8.name()));
            method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
                String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
                JSONObject responseJSON = new JSONObject(response);
                JSONArray docs = responseJSON.getJSONObject(RESPONSE_KEY).getJSONArray(DATA_KEY);
                return docs.length();
            } catch (IOException | JSONException ex) {
                this.logger.warn("Failed to count matching gene names: {}", ex.getMessage());
            }
        } catch (UnsupportedEncodingException ex) {
            // This will not happen, UTF-8 is always available
        }
        return -1;
    }

    @Override
    public long getDistance(String fromTermId, String toTermId)
    {
        // Flat nomenclature
        return -1;
    }

    @Override
    public long getDistance(VocabularyTerm fromTerm, VocabularyTerm toTerm)
    {
        // Flat nomenclature
        return -1;
    }

    @Override
    public String getIdentifier()
    {
        return "hgncRemote";
    }

    @Override
    public String getName()
    {
        return "HUGO Gene Nomenclature Committee's GeneNames (HGNC)";
    }

    @Override
    public Set<String> getAliases()
    {
        Set<String> result = new HashSet<>();
        result.add(getIdentifier());
        result.add("HGNC");
        return result;
    }

    @Override
    public Collection<String> getSupportedCategories()
    {
        return SUPPORTED_CATEGORIES;
    }

    @Override
    public String getWebsite()
    {
        return "http://www.genenames.org/";
    }

    @Override
    public String getCitation()
    {
        return "HGNC Database, HUGO Gene Nomenclature Committee (HGNC), EMBL Outstation - Hinxton, European"
            + " Bioinformatics Institute, Wellcome Trust Genome Campus, Hinxton, Cambridgeshire, CB10 1SD, UK";
    }

    @Override
    public long size()
    {
        JSONObject info = getInfo();
        return info == null ? -1 : info.getLong("numDoc");
    }

    @Override
    public int reindex(String ontologyUrl)
    {
        // Remote vocabulary, we cannot reindex, but we can clear the local cache
        this.cache.removeAll();
        return 0;
    }

    @Override
    public String getDefaultSourceLocation()
    {
        return this.baseServiceURL;
    }

    @Override
    public String getVersion()
    {
        JSONObject info = getInfo();
        return info == null ? "" : info.getString("lastModified");
    }

    private JSONObject getInfo()
    {
        JSONObject info = this.infoCache.get("");
        if (info != null) {
            return info;
        }
        HttpGet method = new HttpGet(this.infoServiceURL);
        method.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
        try (CloseableHttpResponse httpResponse = this.client.execute(method)) {
            String response = IOUtils.toString(httpResponse.getEntity().getContent(), Consts.UTF_8);
            JSONObject responseJSON = new JSONObject(response);
            this.infoCache.set("", responseJSON);
            return responseJSON;
        } catch (IOException | JSONException ex) {
            this.logger.warn("Failed to get HGNC information: {}", ex.getMessage());
        }
        return null;
    }

    /**
     * Generate a Lucene query from a map of parameters, to be used in the "q" parameter for Solr.
     *
     * @param fieldValues a map with term meta-property values that must be matched by the returned terms; the keys are
     *            property names, like {@code id}, {@code description}, {@code is_a}, and the values can be either a
     *            single value, or a collection of values that can (OR) be matched by the term;
     * @return the String representation of the equivalent Lucene query
     */
    private String generateQuery(Map<String, ?> fieldValues)
    {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, ?> field : fieldValues.entrySet()) {
            processQueryPart(query, field, true);
        }
        return StringUtils.removeStart(query.toString().trim(), DEFAULT_OPERATOR);
    }

    private StringBuilder processQueryPart(StringBuilder query, Map.Entry<String, ?> field, boolean includeOperator)
    {
        if (Collection.class.isInstance(field.getValue()) && ((Collection<?>) field.getValue()).isEmpty()) {
            return query;
        }
        if (Map.class.isInstance(field.getValue())) {
            if (QUERY_OPERATORS.containsKey(field.getKey())) {
                @SuppressWarnings("unchecked")
                Map.Entry<String, Map<String, ?>> subquery = (Map.Entry<String, Map<String, ?>>) field;
                return processSubquery(query, subquery);
            } else {
                this.logger.warn("Invalid subquery operator: {}", field.getKey());
                return query;
            }
        }
        query.append(' ');
        if (includeOperator) {
            query.append(QUERY_OPERATORS.get(DEFAULT_OPERATOR));
        }

        query.append(ClientUtils.escapeQueryChars(field.getKey()));
        query.append(":(");
        if (Collection.class.isInstance(field.getValue())) {
            for (Object value : (Collection<?>) field.getValue()) {
                String svalue = String.valueOf(value);
                if (svalue.endsWith(WILDCARD)) {
                    svalue = ClientUtils.escapeQueryChars(StringUtils.removeEnd(svalue, WILDCARD)) + WILDCARD;
                } else {
                    svalue = ClientUtils.escapeQueryChars(svalue);
                }
                query.append(svalue);
                query.append(' ');
            }
        } else {
            String svalue = String.valueOf(field.getValue());
            if (svalue.endsWith(WILDCARD)) {
                svalue = ClientUtils.escapeQueryChars(StringUtils.removeEnd(svalue, WILDCARD)) + WILDCARD;
            } else {
                svalue = ClientUtils.escapeQueryChars(svalue);
            }
            query.append(svalue);
        }
        query.append(')');
        return query;
    }

    private StringBuilder processSubquery(StringBuilder query, Map.Entry<String, Map<String, ?>> subquery)
    {
        query.append(' ').append(QUERY_OPERATORS.get(subquery.getKey())).append('(');
        for (Map.Entry<String, ?> field : subquery.getValue().entrySet()) {
            processQueryPart(query, field, false);
        }
        query.append(')');
        return query;
    }

    private static class JSONOntologyTerm implements VocabularyTerm
    {
        private JSONObject data;

        private Vocabulary ontology;

        JSONOntologyTerm(JSONObject data, Vocabulary ontology)
        {
            this.data = data;
            this.ontology = ontology;
        }

        @Override
        public String getId()
        {
            return this.data.getString(LABEL_KEY);
        }

        @Override
        public String getName()
        {
            return this.data.getString("name");
        }

        @Override
        public String getTranslatedName()
        {
            return getName();
        }

        @Override
        public String getDescription()
        {
            // No description for gene names
            return "";
        }

        @Override
        public String getTranslatedDescription()
        {
            return getDescription();
        }

        @Override
        public Set<VocabularyTerm> getParents()
        {
            return Collections.emptySet();
        }

        @Override
        public Set<VocabularyTerm> getAncestors()
        {
            return Collections.emptySet();
        }

        @Override
        public Set<VocabularyTerm> getAncestorsAndSelf()
        {
            return Collections.<VocabularyTerm>singleton(this);
        }

        @Override
        public long getDistanceTo(VocabularyTerm other)
        {
            return -1;
        }

        @Override
        public Object get(String name)
        {
            return this.data.get(name);
        }

        @Override
        public Vocabulary getVocabulary()
        {
            return this.ontology;
        }

        @Override
        public String toString()
        {
            return "HGNC:" + getId();
        }

        @Override
        public JSONObject toJSON()
        {
            JSONObject json = new JSONObject();
            json.put("id", this.getId());
            return json;
        }

        @Override
        public Collection<?> getTranslatedValues(String name)
        {
            Object result = get(name);
            if (result instanceof Collection) {
                return (Collection<?>) result;
            }
            return Collections.singleton(result);
        }
    }

    @Override
    public List<VocabularyExtension> getExtensions()
    {
        return null;
    }
}
