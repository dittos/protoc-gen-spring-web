package org.sapzil.protobuf

import com.google.common.base.CaseFormat
import com.google.protobuf.Descriptors

enum class NameEquality {
    NO_MATCH, EXACT_EQUAL, EQUAL_IGNORE_CASE
}

fun checkNameEquality(a: String, b: String): NameEquality {
    return if (a.toUpperCase() == b.toUpperCase()) {
        if (a == b) {
            NameEquality.EXACT_EQUAL
        } else {
            NameEquality.EQUAL_IGNORE_CASE
        }
    } else {
        NameEquality.NO_MATCH
    }
}

fun messageHasConflictingClassName(message: Descriptors.Descriptor, className: String, equalityMode: NameEquality): Boolean {
    if (checkNameEquality(message.name, className) == equalityMode) {
        return true
    }
    if (message.nestedTypes.any { messageHasConflictingClassName(it, className, equalityMode) }) {
        return true
    }
    if (message.enumTypes.any { checkNameEquality(it.name, className) == equalityMode }) {
        return true
    }
    return false
}

// Strip package name from a descriptor's full name.
// For example:
//   Full name   : foo.Bar.Baz
//   Package name: foo
//   After strip : Bar.Baz
fun stripPackageName(fullName: String, file: Descriptors.FileDescriptor): String {
    return if (file.`package`.isEmpty()) {
        fullName
    } else {
        // Strip package name
        fullName.substring(file.`package`.length + 1)
    }
}

// A suffix that will be appended to the file's outer class name if the name
// conflicts with some other types defined in the file.
const val outerClassNameSuffix = "OuterClass"

class JavaNameResolver {
    /** Caches the result to provide better performance. */
    private val fileImmutableOuterClassNames = mutableMapOf<Descriptors.FileDescriptor, String>()

    fun getFileDefaultImmutableClassName(file: Descriptors.FileDescriptor): String {
        val lastSlash = file.name.lastIndexOf('/')
        val basename = if (lastSlash == -1) {
            file.name
        } else {
            file.name.substring(lastSlash + 1)
        }
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, stripProto(basename))
    }

    fun getFileImmutableClassName(file: Descriptors.FileDescriptor): String {
        return fileImmutableOuterClassNames.computeIfAbsent(file) {
            if (file.options.hasJavaOuterClassname()) {
                file.options.javaOuterClassname
            } else {
                var className = getFileDefaultImmutableClassName(file)
                if (hasConflictingClassName(file, className, NameEquality.EXACT_EQUAL)) {
                    className += outerClassNameSuffix
                }
                className
            }
        }
    }

    fun hasConflictingClassName(file: Descriptors.FileDescriptor, className: String, equalityMode: NameEquality): Boolean {
        if (file.enumTypes.any { checkNameEquality(it.name, className) == equalityMode }) {
            return true
        }
        if (file.services.any { checkNameEquality(it.name, className) == equalityMode }) {
            return true
        }
        if (file.messageTypes.any { messageHasConflictingClassName(it, className, equalityMode) }) {
            return true
        }
        return false
    }

    fun getFileClassName(file: Descriptors.FileDescriptor, immutable: Boolean): String {
        return if (immutable) {
            getFileImmutableClassName(file)
        } else {
            "Mutable" + getFileImmutableClassName(file)
        }
    }

    fun getClassName(descriptor: Descriptors.FileDescriptor, immutable: Boolean): String {
        var result = fileJavaPackage(descriptor, immutable)
        if (result.isNotEmpty()) result += '.'
        result += getFileClassName(descriptor, immutable)
        return result
    }

    fun getClassFullName(nameWithoutPackage: String, file: Descriptors.FileDescriptor, immutable: Boolean, multipleFiles: Boolean): String {
        var result = if (multipleFiles) {
            fileJavaPackage(file, immutable)
        } else {
            getClassName(file, immutable)
        }
        if (result.isNotEmpty()) {
            result += "."
        }
        result += nameWithoutPackage
        return result
    }

    fun getClassName(descriptor: Descriptors.Descriptor, immutable: Boolean): String {
        return getClassFullName(classNameWithoutPackage(descriptor, immutable), descriptor.file, immutable, descriptor.file.options.javaMultipleFiles)
    }

    fun classNameWithoutPackage(descriptor: Descriptors.Descriptor, immutable: Boolean): String {
        return stripPackageName(descriptor.fullName, descriptor.file)
    }
}