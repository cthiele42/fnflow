/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.kafkaservice;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;

/**
 * @author Sajjad Safaeian
 */
@ControllerAdvice
public class ExceptionHandlerController {

    @ExceptionHandler(TopicDoesNotExistException.class)
    public ProblemDetail handleBaseException(TopicDoesNotExistException e, WebRequest request) {
        ProblemDetail problemDetail
                = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());

        if (request instanceof ServletWebRequest servletWebRequest) {
            HttpServletRequest httpRequest = servletWebRequest.getRequest();
            String requestURI = httpRequest.getRequestURI();
            problemDetail.setInstance(URI.create(requestURI));
        } else {
            problemDetail.setInstance(URI.create("unknown-instance"));
        }

        return problemDetail;
    }

}
