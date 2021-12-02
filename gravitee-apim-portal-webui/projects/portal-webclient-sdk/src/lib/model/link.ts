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


export interface Link { 
    /**
     * Name of the link.
     */
    name?: string;
    /**
     * target of the link. Can be either a URL id resourceType is \'external\' or a UUID if resourceType is \'page\' or \'category\'.
     */
    resourceRef?: string;
    /**
     * the type of the link.
     */
    resourceType?: Link.ResourceTypeEnum;
    /**
     * true if resourceType is \'page\' and resourceRef is the id of a folder.
     */
    folder?: boolean;
}
export namespace Link {
    export type ResourceTypeEnum = 'external' | 'page' | 'category';
    export const ResourceTypeEnum = {
        External: 'external' as ResourceTypeEnum,
        Page: 'page' as ResourceTypeEnum,
        Category: 'category' as ResourceTypeEnum
    };
}


