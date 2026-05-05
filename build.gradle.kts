plugins {
	java
	id("org.springframework.boot") version "4.0.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.diffplug.spotless") version "8.3.0"
}

group = "com.uj"
version = "0.0.1-SNAPSHOT"
description = "Enterprise Policy Orchestrator backend"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
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
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-flyway")

	annotationProcessor("org.projectlombok:lombok")
	compileOnly("org.projectlombok:lombok")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	runtimeOnly("org.postgresql:postgresql")
	runtimeOnly("org.flywaydb:flyway-database-postgresql")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-test")
	testImplementation("org.springframework.boot:spring-boot-test-autoconfigure")
	testImplementation("org.springframework:spring-test")
	testImplementation("org.testcontainers:testcontainers:2.0.5")
	testImplementation("org.testcontainers:postgresql:1.20.4")
	testImplementation("org.testcontainers:junit-jupiter:1.20.4")

	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

spotless {
	java {
		target("src/**/*.java")
		importOrder()
		removeUnusedImports()
		googleJavaFormat()
	}
}

