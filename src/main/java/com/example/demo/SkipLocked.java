package com.example.demo;

import jakarta.persistence.QueryHint;
import org.hibernate.Timeouts;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.data.jpa.repository.QueryHints;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@QueryHints({@QueryHint(name = AvailableSettings.JAKARTA_LOCK_TIMEOUT, value =  Timeouts.SKIP_LOCKED_MILLI + "")
})public @interface SkipLocked {
}
