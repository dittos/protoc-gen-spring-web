package org.sapzil.protobuf

import com.google.protobuf.Descriptors

internal const val defaultPackage = ""

fun stripSuffixString(str: String, suffix: String): String {
    return if (str.endsWith(suffix)) {
        str.substring(0, str.length - suffix.length)
    } else {
        str
    }
}

fun stripProto(filename: String): String {
    return if (filename.endsWith(".protodevel")) {
        stripSuffixString(filename, ".protodevel")
    } else {
        stripSuffixString(filename, ".proto")
    }
}

fun fileJavaPackage(file: Descriptors.FileDescriptor, immutable: Boolean): String {
    return if (file.options.hasJavaPackage()) {
        file.options.javaPackage
    } else {
        var result = defaultPackage
        if (file.`package`.isNotEmpty()) {
            if (result.isNotEmpty()) result += '.'
            result += file.`package`
        }
        result
    }
}

fun className(file: Descriptors.FileDescriptor): String {
    return JavaNameResolver().getClassName(file, immutable = true)
}

fun className(descriptor: Descriptors.Descriptor): String {
    return JavaNameResolver().getClassName(descriptor, immutable = true)
}
