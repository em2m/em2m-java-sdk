/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________
 *
 * Copyright (c) 2013-2017 Elastic M2M Incorporated, All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elastic M2M Incorporated
 *
 * The intellectual and technical concepts contained
 * herein are proprietary to Elastic M2M Incorporated
 * and may be covered by U.S. and Foreign Patents,  patents in
 * process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elastic M2M Incorporated.
 */
package io.em2m.messages.aws

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestStreamHandler
import com.amazonaws.services.lambda.runtime.events.KinesisEvent
import com.amazonaws.services.lambda.runtime.events.SNSEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.flows.Processor
import io.em2m.messages.model.MessageContext
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.InputStream
import java.io.OutputStream

@Suppress("Unused")
class LambdaMessagesRuntime(val processor: Processor<MessageContext>) : RequestStreamHandler {

    private val mapper: ObjectMapper = jacksonObjectMapper().registerModule(SnsDateModule())
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)

    enum class EventType { AwsEvents, Sns, Sqs, Kinesis, Unknown }

    override fun handleRequest(input: InputStream, output: OutputStream, context: Context) {
        val event = mapper.readTree(input) as ObjectNode

        val type = detectEventType(event)
        when (type) {
            EventType.Kinesis -> {
                // Hack to fix the approximateArrivalTimestamp bug in AWS models
                val records = event.get("records") ?: event.get("Records")
                if (records.isArray && records.size() > 0) {
                    // Fix AWS issue where timestamps occasionally come through as a Float instead of Long
                    records.forEach { removeApproximateArrivalTimestamp(it as ObjectNode) }
                }
                handleKinesis(mapper.convertValue(event))
            }
            EventType.Sqs -> handleSqsEvent(mapper.convertValue(event))
            EventType.Sns -> handleSnsEvent(mapper.convertValue(event))
            else -> handleUnknownEvent(event)
        }
    }

    private fun handleKinesis(kinesisEvent: KinesisEvent) {
        kinesisEvent.records.forEach { record ->
            val kinesis = record.kinesis
            val stream = kinesis.data.array().inputStream()
            val env = hashMapOf<String, Any?>(
                    "EventSource" to record.eventSource,
                    "EventSourceArn" to record.eventSourceARN,
                    "EventName" to record.eventName,
                    "AwsRegion" to record.awsRegion,
                    "InvokeIdentityArn" to record.invokeIdentityArn,
                    "Channel" to record.eventSourceARN
            )
            val eventId = record.eventID
            val ctx = MessageContext(stream, environment = env, eventId = eventId)
            processor.process(ctx)
        }
    }

    private fun handleSqsEvent(sqsEvent: SQSEvent) {
        sqsEvent.records.forEach { record ->
            val event = record.body
            if (record != null) {
                val context = MessageContext(event.byteInputStream())
                processor.process(context)
            }
        }
    }

    private fun handleSnsEvent(snsEvent: SNSEvent) {
        snsEvent.records.forEach { record ->
            val event = record.sns.message
            if (event != null) {
                val context = MessageContext(event.byteInputStream())
                processor.process(context)
            }
        }
    }

    private fun handleUnknownEvent(event: ObjectNode) {
        println("Unknown Event: " + mapper.writeValueAsString(event))
    }

    fun removeApproximateArrivalTimestamp(record: ObjectNode) {
        (record.get("kinesis") as ObjectNode).remove("approximateArrivalTimestamp")
    }

    private fun detectEventType(event: ObjectNode): EventType {
        //
        val source = event.path("source").asText(null) ?: event.path("Source").asText(null)
        if (source == "aws.messages") {
            return EventType.AwsEvents
        }
        val records = event.get("records") ?: event.get("Records")
        if (records.isArray && records.size() > 0) {
            val first = records.first()
            return when {
                first.has("kinesis") -> EventType.Kinesis
                first.has("Sns") || first.has("sns") -> EventType.Sns
                first.has("eventSource") && first.get("eventSource").asText() == "aws:sqs" -> EventType.Sqs
                else -> EventType.Unknown
            }
        }
        return EventType.Unknown
    }

    class SnsDateModule : SimpleModule() {
        init {
            addDeserializer(DateTime::class.java, SnsDateDeserializer())
        }
    }

    class SnsDateDeserializer : JsonDeserializer<DateTime>() {

        override fun deserialize(jsonParser: JsonParser?, deserializationContext: DeserializationContext?): DateTime? {
            val formatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            val date = jsonParser?.text
            if (date != null) {
                return formatter.parseDateTime(date)
            } else {
                return null
            }
        }
    }
}