plugins {
	java
	jacoco
	id("org.springframework.boot") version "3.5.10"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.sonarqube") version "7.2.2.6593"
}

group = "com.mysawit"
version = "0.0.1-SNAPSHOT"
description = "Demo project for Spring Boot"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")

	developmentOnly("org.springframework.boot:spring-boot-devtools")
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	runtimeOnly("org.postgresql:postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.register<Test>("unitTest") {
	description = "Runs the unit tests."
	group = "verification"
	filter { excludeTestsMatching("*FunctionalTest") }
}

tasks.register<Test>("functionalTest") {
	description = "Runs the functional tests."
	group = "verification"
	filter { includeTestsMatching("*FunctionalTest") }
}

tasks.test {
	filter { excludeTestsMatching("*FunctionalTest") }
	finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required.set(true)
	}
}

sonar {
	properties {
		property("sonar.projectKey", "KEL-ADPRO_MySawit-backend-manage-pembayaran")
		property("sonar.organization", "kel-adpro")
		property("sonar.host.url", "https://sonarcloud.io")
		property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacocoTestReport.xml")
	}
}
