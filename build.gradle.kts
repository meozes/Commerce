plugins {
	java
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

fun getGitHash(): String {
	return providers.exec {
		commandLine("git", "rev-parse", "--short", "HEAD")
	}.standardOutput.asText.get().trim()
}

group = "kr.hhplus.be"
version = getGitHash()

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:2024.0.0")
	}
}

dependencies {
	// Spring
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")

	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
	implementation("org.projectlombok:lombok")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// DB
	runtimeOnly("com.mysql:mysql-connector-j")

	// Test
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:mysql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
	implementation("jakarta.annotation:jakarta.annotation-api")
	implementation("jakarta.persistence:jakarta.persistence-api")
	annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
	annotationProcessor("jakarta.persistence:jakarta.persistence-api")
	annotationProcessor("jakarta.annotation:jakarta.annotation-api")
	annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")


}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
}
//
//tasks.withType<JavaCompile> {
//  options.compilerArgs.add("-parameters")
//}

val querydslDir = "$buildDir/generated/querydsl"

sourceSets {
	main {
		java {
			srcDirs(querydslDir)
		}
	}
}

tasks.withType<JavaCompile> {
	options.compilerArgs.add("-parameters")
	options.generatedSourceOutputDirectory.set(file(querydslDir))
}

// clean 태스크와 compileJava 태스크 실행시에만 Q클래스 생성
tasks.named("clean") {
	doLast {
		delete(file(querydslDir))
	}
}

tasks.named("compileJava") {
	inputs.dir("src/main/java")
	outputs.dir(querydslDir)
	doFirst {
		if(file(querydslDir).exists()) {
			delete(querydslDir)
		}
	}
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}