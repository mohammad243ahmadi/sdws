# Secure Digital Wallet System (SDWS)

Spring Boot 3 · Java 17 · Spring Security · JPA · Thymeleaf · H2 (default) / MySQL

## Run
    mvn spring-boot:run
Open http://localhost:8080

Seed admin: phone `0700000000`, password `Admin@12345` (change in `application.properties`).
Register normal users from /register.

## Database
Default is a file-based H2 database in `./data`. To use MySQL, edit `application.properties`.
