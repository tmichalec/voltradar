package sk.brutech.voltradar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(basePackages = "sk.brutech.voltradar.persistence.repository")
public class VoltRadarApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoltRadarApplication.class, args);
    }
}
