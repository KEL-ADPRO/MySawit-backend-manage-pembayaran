package com.mysawit.pembayaran;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class PembayaranApplication {

    public static void main(String[] args) {
        SpringApplication.run(PembayaranApplication.class, args);
    }
}
