package org.sapzil.protobuf

import com.google.common.base.CaseFormat
import com.google.protobuf.Descriptors
import com.google.protobuf.ExtensionRegistry
import java.io.StringWriter
import java.io.Writer

class SpringWebGenerator : CodeGenerator() {
    data class Options(
        val context_type: String?
    )

    override fun registerAllExtensions(registry: ExtensionRegistry) {
        AnnotationsProto.registerAllExtensions(registry)
    }

    override fun generate(file: Descriptors.FileDescriptor, parameter: String, generatorContext: GeneratorResponseContext) {
        val parameters = parameter.split(",").associate {
            val kv = it.split("=", limit = 2)
            if (kv.size == 2) {
                kv[0] to kv[1]
            } else {
                kv[0] to true
            }
        }
        val options = Options(
            context_type = parameters["context_type"] as String?
        )

        val packageName = serviceJavaPackage(file)
        val packageFileName = javaPackageToDir(packageName)
        for (service in file.services) {
            val filename = packageFileName + serviceClassName(service) + ".java"
            val output = StringWriter()
            generateService(service, output, options)
            generatorContext.write(filename, output.toString())
        }
    }

    fun generateService(service: Descriptors.ServiceDescriptor, out: Writer, options: Options) {
        val printer = Printer(out)
        val packageName = serviceJavaPackage(service.file)
        if (packageName.isNotEmpty()) {
            printer.print("package $packageName;\n\n")
        }

        printer.print("@javax.annotation.Generated(\"protoc-gen-spring-web\")\n")
        printer.print("public final class ${serviceClassName(service)} {\n\n")
        printer.indent()

        generateInterface(printer, options, service, reactive = false)
        generateInterface(printer, options, service, reactive = true)

        printer.outdent()
        printer.print("}\n")
    }

    fun generateInterface(
        printer: Printer,
        options: Options,
        service: Descriptors.ServiceDescriptor,
        reactive: Boolean
    ) {
        val interfaceName = if (reactive) {
            "ReactiveController"
        } else {
            "Controller"
        }
        printer.print("public interface $interfaceName {\n")
        printer.indent()
        for (method in service.methods) {
            val inputType = className(method.inputType)
            val outputType = if (reactive) {
                "org.reactivestreams.Publisher<${className(method.outputType)}>"
            } else {
                className(method.outputType)
            }
            val lowerMethodName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, method.name)
            val overridesAnnotation = method.options.getExtension(AnnotationsProto.overrides)
            val path = if (overridesAnnotation.path.isNotEmpty()) {
                overridesAnnotation.path
            } else {
                "/${service.fullName}/${method.name}"
            }
            printer.print("\n@org.springframework.web.bind.annotation.PostMapping(\"$path\")\n")
            val contextParameter = if (options.context_type != null) {
                ", @org.springframework.lang.NonNull ${options.context_type} context"
            } else {
                ""
            }
            printer.print("$outputType $lowerMethodName(" +
                "@org.springframework.web.bind.annotation.RequestBody @org.springframework.lang.NonNull $inputType request" +
                contextParameter +
                ");\n")
        }
        printer.outdent()
        printer.print("}\n\n")
    }

    fun serviceJavaPackage(file: Descriptors.FileDescriptor): String {
        val result = className(file)
        return result.substringBeforeLast('.')
    }

    fun serviceClassName(service: Descriptors.ServiceDescriptor): String {
        return service.name + "Rpc"
    }

    fun javaPackageToDir(packageName: String): String {
        if (packageName.isEmpty()) {
            return ""
        }
        return packageName.replace('.', '/') + '/'
    }
}

fun main(args: Array<String>) {
    pluginMain(args, SpringWebGenerator())
}
