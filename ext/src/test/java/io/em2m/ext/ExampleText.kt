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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.typesafe.config.ConfigFactory
import io.em2m.policy.model.Claims
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.PathKeyHandler
import org.junit.Before
import org.junit.Test
import javax.inject.Inject
import kotlin.test.assertEquals

class ExampleText {

    private lateinit var modelService: DeviceModelService
    private lateinit var reportService: ReportService
    private val ctx = mapOf("claims" to Claims(mapOf(
        "entitlements" to listOf("web", "product.ams"),
        "brand" to "em2m")))

    @Before
    fun setup() {
        val config = ConfigFactory.parseMap(mapOf("data.ext.dir" to "src/test/ext"))
        val simplex = Simplex()
        simplex.keys(BasicKeyResolver(
            mapOf(Key("claims", "*") to PathKeyHandler("claims"))))
        val extService = ExtensionServiceImpl(config, simplex)

        modelService = DeviceModelService(extService)
        reportService = ReportService(extService)
    }

    @Test
    fun testDeviceModels() {
        val models = modelService.findDeviceModels(ctx)
        assertEquals(2, models.size)
    }

    @Test
    fun testDeviceManufacturer() {
        val models = modelService.findDeviceManufacturers(ctx)
        assertEquals(1, models.size)
        assertEquals("pui", models[0].id)
    }

    @Test
    fun testReports() {
        val reports = reportService.findReports(ctx)
        assertEquals(2, reports.size)
    }

    class ReportService @Inject constructor(private val extensionService: ExtensionService) {

        private val mapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule())

        fun findReports(ctx: Map<String, Any?> = emptyMap()): List<Report> {
            return extensionService
                .findExtensions({ it.type == "io.em2m.analytics.ext.Report" }, ctx)
                .mapNotNull { ext ->
                    ext.instance(mapper, Report::class.java)
                }
        }
    }

    data class Report(val id: String, val title: String, val description: String)

    class DeviceModelService @Inject constructor(private val extensionService: ExtensionService) {

        val mapper = jacksonObjectMapper()

        fun findDeviceModels(ctx: Map<String, Any?> = emptyMap()): List<DeviceModel> {
            return extensionService
                .findExtensions({ it.type == "io.em2m.device.ext.Model" }, ctx)
                .mapNotNull { ext ->
                    ext.instance(mapper, DeviceModel::class.java)
                }
        }

        fun findDeviceManufacturers(ctx: Map<String, Any?> = emptyMap()): List<DeviceManufacturer> {
            return extensionService
                .findExtensions({ it.type == "io.em2m.device.ext.Manufacturer" }, ctx)
                .mapNotNull { ext ->
                    ext.instance(mapper, DeviceManufacturer::class.java)
                }
        }

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DeviceManufacturer(val id: String, val name: String)

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class DeviceModel(
        val id: String,
        val name: String,
        val carriers: List<String>,
        val family: String,
        val manufacturer: String,
        val battery: Boolean = false)
}

