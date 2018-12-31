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

import com.squareup.wire.schema.internal.parser.ServiceElement

class Service private constructor(
        val type: ProtoType,
        val location: Location,
        val documentation: String,
        val name: String,
        val rpcs: List<Rpc>,
        val options: Options) {

    /** Returns the RPC named `name`, or null if this service has no such method.  */
    fun rpc(name: String): Rpc? {
        for (rpc in rpcs) {
            if (rpc.name == name) {
                return rpc
            }
        }
        return null
    }

    internal fun link(linker: Linker) {
        var _linker = linker.withContext(this)
        for (rpc in rpcs) {
            rpc.link(_linker)
        }
    }

    internal fun linkOptions(linker: Linker) {
        var _linker = linker.withContext(this)
        for (rpc in rpcs) {
            rpc.linkOptions(_linker)
        }
        options.link(_linker)
    }

    internal fun validate(linker: Linker) {
        val _linker = linker.withContext(this)
        for (rpc in rpcs) {
            rpc.validate(_linker)
        }
    }

    internal fun retainAll(schema: Schema, markSet: MarkSet): Service? {
        // If this service is not retained, prune it.
        if (!markSet.contains(type)) {
            return null
        }

        val retainedRpcs = mutableListOf<Rpc>()
        for (rpc in rpcs) {
            val retainedRpc = rpc.retainAll(schema, markSet)
            if (retainedRpc != null && markSet.contains(ProtoMember.get(type, rpc.name))) {
                retainedRpcs.add(retainedRpc)
            }
        }

        return Service(type, location, documentation, name, retainedRpcs,
                options.retainAll(schema, markSet))
    }

    companion object {

        internal fun fromElement(protoType: ProtoType, element: ServiceElement): Service {
            val rpcs = Rpc.fromElements(element.rpcs)
            val options = Options(Options.SERVICE_OPTIONS, element.options)

            return Service(protoType, element.location, element.documentation, element.name, rpcs,
                    options)
        }

        internal fun fromElements(packageName: String?,
                                  elements: List<ServiceElement>): List<Service> {
            val services = mutableListOf<Service>()
            for (service in elements) {
                val protoType = ProtoType[packageName, service.name]
                services.add(Service.fromElement(protoType, service))
            }
            return services
        }

        internal fun toElements(services: List<Service>): List<ServiceElement> {
            val elements = mutableListOf<ServiceElement>()
            for (service in services) {
                elements.add(ServiceElement(
                        location = service.location,
                        documentation=service.documentation,
                        name=service.name,
                        rpcs=Rpc.toElements(service.rpcs),
                        options=service.options.toElements()))
            }
            return elements
        }
    }
}
