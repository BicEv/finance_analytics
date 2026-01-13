package ru.bicev.finance_analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling
public class FinanceAnalyticsApplication {

	public static void main(String[] args) {
		setProperties();
		SpringApplication.run(FinanceAnalyticsApplication.class, args);
	}

	private static void setProperties() {
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
		System.setProperty("jwt.secret", dotenv.get("JWT_SECRET", ""));
		System.setProperty("jwt.expiration", dotenv.get("JWT_EXPIRATION", "P1D"));
		System.setProperty("spring.datasource.url", dotenv.get("DB_URL", ""));
		System.setProperty("spring.datasource.username", dotenv.get("DB_USERNAME", ""));
		System.setProperty("spring.datasource.password", dotenv.get("DB_PASSWORD", ""));
		System.setProperty("app.frontend.redirect-url", dotenv.get("REDIRECT_URL", ""));
		System.setProperty("spring.security.oauth2.client.registration.google.client-id", dotenv.get("GOOGLE_CLIENT_ID"));
		System.setProperty("spring.security.oauth2.client.registration.google.client-secret",
				dotenv.get("GOOGLE_CLIENT_SECRET"));
		System.setProperty("spring.security.oauth2.client.registration.github.client-id", dotenv.get("GITHUB_CLIENT_ID"));
		System.setProperty("spring.security.oauth2.client.registration.github.client-secret",
				dotenv.get("GITHUB_CLIENT_SECRET"));
	}

}
