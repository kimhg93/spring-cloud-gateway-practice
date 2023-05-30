//package com.practice.gateway.config;
//
//import com.practice.gateway.domain.DynamicRoute;
//import com.practice.gateway.domain.DynamicRouteRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.gateway.route.RouteLocator;
//import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.util.List;
//
//@Slf4j
//@Configuration
//public class RouteConfig {
//
//    @Autowired
//    private DynamicRouteRepository repository;
//
//    @Bean // DB 에서 라우트 정보 조회해서 등록
//    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
//        List<DynamicRoute> routeList = repository.findAll();
//
//        RouteLocatorBuilder.Builder routeLocator = builder.routes();
//        for (DynamicRoute route : routeList) {
//            String path = route.getPath();
//            String id = route.getId();
//            String uri = route.getUri();
//
//            routeLocator
//                    .route(id, r -> r
//                            .path(path)
//                            .uri(uri));
//       }
//
//       return routeLocator.build();
//    }
//}
