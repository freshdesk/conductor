package com.netflix.conductor.rest.filter;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@WebFilter(urlPatterns = "/*")
@Component
public class CustomRequestFilter
        implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomRequestFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        long startTime = System.currentTimeMillis();
        LOGGER.info("Incoming request: " + httpRequest.getMethod() + " " + httpRequest.getRequestURI());

        chain.doFilter(request, response);

        long endTime = System.currentTimeMillis();
        LOGGER.info("Request for " + httpRequest.getRequestURI() + " took " + (endTime - startTime) + " ms.");
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}

