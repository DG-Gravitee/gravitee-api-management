/**
 * Gravitee.io Portal Rest API
 * API dedicated to the devportal part of Gravitee
 *
 * The version of the OpenAPI document: 3.14.0-SNAPSHOT
 * Contact: contact@graviteesource.com
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */
import { RatingSummary } from './ratingSummary';
import { User } from './user';
import { Page } from './page';
import { ApiLinks } from './apiLinks';
import { Plan } from './plan';


/**
 * Describes an API. 
 */
export interface Api { 
    /**
     * Unique identifier of an API.
     */
    id: string;
    /**
     * Name of the API.
     */
    name: string;
    /**
     * Version of the API.
     */
    version: string;
    /**
     * Description of the API.
     */
    description: string;
    /**
     * Whether or not the API is in draft.
     */
    draft?: boolean;
    /**
     * Whether or not the API is public.
     */
    _public?: boolean;
    /**
     * Whether or not the API is running.
     */
    running?: boolean;
    /**
     * List of all the avaible endpoints to call the API.
     */
    entrypoints?: Array<string>;
    /**
     * List of labels linked to this API.
     */
    labels?: Array<string>;
    owner: User;
    /**
     * create date and time.
     */
    created_at?: Date;
    /**
     * Last update date and time.
     */
    updated_at?: Date;
    /**
     * List of categories this API belongs to.
     */
    categories?: Array<string>;
    rating_summary?: RatingSummary;
    _links?: ApiLinks;
    /**
     * Only returned with *_/apis/{apiId}*. Need *include* query param to contain \'pages\'.  The documentation pages of this API. Same as *_/apis/{apiId}/pages*. 
     */
    pages?: Array<Page>;
    /**
     * Only returned with *_/apis/{apiId}*. Need *include* query param to contain \'plans\'.  The plans of this API. Same as *_/apis/{apiId}/plans*. 
     */
    plans?: Array<Plan>;
}

