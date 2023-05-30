package com.practice.gateway.controller;

import com.practice.gateway.domain.DynamicRoute;
import com.practice.gateway.domain.DynamicRouteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class RouteController {
    // GatewayLegacyControllerEndpoint, AbstractGatewayControllerEndpoint,  GatewayControllerEndpoint 참고
    // actuator/gateway/ 로직을 처리하는 객체

    private final RouteLocator routeLocator;
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher publisher;
    private final DynamicRouteRepository dynamicRouteRepository;


    // DB에서 모든 라우트 정보를 조회 하여 세팅
    // yml 이나 bean 등록으로 생성된 라우트 정보는 수정 불가
    @GetMapping("/init")
    public Mono<ResponseEntity<String>> initRoutes() {
        List<DynamicRoute> routeList = dynamicRouteRepository.findAll();

        Flux<Route> routes = routeLocator.getRoutes();

        return routes.count().flatMap(count -> {
            if (count > 0) return Mono.just(ResponseEntity.ok().body("already init"));
            else {
                List<Mono<String>> rds = new ArrayList<>();
                for (DynamicRoute route : routeList) {
                    RouteDefinition rd = makeRoute(route);
                    save(rd);
                    rds.add(Mono.just(rd.toString()));
                }
                return Mono.zip(rds, e -> e)
                        .map(r -> ResponseEntity.ok().body(Arrays.toString(r)));
            }
        });
    }


    // 특정 라우트의 정보를 DB 에서 재조회하여 저장
    @GetMapping("/update/{id}")
    public Mono<ResponseEntity<String>> updateById(@PathVariable String id){
        RouteDefinition route = makeRoute(dynamicRouteRepository.findById(id).get());
        routeDefinitionWriter.delete(Mono.just(route.getId()))
                .doOnSuccess(r -> refresh());

        return routeDefinitionWriter.save(Mono.just(route))
                .doOnSuccess(s -> refresh())
                .map(s -> ResponseEntity.ok().body(route.toString()))
                .defaultIfEmpty(ResponseEntity.ok().body(route.toString()));
    }


    // 특정 라우트 정보 조회
    @GetMapping("/route/{id}")
    public Mono<ResponseEntity<String>> getRouteById(@PathVariable String id) {
        return routeLocator.getRoutes()
                .filter(route -> id.equals(route.getId()))
                .next()
                .map(route -> {
                    System.out.println(route);
                    return ResponseEntity.ok().body(route.toString());
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Route not found"));
    }

    // 특정 라우트를 삭제
    @GetMapping("/delete/{id}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable String id) {
        return routeDefinitionWriter.delete(Mono.just(id))
                .then(Mono.defer(() -> Mono.just(ResponseEntity.ok().build())))
                .doOnSuccess(r -> {
                    refresh();
                    log.info("Route removed " + dynamicRouteRepository.findById(id).get().toString());
                }).doOnError(e -> e.printStackTrace());
    }

    // 전체 라우트 정보 조회
    @GetMapping("/routes")
    public Mono<ResponseEntity<String>> getAllRoutes() {
        Flux<Route> routes = routeLocator.getRoutes();
        return routes.collectList()
                .map(r -> {
                    log.info("Route : " + r.toString());
                    return ResponseEntity.ok().body(r.toString());
                });
    }

    // 라우트 정보를 추가하고 refresh (액추에이터 코드 참고)
    public void save(String id, RouteDefinition route) {
        Mono.just(route)
                .flatMap(routeDefinition -> routeDefinitionWriter.save(
                        Mono.just(routeDefinition).map(r -> {
                            r.setId(id);
                            return r;
                        })
                )).doOnSuccess(r -> {
                    refresh();
                    log.info("Route saved : " + route);
                }).doOnError(e -> e.printStackTrace())
                .subscribe();
    }

    // save 간소화
    public void save(RouteDefinition routeDefinition){
        routeDefinitionWriter.save(Mono.just(routeDefinition))
                .doOnSuccess(r -> {
                    refresh();
                    log.info("Route saved : " + routeDefinition.toString());
                }).doOnError(e -> e.printStackTrace())
                .subscribe();
    }

    public void refresh() {
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    // DynamicRoute 에서 데이터 추출하여 RouteDefinition 리턴
    public RouteDefinition makeRoute(DynamicRoute dynamicRoute){
        RouteDefinition rd = new RouteDefinition();

        rd.setId(dynamicRoute.getId());
        rd.setUri(URI.create(dynamicRoute.getUri()));

        PredicateDefinition pd = new PredicateDefinition();
        pd.setName("Path");
        pd.addArg("pattern", dynamicRoute.getPath());
        rd.setPredicates(Collections.singletonList(pd));

        return rd;

        /*
        1.RewritePath : /test 로 접속했을 때 / 로 접속, /test/test 로 접속했을 때 /test
        FilterDefinition rewritePathFilter = new FilterDefinition();
        rewritePathFilter.setName("RewritePath");
        rewritePathFilter.addArg("regexp", "/test/(?<segment>.*)");
        rewritePathFilter.addArg("replacement", "/${segment}");
        routeDefinition.setFilters(Collections.singletonList(rewritePathFilter));

        2. StripPrefix : filters: - StripPrefix=2 를 주면 value 값에 따라 prefix를 제거해줌
            value 가 2 라면, /test/test 접속 시 / , /test/test/test 접속 시 /test
        FilterDefinition stripPrefixFilter = new FilterDefinition();
        stripPrefixFilter.setName("StripPrefix");
        stripPrefixFilter.addArg("parts", "2");
        routeDefinition.setFilters(Collections.singletonList(stripPrefixFilter));

        3. setPath : /test/test/{segment} 를 /test/{segment} 변경해줌
            /test/test/HelloWorld 접속 시 /test/HelloWorld
        FilterDefinition setPathFilter = new FilterDefinition();
        setPathFilter.setName("SetPath");
        setPathFilter.addArg("template", "/test/{segment}");
        routeDefinition.setFilters(Collections.singletonList(setPathFilter));
         */
    }


}
