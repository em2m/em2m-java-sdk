package io.em2m.messages.aws

import com.amazonaws.auth.AWSCredentialsProvider
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.services.kinesis.clientlibrary.exceptions.ShutdownException
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessor
import com.amazonaws.services.kinesis.clientlibrary.interfaces.IRecordProcessorCheckpointer
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.InitialPositionInStream
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.KinesisClientLibConfiguration
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.ShutdownReason
import com.amazonaws.services.kinesis.clientlibrary.lib.worker.Worker
import com.amazonaws.services.kinesis.leases.exceptions.InvalidStateException
import com.amazonaws.services.kinesis.model.Record
import io.em2m.messages.model.MessageContext
import io.em2m.flows.Processor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.InetAddress
import java.util.*

class KinesisRuntime(config: KinesisRuntime.Config, val processor: Processor<MessageContext>) : Runnable {

    val log: Logger = LoggerFactory.getLogger(javaClass)

    data class Config(
            var streamName: String,
            var initialPosition: InitialPositionInStream = InitialPositionInStream.LATEST,
            var applicationName: String,
            var credentialsProvider: AWSCredentialsProvider? = null
    )

    val credentialsProvider = config.credentialsProvider ?: DefaultAWSCredentialsProviderChain()
    val initialPosition = config.initialPosition
    val streamName = config.streamName
    val applicationName = config.applicationName

    override fun run() {
        val workerId = InetAddress.getLocalHost().canonicalHostName + ":" + UUID.randomUUID()
        log.info("Initializing Kinesis client.  Initial Position = " + initialPosition.name)
        val kinesisClientLibConfig = KinesisClientLibConfiguration(applicationName, streamName, credentialsProvider, workerId)
                .withInitialPositionInStream(initialPosition)
        val processor = RecordProcessor()
        Worker({ processor }, kinesisClientLibConfig).run()
    }

    internal inner class RecordProcessor : IRecordProcessor {

        var running = false

        override fun initialize(shardId: String) {
            running = true
        }

        override fun processRecords(records: List<Record>, checkPointer: IRecordProcessorCheckpointer) {

            log.info("Received records from stream: Count = " + records.size)

           records.forEach { record ->
               val stream = record.data.array().inputStream()
               val env = hashMapOf<String, Any?>(
                       "EventSource" to "kinesis",
                       "PartitionKey" to record.partitionKey,
                       "SequenceNumber" to record.sequenceNumber,
                       "ArrivalTimestamp" to record.approximateArrivalTimestamp
               )
               val ctx = MessageContext(stream, environment = env )
               processor.process(ctx).subscribe()
           }

            log.info("Batch complete")

            try {
                checkPointer.checkpoint()
            } catch (e: InvalidStateException) {
                log.error("Invalid state exception:", e)
            } catch (e: ShutdownException) {
                log.error("Error performing checkpoint on stream")
            }
        }

        override fun shutdown(iRecordProcessorCheckpointer: IRecordProcessorCheckpointer, shutdownReason: ShutdownReason) {
            log.info("Shutting down Kinesis client")
        }

    }

}
