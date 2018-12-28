/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.google.common.collect.List
import com.squareup.wire.schema.internal.parser.RpcElement

class Rpc private constructor(private val location: Location, private val name: String, private val documentation: String, private val requestTypeElement: String,
                              private val responseTypeElement: String, private val requestStreaming: Boolean, private val responseStreaming: Boolean, private val options: Options) {
    private var requestType: ProtoType? = null
    private var responseType: ProtoType? = null

    fun location(): Location {
        return location
    }

    fun name(): String {
        return name
    }

    fun documentation(): String {
        return documentation
    }

    fun requestType(): ProtoType? {
        return requestType
    }

    fun responseType(): ProtoType? {
        return responseType
    }

    fun requestStreaming(): Boolean {
        return requestStreaming
    }

    fun responseStreaming(): Boolean {
        return responseStreaming
    }

    fun options(): Options {
        return options
    }

    internal fun link(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        requestType = linker.resolveMessageType(requestTypeElement)
        responseType = linker.resolveMessageType(responseTypeElement)
    }

    internal fun linkOptions(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        options.link(linker)
    }

    internal fun validate(linker: Linker) {
        var linker = linker
        linker = linker.withContext(this)
        linker.validateImport(location(), requestType!!)
        linker.validateImport(location(), responseType!!)
    }

    internal fun retainAll(schema: Schema, markSet: MarkSet): Rpc? {
        if (!markSet.contains(requestType) || !markSet.contains(responseType)) return null
        val result = Rpc(location, name, documentation, requestTypeElement, responseTypeElement,
                requestStreaming, responseStreaming, options.retainAll(schema, markSet))
        result.requestType = requestType
        result.responseType = responseType
        return result
    }

    companion object {

        internal fun fromElements(elements: List<RpcElement>): List<Rpc> {
            val rpcs = List.Builder<Rpc>()
            for (rpcElement in elements) {
                rpcs.add(Rpc(rpcElement.location(), rpcElement.name(), rpcElement.documentation(),
                        rpcElement.requestType(), rpcElement.responseType(),
                        rpcElement.requestStreaming(), rpcElement.responseStreaming(),
                        Options(Options.METHOD_OPTIONS, rpcElement.options())))
            }
            return rpcs.build()
        }

        internal fun toElements(rpcs: List<Rpc>): List<RpcElement> {
            val elements = List.Builder<RpcElement>()
            for (rpc in rpcs) {
                elements.add(RpcElement.builder(rpc.location)
                        .documentation(rpc.documentation)
                        .name(rpc.name)
                        .requestType(rpc.requestTypeElement)
                        .responseType(rpc.responseTypeElement)
                        .requestStreaming(rpc.requestStreaming)
                        .responseStreaming(rpc.responseStreaming)
                        .options(rpc.options.toElements())
                        .build())
            }
            return elements.build()
        }
    }
}
