package io.em2m.messages.aws

import com.amazonaws.services.sqs.AmazonSQS
import com.amazonaws.services.sqs.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.flows.Processor
import io.em2m.messages.model.MessageContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import com.amazonaws.services.sqs.model.Message as SqsMessage


class SqsPollingRuntime @Inject constructor(
        val sqs: AmazonSQS,
        val processor: Processor<MessageContext>,
        val objectMapper: ObjectMapper = jacksonObjectMapper()) {

    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private var running: Boolean = false
    val executor: ExecutorService = Executors.newCachedThreadPool()

    fun getOrCreateQueue(name: String): String {
        try {
            return sqs.createQueue(CreateQueueRequest(name)).queueUrl
        } catch (e: AmazonSQSException) {
            if (e.errorCode != "QueueAlreadyExists") {
                throw e
            } else {
                return sqs.getQueueUrl(GetQueueUrlRequest(name)).queueUrl
            }
        }
    }

    fun shutdown() {
        this.running = false
        executor.shutdown()
    }

    fun shutdownNow() {
        this.running = false
        executor.shutdownNow()
    }

    fun subscribe(flow: String, queueUrl: String): SqsPollingRuntime {
        executor.submit(Job(flow, queueUrl))
        return this
    }

    fun publishMessage(queueUrl: String, message: String) {
        val msg = objectMapper.writeValueAsString(message)
        val sendRequest = SendMessageRequest(queueUrl, msg)
        val sendResult = sqs.sendMessage(sendRequest)
        //print MessageId of message published to SNS topic
        System.out.println("MessageId - " + sendResult.messageId)
    }

    inner class Job(val flow: String, val queueUrl: String) : Runnable {

        override fun run() {
            while (running) {
                try {
                    val receiveMessageResult = sqs.receiveMessage(
                            ReceiveMessageRequest(queueUrl).withMaxNumberOfMessages(1).withWaitTimeSeconds(20))
                    if (receiveMessageResult.messages.isNotEmpty()) {
                        log.info("Received ${receiveMessageResult.messages.size} messages")
                        receiveMessageResult.messages.forEach { sqsMessage ->
                            val ctx = convert(sqsMessage)
                            processor.process(flow, ctx).doOnCompleted {
                                sqs.deleteMessage(DeleteMessageRequest(queueUrl, sqsMessage.receiptHandle))
                                }.subscribe()
                            }
                        }
                } catch (error: Exception) {
                    log.error("Error receiving messages from SQS", error)
                }
            }
        }

        fun convert(sqsMessage: SqsMessage): MessageContext {
            val ctx = MessageContext(environment = createEnvironment(sqsMessage))
            ctx.inputStream = sqsMessage.body.byteInputStream()
            /*
            Need to auto-detect and unwrap SNS messages
            val str = if (body.has("TopicArn")) {
                // sns over sqs
            } else {
                body.asText()
            }
            */
            return ctx
        }

        private fun createEnvironment(sqsMessage: SqsMessage): MutableMap<String, Any?> {
            val currentTime = Date()
            return mapOf(
                    "QueueUrl" to queueUrl,
                    "CurrentTime" to currentTime
            ).toMutableMap()
        }

    }

}