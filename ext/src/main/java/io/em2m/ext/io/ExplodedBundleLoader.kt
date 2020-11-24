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
package io.em2m.ext.io

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.policy.keys.ClaimsKeyHandler
import io.em2m.policy.keys.EnvironmentKeyHandler
import io.em2m.ext.Bundle
import io.em2m.ext.BundleSpec
import io.em2m.ext.Extension
import io.em2m.ext.ExtensionSpec
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.PathKeyHandler
import io.em2m.simplex.parser.SimplexModule
import org.slf4j.LoggerFactory
import java.io.File

class ExplodedBundleLoader() {

    private val log = LoggerFactory.getLogger(javaClass)

    private val simplex = Simplex()

    init {
        simplex.keys(BasicKeyResolver(mapOf(
            Key("env", "*") to EnvironmentKeyHandler(),
            Key("claims", "*") to ClaimsKeyHandler(),
            Key("field", "*") to PathKeyHandler(),
            Key("f", "*") to PathKeyHandler())
        ))
    }

    private fun File.isYml(): Boolean = extension == "yml" || extension == "yaml"
    private val jsonMapper = jacksonObjectMapper().registerModule(SimplexModule(simplex))
    private val ymlMapper = ObjectMapper(YAMLFactory())

    fun loadExtension(bundleId: String, bundleDir: File, type: String, spec: ExtensionSpec): Extension {
        val data = spec.data.plus("id" to spec.id)
        return Extension(bundleId, spec.id, type, spec.filter, spec.ref, spec.priority, spec.target, data, bundleDir)
    }

    fun loadBundle(dir: File): Bundle? {
        val ymlFile = File(dir, "ext.yml")
        val jsonFile = File(dir, "ext.json")
        val specFile = when {
            ymlFile.exists() -> ymlFile
            jsonFile.exists() -> jsonFile
            else -> null
        }
        return if (specFile != null) {
            val id = dir.name
            try {
                val tree = readTree(specFile)
                tree.put("id", id)
                val spec = jsonMapper.convertValue<BundleSpec>(tree)
                val extensions = spec.extensions.flatMap { specs ->
                    specs.value.mapNotNull { spec ->
                        try {
                            loadExtension(id, dir, specs.key, spec)
                        } catch (ex: Exception) {
                            log.error("Error loading spec ${spec?.id} from ${specFile}")
                            null
                        }
                    }
                }
                log.info("Loaded bundle $id with ${extensions.size} extensions")
                Bundle(id, spec.filter, dir, extensions)
            } catch (ex: Throwable) {
                ex.printStackTrace()
                null
            }
        } else null
    }

    fun readTree(file: File): ObjectNode {
        return if (file.isYml()) {
            ymlMapper.readTree(file) as ObjectNode
        } else jsonMapper.readTree(file) as ObjectNode
    }

}
