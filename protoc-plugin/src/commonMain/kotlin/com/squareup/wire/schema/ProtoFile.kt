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
import com.squareup.wire.schema.internal.parser.ProtoFileElement

import com.squareup.wire.schema.Options.FILE_OPTIONS

class ProtoFile private constructor(private val location: Location, private val imports: List<String>,
                                    private val publicImports: List<String>, private val packageName: String, private val types: List<Type>,
                                    private val services: List<Service>, private val extendList: List<Extend>, private val options: Options,
                                    private val syntax: Syntax) {
    private var javaPackage: Any? = null

    internal fun toElement(): ProtoFileElement {
        return ProtoFileElement.builder(location)
                .imports(imports)
                .publicImports(publicImports)
                .packageName(packageName)
                .types(Type.toElements(types))
                .services(Service.toElements(services))
                .extendDeclarations(Extend.toElements(extendList))
                .options(options.toElements())
                .syntax(syntax)
                .build()
    }

    fun location(): Location {
        return location
    }

    internal fun imports(): List<String> {
        return imports
    }

    internal fun publicImports(): List<String> {
        return publicImports
    }

    /**
     * Returns the name of this proto file, like `simple_message` for `squareup/protos/person/simple_message.proto`.
     */
    fun name(): String {
        var result = location().path()

        val slashIndex = result.lastIndexOf('/')
        if (slashIndex != -1) {
            result = result.substring(slashIndex + 1)
        }

        if (result.endsWith(".proto")) {
            result = result.substring(0, result.length - ".proto".length)
        }

        return result
    }

    fun packageName(): String {
        return packageName
    }

    fun javaPackage(): String? {
        return if (javaPackage != null) javaPackage.toString() else null
    }

    fun types(): List<Type> {
        return types
    }

    fun services(): List<Service> {
        return services
    }

    internal fun extendList(): List<Extend> {
        return extendList
    }

    fun options(): Options {
        return options
    }

    /** Returns a new proto file that omits types and services not in `identifiers`.  */
    internal fun retainAll(schema: Schema, markSet: MarkSet): ProtoFile {
        val retainedTypes = List.builder<Type>()
        for (type in types) {
            val retainedType = type.retainAll(schema, markSet)
            if (retainedType != null) {
                retainedTypes.add(retainedType)
            }
        }

        val retainedServices = List.builder<Service>()
        for (service in services) {
            val retainedService = service.retainAll(schema, markSet)
            if (retainedService != null) {
                retainedServices.add(retainedService)
            }
        }

        val result = ProtoFile(location, imports, publicImports, packageName,
                retainedTypes.build(), retainedServices.build(), extendList,
                options.retainAll(schema, markSet), syntax)
        result.javaPackage = javaPackage
        return result
    }

    internal fun linkOptions(linker: Linker) {
        options.link(linker)
        javaPackage = options().get(JAVA_PACKAGE)
    }

    override fun toString(): String {
        return location().path()
    }

    fun toSchema(): String {
        return toElement().toSchema()
    }

    /** Syntax version.  */
    enum class Syntax private constructor(private val string: String) {
        PROTO_2("proto2"),
        PROTO_3("proto3");

        override fun toString(): String {
            return string
        }

        companion object {

            operator fun get(string: String): Syntax {
                for (syntax in values()) {
                    if (syntax.string == string) return syntax
                }
                throw IllegalArgumentException("unexpected syntax: $string")
            }
        }
    }

    companion object {
        internal val JAVA_PACKAGE = ProtoMember.get(FILE_OPTIONS, "java_package")

        internal operator fun get(protoFileElement: ProtoFileElement): ProtoFile {
            val packageName = protoFileElement.packageName()

            val types = Type.fromElements(packageName, protoFileElement.types())

            val services = Service.fromElements(packageName, protoFileElement.services())

            val wireExtends = Extend.fromElements(packageName, protoFileElement.extendDeclarations())

            val options = Options(Options.FILE_OPTIONS, protoFileElement.options())

            return ProtoFile(protoFileElement.location(), protoFileElement.imports(),
                    protoFileElement.publicImports(), packageName, types, services, wireExtends, options,
                    protoFileElement.syntax())
        }
    }
}
