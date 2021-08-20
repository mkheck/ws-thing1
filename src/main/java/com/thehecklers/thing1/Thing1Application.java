package com.thehecklers.thing1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.stereotype.Controller;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;

import java.time.Instant;

@SpringBootApplication
public class Thing1Application {

    public static void main(String[] args) {
        Hooks.onErrorDropped(err -> System.out.println("Disconnecting, " + err.getLocalizedMessage()));
        SpringApplication.run(Thing1Application.class, args);
    }

	@Bean
    RSocketRequester requester(RSocketRequester.Builder builder) {
		return builder.tcp("localhost", 7635);
	}

//    @Bean
//    WebClient client() {
//        return WebClient.create("http://localhost:7634");
//    }
}

@Controller
@AllArgsConstructor
class Thing1Controller {
//    private final WebClient client;
    private final RSocketRequester requester;

    // Request/stream
    @MessageMapping("aircraft")
    Flux<Aircraft> getAircraft(Mono<Instant> instantMono) {
        return instantMono.doOnNext(ts -> System.out.println("⏰ " + ts))
                .thenMany(requester.route("acstream")
                        .data(Instant.now())
                        .retrieveFlux(Aircraft.class));
    }

    // Bidirectional channel
    @MessageMapping("channel")
    Flux<Aircraft> channel(Flux<Weather> weatherFlux) {
        return weatherFlux.doOnSubscribe(subs -> System.out.println("SUBSCRIBED TO WEATHER!"))
                .doOnNext(wx -> System.out.println("☀️ " + wx))
                .switchMap(wx -> requester.route("acstream")
                        .data(Instant.now())
                        .retrieveFlux(Aircraft.class));
    }
}

@Data
@AllArgsConstructor
class Weather {
    private Instant when;
    private String observation;
}

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class Aircraft {
    private String callsign, reg, flightno, type;
    private int altitude, heading, speed;
    private double lat, lon;
}