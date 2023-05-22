package com.practice.gateway.controller;

import com.practice.gateway.domain.DynamicRoute;
import com.practice.gateway.domain.DynamicRouteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class RouteController {

    private final DynamicRouteRepository routeRepository;
    private final RouteLocator routeLocator;


    @GetMapping("/refresh")
    public void refreshRoutes() {
        String url = "/actuator/gateway/refresh";

        WebClient.create(url)
                .post()
                .retrieve()
                .bodyToFlux(Map.class);
    }

    @PostMapping("/update")
    public void updateRoute(@RequestBody DynamicRoute dynamicRoute){
        Optional<DynamicRoute> optionalEntity = routeRepository.findById(dynamicRoute.getId());
        if(optionalEntity.isPresent()){
            dynamicRoute = optionalEntity.get();

            dynamicRoute.setPath("");

            routeRepository.save(dynamicRoute);
        } else {
            throw new EntityNotFoundException();
        }

    }

    @GetMapping("/route/{id}")
    public Mono<ResponseEntity<String>> getRouteById(@PathVariable String id) {
        return routeLocator.getRoutes()
                .filter(route -> id.equals(route.getId()))
                .next()
                .map(route -> {
                    // 라우트 정보 출력
                    System.out.println(route);
                    return ResponseEntity.ok("Route found");
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Route not found"));
    }



    @GetMapping("/add")
    public void add(){
        List<DynamicRoute> routeList = routeRepository.findAll();

        for(DynamicRoute route : routeList){

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            body.add("uri", route.getId());

            // 프리디케이트 설정
            MultiValueMap<String, Object> predicate = new LinkedMultiValueMap<>();
            predicate.add("name", "Path");

            MultiValueMap<String, Object> predicateArgs = new LinkedMultiValueMap<>();
            predicateArgs.add("pattern", route.getPath() + "/**");
            predicate.add("args", predicateArgs);

            body.add("predicates", predicate);

            MultiValueMap<String, Object> filter = new LinkedMultiValueMap<>();
            filter.add("[RewritePath "+route.getPath()+"/(?<segment>.*) = '/${segment}']", "order = 0");
            body.add("filters", filter);

            WebClient.create()
                    .post()
                    .uri("/actuator/gateway/routes/{id}", route.getId())
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .header(HttpHeaders.ACCEPT_CHARSET, StandardCharsets.UTF_8.toString())
                    .bodyValue(body)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .subscribe();

        }

    }

}