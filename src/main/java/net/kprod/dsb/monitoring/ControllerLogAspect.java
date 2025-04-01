//package net.kprod.dsb.monitoring;
//
//
//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Controller;
//import org.springframework.util.StringUtils;
//import org.springframework.web.bind.annotation.RestController;
//
//@Aspect
//@Component
//public class ControllerLogAspect {
//
//    @Autowired
//    private MonitoringServiceImpl monitoringService;
//
//    @Around("(@annotation(org.springframework.web.bind.annotation.RequestMapping)"
//            + " || @annotation(org.springframework.web.bind.annotation.GetMapping)"
//            + " || @annotation(org.springframework.web.bind.annotation.PostMapping)"
//            + " || @annotation(org.springframework.web.bind.annotation.PutMapping)"
//            + " || @annotation(org.springframework.web.bind.annotation.PatchMapping)"
//            + " || @annotation(org.springframework.web.bind.annotation.PostMapping)"
//            + " || @annotation(net.kprod.dsb.monitoring.MonitoringForce))"
//            + " && !@annotation(net.kprod.dsb.monitoring.MonitoringDisable)")
//    public Object controllerInterceptor(ProceedingJoinPoint joinPoint) throws Throwable {
//        //serviceName is composed of Controller name and Controller method name
//        //get the method name
//        String methodName = joinPoint.getSignature().getName();
//
//        //get the controller name ; it could be either a RestController or a Controller
//        String controllerName = null;
//        Controller controllerAnnotation = joinPoint.getTarget().getClass().getAnnotation(Controller.class);
//        RestController restControllerAnnotation = joinPoint.getTarget().getClass().getAnnotation(RestController.class);
//        if(restControllerAnnotation != null) {
//            controllerName = restControllerAnnotation.value();
//        } else if(controllerAnnotation != null) {
//            controllerName = controllerAnnotation.value();
//        }
//        if (StringUtils.isEmpty(controllerName)) {
//            controllerName = joinPoint.getTarget().getClass().getSimpleName();
//        }
//
//        //keep current time
//        long start = System.currentTimeMillis();
//        //start monitoring
//        monitoringService.start(controllerName, methodName);
//        //execute controller method
//        Object proceed = joinPoint.proceed();
//        //get execution time
//        Long elapsedTime = System.currentTimeMillis() - start;
//
//        monitoringService.end(elapsedTime);
//        return proceed;
//    }
//}