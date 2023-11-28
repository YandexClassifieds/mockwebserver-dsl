plugins {
    kotlin("jvm") version "1.7.10"
    id("maven-publish")
    id("signing")
}

signing {
   sign(publishing.publications)
   useInMemoryPgpKeys(
       extra["signing.keyId"] as String,
       extra["signing.secretKey"] as String,
       extra["signing.password"] as String
   )
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

group = "com.yandex.classifieds"
version = "1.0.2"

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "mockwebserver-dsl"
            from(components.getByName("java"))
            pom {
                name.set("MockWebServer DSL")
                description.set("MockWebServer DSL is a Kotlin DSL for OkHttp's MockWebServer")
                url.set("https://yandexclassifieds.github.io/mockwebserver-dsl/")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        name.set("Yandex")
                        url.set("http://yandex.com/dev")
                    }
                }
                scm {
                    connection.set("scm:git://github.com/YandexClassifieds/mockwebserver-dsl.git")
                    developerConnection.set("scm:git://github.com/YandexClassifieds/mockwebserver-dsl.git")
                    url.set("https://github.com/YandexClassifieds/mockwebserver-dsl")
                }
            }
        }
    }
    repositories {
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
            credentials(PasswordCredentials::class)
        }
        maven {
            name = "bucket"
            url = uri("https://bucket.yandex-team.ru/v1/maven/yandex_vertis_releases")
	        credentials(PasswordCredentials::class)
        }
    }
}

dependencies {
    api("com.squareup.okhttp3:mockwebserver:4.10.0")

    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
}

tasks.named<Test>("test") {
    //@org.junit.Rule works in JUnit 4
    useJUnit()
}
