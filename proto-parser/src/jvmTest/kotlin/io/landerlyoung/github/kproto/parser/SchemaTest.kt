package io.landerlyoung.github.kproto.parser

import com.squareup.wire.schema.SchemaLoader
import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * <pre>
 * Author: landerlyoung@gmail.com
 * Date:   2018-12-31
 * Time:   21:49
 * Life with Passion, Code with Creativity.
 * </pre>
 */

class SchemaTest {
    @Test
    fun testTest() {
        assertTrue(true)
    }

    @Test
    fun loadProtos() {
        val loader = SchemaLoader()
        loader.addSource(File("src/commonTest/resources/proto"))
        loader.addProto("all_types.proto")
        loader.addProto("one_of.proto")

        val schema = loader.load()
        val allTypes = schema.getType("squareup.protos.alltypes.AllTypes")

        assertTrue(schema.protoFiles().isNotEmpty())
    }
}
