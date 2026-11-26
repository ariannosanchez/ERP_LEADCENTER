package archfixture.badmodule.domain;

import archfixture.badmodule.application.BadHandler;
import archfixture.badmodule.infrastructure.out.BadAdapterOut;
import org.springframework.stereotype.Component;

/** Test-only fixture that intentionally violates the domain rules. */
@Component
@jakarta.persistence.Entity
public class BadDomain {
    BadHandler handler;
    BadAdapterOut adapter;
}
