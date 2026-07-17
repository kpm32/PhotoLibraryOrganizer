import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.anvar.photolibraryorganizer.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "PhotoLibraryOrganizer"
            packageVersion = "1.0.7"
            description = "Photo archive organizer"

            macOS {
                bundleID = "com.anvar.photolibraryorganizer"
                iconFile.set(project.file("src/main/resources/app-icon.icns"))
            }
        }
    }
}
