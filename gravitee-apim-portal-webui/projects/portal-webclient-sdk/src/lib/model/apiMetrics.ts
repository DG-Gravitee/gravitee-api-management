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


export interface ApiMetrics { 
    /**
     * Number of applications that have subscribed to this API.
     */
    subscribers?: number;
    /**
     * Number of calls on this API, during last 7 days.
     */
    hits?: number;
    /**
     * Healthcheck ratio over the last 7 days. It\'s a decimal number between 0 and 1.
     */
    health?: number;
}

