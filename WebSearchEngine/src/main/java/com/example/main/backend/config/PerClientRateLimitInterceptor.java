package com.example.main.backend.config;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;

public class PerClientRateLimitInterceptor implements HandlerInterceptor {

	  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

	  @Override
	  public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
	      Object handler) throws Exception {
		  
		System.out.println("##### Request intercepted #####");

	    String clientIP = getClientIP(request);
	    System.out.println("IP:"+clientIP);
	    Bucket requestBucket = this.buckets.computeIfAbsent(clientIP, key -> standardBucket());

	    ConsumptionProbe probe = requestBucket.tryConsumeAndReturnRemaining(1);
	    if (probe.isConsumed()) {
	      response.addHeader("X-Rate-Limit-Remaining",
	          Long.toString(probe.getRemainingTokens()));
	      return true;
	    }
	    System.out.println("NO MORE RESOURCES");

	    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
	    response.addHeader("X-Rate-Limit-Retry-After-Milliseconds",
	        Long.toString(TimeUnit.NANOSECONDS.toMillis(probe.getNanosToWaitForRefill())));

	    return false;
	  }

	  private static Bucket standardBucket() {
	    return Bucket4j.builder()
	        .addLimit(Bandwidth.classic(1, Refill.intervally(1, Duration.ofSeconds(1))))
	        .build();
	  }
	  
		public String getClientIP(HttpServletRequest request) {
			String xfHeader = request.getHeader("X-Forwarded-For");
			if (xfHeader == null){
			    return request.getRemoteAddr();
			}
			return xfHeader.split(",")[0]; //
		}

	}