/**
 * ELASTIC M2M Inc. CONFIDENTIAL
 * __________________
 *
 * Copyright (c) 2013-2020 Elastic M2M Incorporated, All Rights Reserved.
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
package io.em2m.ext

import com.typesafe.config.Config
import io.em2m.ext.io.ExplodedBundleLoader
import io.em2m.simplex.Simplex
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.*
import java.nio.file.WatchEvent
import javax.inject.Inject
import kotlin.concurrent.thread

interface ExtensionService {
    fun findExtensions(predicate: (Extension) -> Boolean, context: Map<String, Any?>): List<Extension>
    fun findExtensions(type: String): List<Extension>
    fun getResource(bundleId: String, path: String): URL?
    fun startMonitoring()
    fun stopMonitoring()
}

class ExtensionServiceImpl @Inject constructor(val config: Config, val simplex: Simplex) : ExtensionService {

    private val log: Logger = LoggerFactory.getLogger(javaClass)
    private val dir = File(config.getString("data.ext.dir"))
    private val monitoringEnabled =
        if (config.hasPath("ext.monitoring.enabled")) config.getBoolean("ext.monitoring.enabled") else false

    private var running = false

    private var bundles: List<Bundle> = emptyList()
    var types: List<String> = emptyList()
    val loader = ExplodedBundleLoader()

    init {
        reload()
    }

    override fun findExtensions(predicate: (Extension) -> Boolean, context: Map<String, Any?>): List<Extension> {
        return bundles
            .filter { it.filter(context) }
            .flatMap { it.findExtension(predicate, context) }
            .sortedByDescending { it.priority }
    }

    override fun findExtensions(type: String): List<Extension> {
        return bundles
            .flatMap { it.findExtension(type) }
            .sortedByDescending { it.priority }
    }

    override fun getResource(bundleId: String, path: String): URL? {
        val bundle = bundles.find { it.id == bundleId }
        if (bundle != null) {
            val file = File(bundle.dir, path)
            if (file.exists()) {
                return file.toURI().toURL()
            }
        }
        return null
    }

    private fun reload() {
        log.info("Loading Bundles")
        val dirs = dir.listFiles()?.filter { it.isDirectory }
        bundles = dirs?.mapNotNull { loader.loadBundle(it) } ?: emptyList()
        val ext = bundles.flatMap { it.extensions }
        types = ext.map { it.type }.distinct()
    }

    override fun startMonitoring() {
        log.debug("Ext Monitoring disabled")
        if (!monitoringEnabled) {
            return
        }
        log.info("Monitoring starting")
        try {
            val watcher = FileSystems.getDefault().newWatchService()
            dir.toPath().register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
            running = true
            thread(start = true, isDaemon = true, name = "ExtensionServiceWatcher") {
                while (running) {
                    try {

                        val key = watcher.take()
                        val events = key.pollEvents()
                        var reloadRequired = false
                        events.forEach { event ->
                            if (event.kind() != OVERFLOW) {
                                @Suppress("UNCHECKED_CAST")
                                val ev: WatchEvent<Path> = event as WatchEvent<Path>
                                val filename: Path = ev.context()
                                //if (filename.endsWith("yml") || filename.endsWith("json")) {
                                reloadRequired = true
                                //}
                                log.debug("File modified: $filename")
                            }
                        }
                        key.reset()
                        if (reloadRequired) {
                            log.info("Changes detected.  Reloading bundles")
                            reload()
                        }
                    } catch (x: InterruptedException) {
                        running = false
                    }
                }
                log.info("Ext Monitoring complete")
            }
        } catch (ex: Exception) {
            log.error("Error starting monitoring")
        }
    }

    override fun stopMonitoring() {
        log.debug("Stopping monitoring")
        running = false
    }
}
