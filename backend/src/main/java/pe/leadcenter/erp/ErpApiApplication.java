package pe.leadcenter.erp;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ErpApiApplication {

    public static void main(String[] args) {
        // UTC everywhere: store and process in UTC; display conversion (America/Lima) is a client
        // concern.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(ErpApiApplication.class, args);
    }
}
