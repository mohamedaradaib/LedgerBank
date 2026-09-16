# Bank Application

A bank application built with Java, Spring Boot, and MySQL to simulate core financial operations such as account management, deposits, withdrawals, transfers, and transaction tracking.

## Features
- User registration and login
- Secure account access using Spring Security
- Balance tracking for checking and savings accounts
- Deposit and withdrawal operations
- Transfers between account types and between users
- Transaction history viewing
- Password reset workflow

## Tech Stack
- Java 17
- Spring Boot 3
- Spring Security
- Spring Data JPA
- Thymeleaf
- MySQL
- Maven
- Bootstrap

## Getting Started
### Prerequisites
- Java 17+
- Maven
- MySQL

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/mohamedaradaib/Bank.git
   cd Bank
   ```

2. Create the database:
   ```sql
   CREATE DATABASE bankdb;
   ```

3. Update database credentials in `src/main/resources/application.properties` if needed:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/bankdb
   spring.datasource.username=root
   spring.datasource.password=your_password
   ```

4. Run the application:
   ```bash
   ./mvnw spring-boot:run
   ```
   Windows:
   ```bash
   mvnw.cmd spring-boot:run
   ```

5. Open the app in a browser:
   ```text
   http://localhost:8080
   ```

## Live Demo

## License
Distributed under the MIT License.
