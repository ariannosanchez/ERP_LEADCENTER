package pe.leadcenter.erp;

import org.springframework.boot.SpringApplication;

public class TestErpApiApplication {

    public static void main(String[] args) {
        SpringApplication.from(ErpApiApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
