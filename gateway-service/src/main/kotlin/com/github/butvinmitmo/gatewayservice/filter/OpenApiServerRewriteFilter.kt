package com.github.butvinmitmo.gatewayservice.filter

import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets

/**
 * Gateway filter that removes auto-generated server URLs from OpenAPI specs.
 * This ensures "Try it out" requests go through the gateway using relative URLs.
 */
@Component
class OpenApiServerRewriteFilter :
    GlobalFilter,
    Ordered {
    override fun filter(
        exchange: ServerWebExchange,
        chain: GatewayFilterChain,
    ): Mono<Void> {
        val path = exchange.request.path.value()

        // Only process OpenAPI docs requests
        if (!path.startsWith("/v3/api-docs/")) {
            return chain.filter(exchange)
        }

        val originalResponse = exchange.response
        val bufferFactory = originalResponse.bufferFactory()

        val decoratedResponse =
            object : ServerHttpResponseDecorator(originalResponse) {
                override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> =
                    if (body is Flux) {
                        val modifiedBody =
                            DataBufferUtils.join(body).map { dataBuffer ->
                                val content = dataBuffer.toString(StandardCharsets.UTF_8)
                                DataBufferUtils.release(dataBuffer)

                                val modifiedContent = removeServersFromJson(content)
                                bufferFactory.wrap(modifiedContent.toByteArray(StandardCharsets.UTF_8))
                            }
                        super.writeWith(modifiedBody)
                    } else {
                        super.writeWith(body)
                    }
            }

        return chain.filter(exchange.mutate().response(decoratedResponse).build())
    }

    private fun removeServersFromJson(json: String): String {
        // Remove the "servers" array from OpenAPI JSON
        // Pattern matches: "servers":[...],  or  "servers":[...]  at end
        return json
            .replace(Regex(""""servers"\s*:\s*\[[^\]]*\]\s*,\s*"""), "")
            .replace(Regex(""",\s*"servers"\s*:\s*\[[^\]]*\]"""), "")
    }

    override fun getOrder() = Ordered.HIGHEST_PRECEDENCE + 1
}
