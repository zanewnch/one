plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.flow"
version = "0.0.1-SNAPSHOT"
description = "Basic Spring Boot template"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-json")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.xerial:sqlite-jdbc:3.53.4.0")
	runtimeOnly("com.h2database:h2")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

listOf("development", "staging", "production").forEach { profile ->
	val profileTaskSuffix = profile.replaceFirstChar { it.uppercase() }
	val profileResources = layout.buildDirectory.dir("profile-resources/$profile")
	val prepareProfileResources = tasks.register<Copy>("prepare${profileTaskSuffix}Resources") {
		from(sourceSets.main.get().output)
		include("**/*")
		destinationDir = profileResources.get().asFile
		filesMatching("application.yml") {
			filter { line -> line.replace("default: development", "default: $profile") }
		}
	}

	tasks.register<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar$profileTaskSuffix") {
		group = "build"
		description = "Builds the executable JAR with $profile as the default Spring profile."
		archiveClassifier.set(profile)
		dependsOn(prepareProfileResources)
		from(profileResources)
	}
}

listOf("development", "staging", "production").forEach { profile ->
	val profileTaskSuffix = profile.replaceFirstChar { it.uppercase() }
	tasks.register<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun$profileTaskSuffix") {
		group = "application"
		description = "Runs the application with the $profile Spring profile."
		mainClass.set(tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun").get().mainClass)
		classpath = sourceSets.main.get().runtimeClasspath
		args("--spring.profiles.active=$profile")
	}
}
