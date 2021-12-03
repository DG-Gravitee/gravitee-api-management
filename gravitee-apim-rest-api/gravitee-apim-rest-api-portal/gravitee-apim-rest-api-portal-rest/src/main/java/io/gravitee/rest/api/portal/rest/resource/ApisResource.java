/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.portal.rest.resource;

import io.gravitee.common.http.MediaType;
import io.gravitee.repository.exceptions.TechnicalException;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.api.ApiQuery;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.portal.rest.mapper.ApiMapper;
import io.gravitee.rest.api.portal.rest.model.Api;
import io.gravitee.rest.api.portal.rest.model.FilterApiQuery;
import io.gravitee.rest.api.portal.rest.resource.param.ApisParam;
import io.gravitee.rest.api.portal.rest.resource.param.PaginationParam;
import io.gravitee.rest.api.portal.rest.security.RequirePortalAuth;
import io.gravitee.rest.api.portal.rest.utils.PortalApiLinkHelper;
import io.gravitee.rest.api.service.CategoryService;
import io.gravitee.rest.api.service.filtering.FilteringService;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
public class ApisResource extends AbstractResource {

    @Context
    private ResourceContext resourceContext;

    @Inject
    private ApiMapper apiMapper;

    @Inject
    private FilteringService filteringService;

    @Inject
    private CategoryService categoryService;

    @GET
    @Path("_categories")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response listCategories(@BeanParam ApisParam apisParam) {
        FilteredEntities<String> filteredApis = findAndFilterApi(apisParam);
        Set<String> categories = apiService.listCategories(filteredApis.getFilteredItems());
        return Response.ok(new DataResponse().data(categories)).build();
    }

    private FilteredEntities<String> findAndFilterApi(ApisParam apisParam) {
        LinkedHashSet<String> apis = apiService.findPublishedIdsByUser(getAuthenticatedUserOrNull(), createQueryFromParam(null));
        return filteringService.filterApis(apis, convert(apisParam.getFilter()), convert(apisParam.getExcludedFilter()));
    }

    private FilteringService.FilterType convert(FilterApiQuery filter) {
        return filter != null ? FilteringService.FilterType.valueOf(filter.name()) : null;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response getApis(@BeanParam PaginationParam paginationParam, @BeanParam ApisParam apisParam) {
        boolean isCategoryMode = apisParam.isCategoryMode();

        String categoryFilter = apisParam.getCategory();
        if (!isCategoryMode && categoryFilter != null) {
            apisParam.setCategory(null);
        }

        LinkedHashSet<String> apis = apiService.findPublishedIdsByUser(getAuthenticatedUserOrNull(), createQueryFromParam(null));

        FilteredEntities<String> filteredApis = filteringService.filterApis(
            apis,
            convert(apisParam.getFilter()),
            convert(apisParam.getExcludedFilter())
        );
        Collection<String> filteredApisList = filteredApis.getFilteredItems();

        Stream<String> resultStream = filteredApisList.stream();

        if (filteredApisList.size() > 0 && apisParam.getPromoted() != null) {
            //By default, the promoted API is the first of the list;
            String promotedApiId = filteredApisList.iterator().next();

            if (isCategoryMode) {
                // If apis are searched in a category, looks for the category highlighted API (HL API) and if this HL API is in the searchResult.
                // If it is, then the HL API becomes the promoted API
                String highlightedApiId = this.categoryService.findById(categoryFilter).getHighlightApi();
                if (highlightedApiId != null) {
                    Optional<String> highlightedApiInResult = filteredApisList
                        .stream()
                        .filter(api -> api.equals(highlightedApiId))
                        .findFirst();
                    if (highlightedApiInResult.isPresent()) {
                        promotedApiId = highlightedApiInResult.get();
                    }
                }
            }
            String finalPromotedApiId = promotedApiId;
            if (apisParam.getPromoted() == Boolean.TRUE) {
                // Only the promoted API has to be returned
                resultStream = resultStream.filter(api -> api.equals(finalPromotedApiId));
            } else if (apisParam.getPromoted() == Boolean.FALSE) {
                // All filtered API except the promoted API have to be returned
                if (!isCategoryMode && categoryFilter != null) {
                    // TODO: Manage this case...
                    //  resultStream = resultStream.filter(api -> api.getCategories() != null && api.getCategories().contains(categoryFilter));
                }
                resultStream = resultStream.filter(api -> !api.equals(finalPromotedApiId));
            }
        }

        //        List<Api> apisList = resultStream.map(apiMapper::convert).map(this::addApiLinks).collect(Collectors.toList());
        List<String> apisList = resultStream.collect(Collectors.toList());
        return createListResponse(apisList, paginationParam, filteredApis.getMetadata());
    }

    @POST
    @Path("_search")
    @Produces(MediaType.APPLICATION_JSON)
    @RequirePortalAuth
    public Response searchApis(
        @NotNull(message = "Input must not be null.") @QueryParam("q") String query,
        @BeanParam PaginationParam paginationParam
    ) {
        LinkedHashSet<String> apis = apiService.findPublishedIdsByUser(getAuthenticatedUserOrNull(), createQueryFromParam(null));

        Map<String, Object> filters = new HashMap<>();
        filters.put("api", apis);

        try {
            Collection<ApiEntity> apisList = apiService.search(query, filters);
            return createListResponse(new ArrayList(apisList), paginationParam);
        } catch (TechnicalException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build();
        }
    }

    @Override
    protected List populatePage(List paginatedList) {
        return (List) paginatedList
            .stream()
            .map(
                o -> {
                    Api api = null;
                    if (!(o instanceof Api)) {
                        if (o instanceof String) {
                            api = apiMapper.convert(apiService.findById((String) o));
                        } else if (o instanceof ApiEntity) {
                            api = apiMapper.convert((ApiEntity) o);
                        }
                    } else {
                        api = (Api) o;
                    }
                    return addApiLinks(api);
                }
            )
            .collect(Collectors.toList());
    }

    private ApiQuery createQueryFromParam(ApisParam apisParam) {
        final ApiQuery apiQuery = new ApiQuery();
        if (apisParam != null) {
            apiQuery.setContextPath(apisParam.getContextPath());
            apiQuery.setLabel(apisParam.getLabel());
            apiQuery.setName(apisParam.getName());
            apiQuery.setTag(apisParam.getTag());
            apiQuery.setVersion(apisParam.getVersion());
            if (apisParam.getCategory() != null) {
                apiQuery.setCategory(apisParam.getCategory());
            }
        }
        return apiQuery;
    }

    private Api addApiLinks(Api api) {
        final OffsetDateTime updatedAt = api.getUpdatedAt();
        Date updateDate = null;
        if (updatedAt != null) {
            long epochMilli = updatedAt.toInstant().toEpochMilli();
            updateDate = new Date(epochMilli);
        }
        return api.links(apiMapper.computeApiLinks(PortalApiLinkHelper.apisURL(uriInfo.getBaseUriBuilder(), api.getId()), updateDate));
    }

    @Path("{apiId}")
    public ApiResource getApiResource() {
        return resourceContext.getResource(ApiResource.class);
    }
}
