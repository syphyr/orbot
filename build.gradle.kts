plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

allprojects {
    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(
            arrayOf(
                "-parameters",
                "-Xlint:deprecation",
                "-Xlint:unchecked"
            )
        )
    }
}
