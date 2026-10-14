package com.xiaomi.push.service;

import android.app.PendingIntent;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/** SDK notification and timer intents have complete payloads before they are created. */
@Aspect
public class PendingIntentAspect {
    @Around("call(* android.app.PendingIntent.getActivity(android.content.Context, int, android.content.Intent, int)) || " +
            "call(* android.app.PendingIntent.getService(android.content.Context, int, android.content.Intent, int)) || " +
            "call(* android.app.PendingIntent.getBroadcast(android.content.Context, int, android.content.Intent, int))")
    public PendingIntent createImmutable(ProceedingJoinPoint call) throws Throwable {
        Object[] args = call.getArgs();
        args[3] = (Integer) args[3] | PendingIntent.FLAG_IMMUTABLE;
        return (PendingIntent) call.proceed(args);
    }
}
