package com.example;

import com.fasterxml.jackson.core.JsonFactory;
import io.github.fomin.oasgen.ByteBufConverter;
import io.github.fomin.oasgen.UrlEncoderUtils;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRoutes;

public abstract class SimpleRoutes implements Consumer<HttpServerRoutes> {
    private final ByteBufConverter byteBufConverter;
    private final String baseUrl;

    protected SimpleRoutes(JsonFactory jsonFactory, String baseUrl) {
        this.byteBufConverter = new ByteBufConverter(jsonFactory);
        this.baseUrl = baseUrl;
    }

    @Nonnull
    public abstract Mono<java.lang.String> simplePost(@Nonnull Mono<com.example.Dto> requestBodyMono);

    @Nonnull
    public abstract Mono<com.example.Dto> simpleGet(@Nonnull java.lang.String id, @Nonnull java.lang.String param1, @Nullable com.example.Param2OfSimpleGet param2);

    @Override
    public final void accept(HttpServerRoutes httpServerRoutes) {
        httpServerRoutes
            .post(baseUrl + "/path1", (request, response) -> {


                Mono<com.example.Dto> requestMono = byteBufConverter.parse(request.receive(), new com.example.Dto.Parser());
                Mono<java.lang.String> responseMono = simplePost(requestMono);
                return response
                        .header("Content-Type", "application/json")
                        .send(byteBufConverter.write(response, responseMono, io.github.fomin.oasgen.StringConverter.WRITER));
            })
            .get(baseUrl + "/path2/{id}", (request, response) -> {
                Map<String, String> queryParams = UrlEncoderUtils.parseQueryParams(request.uri());
                String param0Str = request.param("id");
                java.lang.String param0 = param0Str != null ? param0Str : null;
                String param1Str = queryParams.get("param1");
                java.lang.String param1 = param1Str != null ? param1Str : null;
                String param2Str = queryParams.get("param2");
                com.example.Param2OfSimpleGet param2 = param2Str != null ? com.example.Param2OfSimpleGet.parseString(param2Str) : null;

                Mono<com.example.Dto> responseMono = simpleGet(param0, param1, param2);
                return response
                        .header("Content-Type", "application/json")
                        .send(byteBufConverter.write(response, responseMono, com.example.Dto.Writer.INSTANCE));
            })
        ;
    }
}
