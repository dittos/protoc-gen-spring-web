package org.sapzil.protobuf

import java.io.Writer

class Printer(private val out: Writer) {
    private var indent = ""

    fun indent() {
        indent += "  "
    }

    fun outdent() {
        indent = indent.dropLast(2)
    }

    fun print(str: String) {
        for (line in str.lineSequence()) {
            if (line.isNotEmpty()) {
                out.write(indent + line)
            } else {
                out.write("\n")
            }
        }
    }
}
