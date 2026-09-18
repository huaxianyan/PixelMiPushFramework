import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ScopedArtifacts
import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

abstract class WeaveSdkTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputJars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDirectories: ListProperty<Directory>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sdkInput: RegularFileProperty

    @get:Classpath
    abstract val compileLibraries: ConfigurableFileCollection

    @get:Classpath
    abstract val androidLibraries: ConfigurableFileCollection

    @get:Classpath
    abstract val compiler: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputJar: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun weave() {
        val projectClasses = inputJars.get().map { it.asFile } + inputDirectories.get().map { it.asFile }
        val inpath = (projectClasses + sdkInput.get().asFile).joinToString(File.pathSeparator)
        val aspectpath = projectClasses.joinToString(File.pathSeparator)
        val classpath = (compileLibraries.files + androidLibraries.files + projectClasses)
            .joinToString(File.pathSeparator)
        outputJar.get().asFile.parentFile.mkdirs()
        execOperations.javaexec {
            classpath(compiler)
            mainClass.set("org.aspectj.tools.ajc.Main")
            args("-11", "-showWeaveInfo", "-inpath", inpath, "-aspectpath", aspectpath,
                "-classpath", classpath, "-outjar", outputJar.get().asFile.absolutePath)
        }.assertNormalExitValue()
    }
}

val mipushLib = file("libs/miuipushsdkshared_3_7_9.jar")
extra["mipushLib"] = mipushLib

android {
    namespace = "com.nihility.mipush_hook"
    compileSdk = rootProject.extra["compileSdkVersion"] as Int

    defaultConfig {
        minSdk = rootProject.extra["minSdkVersion"] as Int

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

}

val aspectCompiler by configurations.creating
val aspectjVersion = "1.9.7"

androidComponents.onVariants(androidComponents.selector().all()) { variant ->
    val weaveTask = tasks.register<WeaveSdkTask>("weave${variant.name.replaceFirstChar { it.uppercaseChar() }}Sdk") {
        sdkInput.set(mipushLib)
        // Use AGP's resolved class JARs, not the raw AAR dependencies.
        compileLibraries.from(variant.compileClasspath)
        androidLibraries.from(androidComponents.sdkComponents.bootClasspath)
        compiler.from(aspectCompiler)
    }
    variant.artifacts.forScope(ScopedArtifacts.Scope.PROJECT).use(weaveTask).toTransform(
        ScopedArtifact.CLASSES, WeaveSdkTask::inputJars,
        WeaveSdkTask::inputDirectories, WeaveSdkTask::outputJar)
}

dependencies {
    compileOnly(files(mipushLib))
    // The public handler interfaces expose AspectJ types to the application module.
    api("org.aspectj:aspectjrt:$aspectjVersion")
    aspectCompiler("org.aspectj:aspectjtools:$aspectjVersion")
    implementation("androidx.startup:startup-runtime:1.1.1")

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}