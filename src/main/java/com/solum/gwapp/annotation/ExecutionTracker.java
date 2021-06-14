package com.solum.gwapp.annotation;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ExecutionTracker {


	@Around("@annotation(com.solum.gwapp.annotation.ExecutionTimeTracker)")
	public Object calculateExecutionTime(ProceedingJoinPoint joinPoint) {
		Object object = null;
		long begin = System.currentTimeMillis();
		try {
			object = joinPoint.proceed();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		long end = System.currentTimeMillis();
		log.info("Execution Time Tracker> " + joinPoint.getSignature().getName() + " method of " +   joinPoint.getTarget().getClass().toString() + " taken time : [" + (end - begin) + "] ms");
		return object;

	}
}
