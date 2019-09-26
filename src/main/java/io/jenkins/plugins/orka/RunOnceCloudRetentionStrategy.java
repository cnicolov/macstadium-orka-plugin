package io.jenkins.plugins.orka;

import hudson.model.Descriptor;
import hudson.model.Executor;
import hudson.model.ExecutorListener;
import hudson.model.Queue;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.CloudRetentionStrategy;
import hudson.slaves.RetentionStrategy;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.DataBoundConstructor;

public class RunOnceCloudRetentionStrategy extends CloudRetentionStrategy implements ExecutorListener {
    private static final Logger LOGGER = Logger.getLogger(RunOnceCloudRetentionStrategy.class.getName());

    private final int idleMinutes;

    @DataBoundConstructor
    public RunOnceCloudRetentionStrategy(int idleMinutes) {
        super(idleMinutes);
        this.idleMinutes = idleMinutes;
    }

    public int getIdleMinutes() {
        return idleMinutes;
    }

    @Override
    public void taskAccepted(final Executor executor, final Queue.Task task) {
    }

    @Override
    public void taskCompleted(final Executor executor, final Queue.Task task, final long durationMS) {
        taskCompleted(executor);
    }

    private void taskCompleted(final Executor executor) {
        final AbstractCloudComputer<?> computer = (AbstractCloudComputer<?>) executor.getOwner();
        final Queue.Executable currentExecutable = executor.getCurrentExecutable();
        LOGGER.log(Level.FINE, "Terminating {0}.Build {1} is finished",
                new Object[] { computer.getName(), currentExecutable });
        taskCompleted(computer);
    }

    private void taskCompleted(final AbstractCloudComputer<?> computer) {
        computer.setAcceptingTasks(false);

        final AbstractCloudSlave computerNode = computer.getNode();
        if (computerNode != null) {
            try {
                computerNode.terminate();
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, "Failed to terminate " + computer.getName(), e);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to terminate " + computer.getName(), e);
            }
        }
    }

    @Override
    public void taskCompletedWithProblems(final Executor executor, final Queue.Task task, final long durationMS,
            final Throwable problems) {
        taskCompleted(executor);
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }

    @Restricted(NoExternalUse.class)
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    public static final class DescriptorImpl extends Descriptor<RetentionStrategy<?>> {
        @Override
        public String getDisplayName() {
            return "Terminate immediately after use";
        }
    }

    private Object readResolve() {
        return new RunOnceCloudRetentionStrategy(idleMinutes);
    }
}