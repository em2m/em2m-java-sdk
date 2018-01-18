/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________

 * Copyright (c) 2013-2016 Elastic M2M Incorporated, All Rights Reserved.

 * NOTICE:  All information contained herein is, and remains
 * the property of Elastic M2M Incorporated

 * The intellectual and technical concepts contained
 * herein are proprietary to Elastic M2M Incorporated
 * and may be covered by U.S. and Foreign Patents,  patents in
 * process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elastic M2M Incorporated.
 */
package io.em2m.actions.model

import com.fasterxml.jackson.annotation.JsonInclude
import io.em2m.flows.FlowNotFound

data class ActionError(val status: Int = 500, val code: Int = 0, val messages: List<Message> = emptyList()) : Error() {

    companion object {

        fun convert(throwable: Throwable, context: ActionContext): ActionError {
            return when (throwable) {
                is FlowNotFound -> ActionError(Status.NOT_FOUND, 0, listOf(Message("Action ${context.actionName} not found.")))
                is IllegalArgumentException -> ActionError(Status.CONFLICT, 0, listOf(Message("Action ${context.actionName} not found.")))
                else -> ActionError(Status.INTERNAL_SERVER_ERROR, messages = listOf(Message(throwable.message)))
            }
        }
    }

    object Status {
        val BAD_REQUEST = 400
        val NOT_AUTHORIZED = 401
        val FORBIDDEN = 403
        val NOT_FOUND = 404
        val CONFLICT = 409
        val INTERNAL_SERVER_ERROR = 500
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    data class Message(val user: String? = null, val detail: String? = null, val developer: String? = null)
}
