package net.jqwik.execution;

import net.jqwik.*;
import net.jqwik.api.lifecycle.*;
import net.jqwik.descriptor.*;
import net.jqwik.support.*;
import org.junit.platform.engine.*;

import java.util.*;
import java.util.stream.*;

public class LifecycleRegistry implements LifecycleSupplier {

	private List<HookRegistration> registrations = new ArrayList<>();
	private Map<Class<? extends LifecycleHook>, LifecycleHook> instances = new HashMap<>();

	@Override
	public TeardownPropertyHook teardownPropertyHook(PropertyMethodDescriptor propertyMethodDescriptor) {
		return new AutoCloseableLifecycle();
	}

	@Override
	public AroundPropertyHook aroundPropertyHook(PropertyMethodDescriptor propertyMethodDescriptor) {
		List<AroundPropertyHook> aroundPropertyHooks = findHooks(propertyMethodDescriptor, AroundPropertyHook.class);
		return combineAroundHooks(aroundPropertyHooks);
	}

	private AroundPropertyHook combineAroundHooks(List<AroundPropertyHook> aroundPropertyHooks) {
		return aroundPropertyHooks.isEmpty() ? AroundPropertyHook.NONE : aroundPropertyHooks.get(0);
	}

	private <T extends LifecycleHook> List<T> findHooks(TestDescriptor descriptor, Class<T> hookType) {
		List<Class<T>> hookClasses = findHookClasses(descriptor, hookType);
		//noinspection unchecked
		return hookClasses.stream().map(hookClass -> (T) instances.get(hookClass)).collect(Collectors.toList());
	}

	private <T extends LifecycleHook> List<Class<T>> findHookClasses(TestDescriptor descriptor, Class<T> hookType) {
		//noinspection unchecked
		return registrations.stream()
			.filter(registration -> registration.match(descriptor))
			.filter(registration -> registration.match(hookType))
			.map(registration -> (Class<T>) registration.hookClass)
			.collect(Collectors.toList());
	}

	public void registerLifecycleHook(TestDescriptor descriptor, Class<? extends LifecycleHook> hookClass) {
		if (JqwikReflectionSupport.isInnerClass(hookClass)) {
			String message = String.format("Inner class [%s] cannot be used as LifecycleHook", hookClass.getName());
			throw new JqwikException(message);
		}
		registrations.add(new HookRegistration(descriptor, hookClass));
		if (!instances.containsKey(hookClass)) {
			LifecycleHook hookInstance = JqwikReflectionSupport.newInstanceWithDefaultConstructor(hookClass);
			instances.put(hookClass, hookInstance);
		}
	}

	private static class HookRegistration {
		private TestDescriptor descriptor;
		private final Class<? extends LifecycleHook> hookClass;

		private HookRegistration(TestDescriptor descriptor, Class<? extends LifecycleHook> hookClass) {
			this.descriptor = descriptor;
			this.hookClass = hookClass;
		}

		public boolean match(TestDescriptor descriptor) {
			return this.descriptor.equals(descriptor);
		}

		public <T extends LifecycleHook> boolean match(Class<? extends LifecycleHook> hookType) {
			return hookType.isAssignableFrom(hookClass);
		}
	}
}

