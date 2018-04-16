package uk.gov.pay.adminusers.pact;

import au.com.dius.pact.model.Pact;
import au.com.dius.pact.model.PactSource;
import au.com.dius.pact.provider.junit.InteractionRunner;
import au.com.dius.pact.provider.junit.PactRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

public class PayPactRunner extends PactRunner {
    public PayPactRunner(Class clazz) {
        super(clazz);
    }

    @NotNull
    @Override
    protected InteractionRunner newInteractionRunner(TestClass testClass, Pact pact, PactSource pactSource) {
        try {
            return new PayInteractionRunner(testClass, pact, pactSource);
        } catch (InitializationError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void runChild(InteractionRunner interaction, RunNotifier notifier) {
        super.runChild(interaction, notifier);
    }
}
