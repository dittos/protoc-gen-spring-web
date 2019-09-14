package org.sapzil.protobuf

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.google.protobuf.ExtensionRegistry
import com.google.protobuf.compiler.PluginProtos

abstract class CodeGenerator {
    open fun registerAllExtensions(registry: ExtensionRegistry) {}

    fun generateAll(files: List<Descriptors.FileDescriptor>, parameter: String, generatorContext: GeneratorResponseContext) {
        for (file in files) {
            generate(file, parameter, generatorContext)
        }
    }

    abstract fun generate(file: Descriptors.FileDescriptor, parameter: String, generatorContext: GeneratorResponseContext)
}

class GeneratorResponseContext(
    val response: PluginProtos.CodeGeneratorResponse.Builder,
    val parsedFiles: List<Descriptors.FileDescriptor>
) {
    fun write(filename: String, content: String) {
        val file = response.addFileBuilder()
        file.name = filename
        file.content = content
    }
}

class DescriptorPool(private val fileMap: Map<String, DescriptorProtos.FileDescriptorProto>) {
    val pool = mutableMapOf<String, Descriptors.FileDescriptor>()

    fun descriptorFor(proto: DescriptorProtos.FileDescriptorProto): Descriptors.FileDescriptor {
        return pool.computeIfAbsent(proto.name) {
            val deps = proto.dependencyList.map { descriptorFor(fileMap[it]!!) }
            Descriptors.FileDescriptor.buildFrom(proto, deps.toTypedArray())
        }
    }

    fun get(fileName: String): Descriptors.FileDescriptor? {
        return fileMap[fileName]?.let { descriptorFor(it) }
    }
}

fun generateCode(request: PluginProtos.CodeGeneratorRequest, generator: CodeGenerator): PluginProtos.CodeGeneratorResponse {
    val fileMap = request.protoFileList.associateBy { it.name }
    val pool = DescriptorPool(fileMap)
    for (file in request.protoFileList) {
        pool.descriptorFor(file)
    }

    val parsedFiles = request.fileToGenerateList.map {
        requireNotNull(pool.get(it)) {
            "protoc asked plugin to generate a file but did not provide a descriptor for the file: $it"
        }
    }
    val context = GeneratorResponseContext(PluginProtos.CodeGeneratorResponse.newBuilder(), parsedFiles)
    generator.generateAll(parsedFiles, request.parameter, context)
    return context.response.build()
}

fun pluginMain(args: Array<String>, generator: CodeGenerator) {
    val registry = ExtensionRegistry.newInstance()
    generator.registerAllExtensions(registry)
    val request = PluginProtos.CodeGeneratorRequest.parseFrom(System.`in`, registry)
    val response = generateCode(request, generator)
    response.writeTo(System.out)
}
