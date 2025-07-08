package net.kprod.mn.monitoring;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice
class GlobalDefaultExceptionHandler {
  public static final String DEFAULT_ERROR_VIEW = "error";

  @ExceptionHandler(value = Exception.class)
  public String
  defaultErrorHandler(HttpServletRequest req, Exception e) throws Exception {
    // If the exception is annotated with @ResponseStatus rethrow it and let the framework handle it
    // AnnotationUtils is a Spring Framework utility class.
    System.err.println("ERROR url " + req.getRequestURL());
    e.printStackTrace();

    if (org.springframework.core.annotation.AnnotationUtils.findAnnotation(e.getClass(), ResponseStatus.class) != null) {
      throw e;
    }

    // Otherwise setup and send the user to a default error-view.
    return DEFAULT_ERROR_VIEW;
  }
}