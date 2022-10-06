package com.projectronin.interop.validation.client.auth

import com.projectronin.interop.common.auth.Authentication

/**
 * Service responsible for retrieving [Authentication] to call the Data Ingestion Validation Error Management Server.
 */
interface ValidationAuthenticationService {
    /**
     * Retrieves an [Authentication] to call the Data Ingestion Validation Error Management Server.
     */
    fun getAuthentication(): Authentication
}
