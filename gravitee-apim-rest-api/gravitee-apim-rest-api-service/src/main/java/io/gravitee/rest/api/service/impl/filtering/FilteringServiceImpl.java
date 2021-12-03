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
package io.gravitee.rest.api.service.impl.filtering;

import io.gravitee.rest.api.model.SubscriptionEntity;
import io.gravitee.rest.api.model.SubscriptionStatus;
import io.gravitee.rest.api.model.TopApiEntity;
import io.gravitee.rest.api.model.api.ApiEntity;
import io.gravitee.rest.api.model.application.ApplicationListItem;
import io.gravitee.rest.api.model.filtering.FilterableItem;
import io.gravitee.rest.api.model.filtering.FilteredEntities;
import io.gravitee.rest.api.model.subscription.SubscriptionQuery;
import io.gravitee.rest.api.service.ApplicationService;
import io.gravitee.rest.api.service.RatingService;
import io.gravitee.rest.api.service.SubscriptionService;
import io.gravitee.rest.api.service.TopApiService;
import io.gravitee.rest.api.service.filtering.FilteringService;
import io.gravitee.rest.api.service.impl.AbstractService;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Florent CHAMFROY (florent.chamfroy at graviteesource.com)
 * @author GraviteeSource Team
 */
@Component
public class FilteringServiceImpl extends AbstractService implements FilteringService {

    @Autowired
    SubscriptionService subscriptionService;

    @Autowired
    RatingService ratingService;

    @Autowired
    TopApiService topApiService;

    @Autowired
    ApplicationService applicationService;

    /**
     * Filters and sorts input entities by number of subscriptions.
     *
     * @param ids    Can be {@link ApiEntity} or {@link ApplicationListItem}
     * @param excluded If set to true, only entities without subscriptions are returned. Else, only entities with subscriptions are returned.
     * @return a {@link FilteredEntities} object with the filtered and sorted list of ids and a metadata map.
     */
    private FilteredEntities<String> getEntitiesOrderByNumberOfSubscriptions(
        LinkedHashSet<String> ids,
        Boolean excluded,
        boolean isAsc,
        Function<SubscriptionEntity, String> getItemFunction,
        SubscriptionQuery subscriptionQuery
    ) {
        if (ids == null || ids.isEmpty()) {
            return new FilteredEntities<>(Collections.emptyList(), new HashMap<>());
        }

        // group by ids
        // TODO: add method in repository to compute ranking and return the reference we need api/app
        // After that, just concat ranking with ids like in getRatedApis
        Map<String, Long> subscribedItemsWithCount = subscriptionService
            .search(subscriptionQuery)
            .stream()
            .collect(Collectors.groupingBy(getItemFunction, Collectors.counting()));

        // link an item with its nb of subscriptions
        Map<String, Long> itemsWithCount = new HashMap<>();
        Map<String, Map<String, Object>> itemsMetadata = new HashMap<>();
        Map<String, Object> subscriptionsMetadata = new HashMap<>();
        itemsMetadata.put("subscriptions", subscriptionsMetadata);
        ids.forEach(
            id -> {
                Long itemSubscriptionsCount = subscribedItemsWithCount.get(id);
                if ((excluded == null) || (!excluded && itemSubscriptionsCount != null) || (excluded && itemSubscriptionsCount == null)) {
                    //creation of a map which will be sorted to retrieve ids in the right order
                    itemsWithCount.put(id, itemSubscriptionsCount == null ? 0L : itemSubscriptionsCount);

                    //creation of a metadata map
                    subscriptionsMetadata.put(id, itemSubscriptionsCount == null ? 0L : itemSubscriptionsCount);
                }
            }
        );

        // order the list
        Comparator<Map.Entry<FilterableItem, Long>> comparingByValue = Map.Entry.comparingByValue();
        if (!isAsc) {
            comparingByValue = comparingByValue.reversed();
        }

        return new FilteredEntities(
            itemsWithCount
                .entrySet()
                .stream()
                //                .sorted(
                //                    Map.Entry
                //                        .<FilterableItem, Long>comparingByValue()
                //                        .reversed()
                //                        .thenComparing(
                //                            Map.Entry.comparingByKey(Comparator.comparing(FilterableItem::getName, String.CASE_INSENSITIVE_ORDER))
                //                        )
                //                )
                .map(Map.Entry::getKey)
                .collect(Collectors.toList()),
            itemsMetadata
        );
    }

