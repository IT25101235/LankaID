package lk.sliit.identityissuanceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class IdentityIssuanceServiceApplication {

    public static void main(String[] args) {
        // Booking dates, slot times and the 72-hour rule all use the server clock, so pin it to Sri Lanka time.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Colombo"));
        SpringApplication.run(IdentityIssuanceServiceApplication.class, args);
    }

}
