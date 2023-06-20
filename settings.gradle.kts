rootProject.name = "mock-web-server-dsl"

dependencyResolutionManagement {
    repositories {
        if (extra.has("internal")) {
            maven(url = "https://bucket.yandex-team.ru/v1/maven/mobile") {
                name = "bucket"
                credentials(PasswordCredentials::class)
            }
        } else {
            mavenCentral()
        }
    }
}