    @Override
    public FilteredEntities<String> getApisOrderByNumberOfSubscriptions(LinkedHashSet<String> ids, Boolean excluded, boolean isAsc) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApis(ids);
        return this.getEntitiesOrderByNumberOfSubscriptions(
                ids,
                excluded,
                isAsc,
                (SubscriptionEntity sub) -> sub.getApi(),
                subscriptionQuery
            );
    }

    @Override
    public FilteredEntities<String> getApplicationsOrderByNumberOfSubscriptions(Set<String> ids, Boolean excluded, boolean isAsc) {
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setStatuses(Arrays.asList(SubscriptionStatus.ACCEPTED, SubscriptionStatus.PAUSED));
        subscriptionQuery.setApplications(ids);
        return this.getEntitiesOrderByNumberOfSubscriptions(
                //TODO: update interface with LinkedHashSet
                new LinkedHashSet<>(ids),
                excluded,
                isAsc,
                (SubscriptionEntity sub) -> sub.getApplication(),
                subscriptionQuery
            );
    }

    @Override
    public FilteredEntities<String> filterApis(
        final LinkedHashSet<String> apis,
        final FilterType filterType,
        final FilterType excludedFilterType
    ) {
        final FilterType filter = excludedFilterType == null ? filterType : excludedFilterType;
        final boolean excluded = excludedFilterType != null;
        if (filter != null) {
            switch (filter) {
                case MINE:
                    if (isAuthenticated()) {
                        return getCurrentUserSubscribedApis(apis, excluded);
                    } else {
                        return new FilteredEntities<>(Collections.emptyList(), null);
                    }
                case STARRED:
                    if (ratingService.isEnabled()) {
                        return getRatedApis(apis, excluded);
                    } else {
                        return new FilteredEntities<>(Collections.emptyList(), null);
                    }
                case TRENDINGS:
                    return getApisOrderByNumberOfSubscriptions(apis, excluded, false);
                case FEATURED:
                    return getTopApis(apis, excluded);
                case ALL:
                default:
                    break;
            }
        }

        // Apis is already ordered
        return new FilteredEntities<>(apis, null);
    }

    private FilteredEntities<String> getTopApis(LinkedHashSet<String> apis, boolean excluded) {
        Map<String, Integer> topApiIdAndOrderMap = topApiService
            .findAll()
            .stream()
            .collect(Collectors.toMap(TopApiEntity::getApi, TopApiEntity::getOrder));

        if (topApiIdAndOrderMap.isEmpty()) {
            if (excluded) {
                return new FilteredEntities<>(apis, null);
            } else {
                return new FilteredEntities<>(Collections.emptyList(), null);
            }
        } else if (excluded) {
            return new FilteredEntities<>(
                apis.stream().filter(api -> (!topApiIdAndOrderMap.containsKey(api))).collect(Collectors.toList()),
                null
            );
        } else {
            return new FilteredEntities<>(
                apis
                    .stream()
                    .filter(api -> topApiIdAndOrderMap.containsKey(api))
                    .sorted(Comparator.comparing(o -> topApiIdAndOrderMap.get(o)))
                    .collect(Collectors.toList()),
                null
            );
        }
    }

    private FilteredEntities<String> getRatedApis(LinkedHashSet<String> apis, boolean excluded) {
        Set<String> ratings = ratingService.computeRanking(apis);

        if (excluded) {
            // remove apis rated to apis already sorted by name
            apis.removeAll(ratings);
            return new FilteredEntities<>(apis, null);
        } else {
            // add apis already sorted by name to apis sorted by rate
            ratings.addAll(apis);
            return new FilteredEntities(ratings, null);
        }
    }

    private FilteredEntities<String> getCurrentUserSubscribedApis(LinkedHashSet<String> apis, boolean excluded) {
        //get Current user applications
        List<String> currentUserApplicationsId = applicationService
            .findByUser(getAuthenticatedUser().getUsername())
            .stream()
            .map(ApplicationListItem::getId)
            .collect(Collectors.toList());

        //find all subscribed apis for these applications
        SubscriptionQuery subscriptionQuery = new SubscriptionQuery();
        subscriptionQuery.setApplications(currentUserApplicationsId);
        List<String> subscribedApis = subscriptionService
            .search(subscriptionQuery)
            .stream()
            .map(SubscriptionEntity::getApi)
            .distinct()
            .collect(Collectors.toList());

        //filter apis list with subscribed apis list
        return new FilteredEntities<>(
            apis
                .stream()
                .filter(api -> (!excluded && subscribedApis.contains(api)) || (excluded && !subscribedApis.contains(api)))
                // .sorted((a1, a2) -> String.CASE_INSENSITIVE_ORDER.compare(a1.getName(), a2.getName()))
                .collect(Collectors.toList()),
            null
        );
    }
}
