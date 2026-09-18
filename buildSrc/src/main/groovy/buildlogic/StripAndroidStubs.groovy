package buildlogic

import groovy.io.FileType
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Keep the existing compile-time android.* stubs out of packaged library classes. */
abstract class StripAndroidStubs extends DefaultTask {
    @InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    abstract ListProperty<RegularFile> getInputJars()

    @InputFiles @PathSensitive(PathSensitivity.RELATIVE)
    abstract ListProperty<Directory> getInputDirectories()

    @OutputFile
    abstract RegularFileProperty getOutputJar()

    @TaskAction
    void strip() {
        Map<String, byte[]> entries = new TreeMap<>()
        def add = { String name, byte[] bytes ->
            // Unlike the framework stubs, this is a real helper used by AppOpsKit.
            if (!name.startsWith('android/') || name == 'android/app/AppOpsManagerExtender.class') {
                if (entries.putIfAbsent(name, bytes) != null) {
                    throw new GradleException("Duplicate library entry: ${name}")
                }
            }
        }
        inputJars.get().each { input ->
            new ZipFile(input.asFile).withCloseable { zip ->
                zip.entries().each { entry ->
                    if (!entry.directory) {
                        zip.getInputStream(entry).withCloseable { add(entry.name, it.bytes) }
                    }
                }
            }
        }
        inputDirectories.get().each { input ->
            File root = input.asFile
            root.eachFileRecurse(FileType.FILES) { file ->
                add(root.toPath().relativize(file.toPath()).toString().replace('\\', '/'), file.bytes)
            }
        }
        File output = outputJar.get().asFile
        output.parentFile.mkdirs()
        new ZipOutputStream(new FileOutputStream(output)).withCloseable { zip ->
            entries.each { name, bytes ->
                ZipEntry entry = new ZipEntry(name)
                entry.time = 0
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
        }
    }
}
