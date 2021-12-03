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
package io.gravitee.rest.api.service.filtering;

import io.gravitee.rest.api.model.filtering.FilteredEntities;
import java.util.LinkedHashSet;
import java.util.Set;

public interface FilteringService {
    FilteredEntities<String> getApisOrderByNumberOfSubscriptions(LinkedHashSet<String> ids, Boolean excluded, boolean isAsc);

    // TODO: force LinkedHashSet to preserve order
    FilteredEntities<String> getApplicationsOrderByNumberOfSubscriptions(Set<String> ids, Boolean excluded, boolean isAsc);

    FilteredEntities<String> filterApis(final LinkedHashSet<String> apis, final FilterType filterType, final FilterType excludedFilterType);

    enum FilterType {
        ALL,
        FEATURED,
        MINE,
        STARRED,
        TRENDINGS,
    }
}
