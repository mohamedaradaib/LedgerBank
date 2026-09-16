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

## How It Works
The application follows a simple banking workflow: a user creates an account and logs in with secure credentials, the system validates the session and grants access to the dashboard, and the user can then view balances, deposit funds, withdraw money, or transfer funds between accounts. Every action is recorded as a transaction in the database, and the dashboard presents the current balances and recent activity so the user has a clear view of their financial status. This is implemented using Spring Boot for the backend, Spring Security for authentication, JPA for database access, and Thymeleaf for the web interface.

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

5. Open the app in a browser:
   ```text
   http://localhost:8080
   ```

## Live Demo

## License
MIT License.
