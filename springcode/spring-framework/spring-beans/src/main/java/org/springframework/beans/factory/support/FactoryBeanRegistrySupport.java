/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.beans.factory.support;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.FactoryBeanNotInitializedException;

/**
 * Support base class for singleton registries which need to handle
 * 支持基类单例注册需要处理,需要FactoryBean实例 {@link org.springframework.beans.factory.FactoryBean}
 * instances, integrated with {@link DefaultSingletonBeanRegistry}'s singleton management.
 *
 * <p>
 * Serves as base class for {@link AbstractBeanFactory}.
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public abstract class FactoryBeanRegistrySupport extends DefaultSingletonBeanRegistry {

	/** Cache of singleton objects created by FactoryBeans: FactoryBean name --> object */
	// 缓存被bean工厂创建的单例
	private final Map<String, Object> factoryBeanObjectCache = new ConcurrentHashMap<String, Object>(
			16);

	/**
	 * Determine the type for the given FactoryBean. 决定被给与的工厂的类型
	 * 
	 * @param factoryBean the FactoryBean instance to check
	 * @return the FactoryBean's object type, or {@code null} if the type cannot be
	 *         determined yet
	 */
	protected Class<?> getTypeForFactoryBean(final FactoryBean<?> factoryBean) {
		try {
			// 用来判断是否能够安全的执行某个操作
			if (System.getSecurityManager() != null) {
				return AccessController.doPrivileged(new PrivilegedAction<Class<?>>() {

					@Override
					public Class<?> run() {
						return factoryBean.getObjectType();
					}
				}, getAccessControlContext());
			}
			else {
				return factoryBean.getObjectType();
			}
		}
		catch (Throwable ex) {
			// Thrown from the FactoryBean's getObjectType implementation.
			logger.warn(
					"FactoryBean threw exception from getObjectType, despite the contract saying "
							+ "that it should return null if the type of its object cannot be determined yet",
					ex);
			return null;
		}
	}

	/**
	 * Obtain an object to expose from the given FactoryBean, if available in cached form.
	 * Quick check for minimal synchronization.
	 * 从给定的FactoryBean公开获取了一个对象,如果可用在缓存的形式。快速检查最小同步。
	 * 
	 * @param beanName the name of the bean
	 * @return the object obtained from the FactoryBean, or {@code null} if not available
	 */
	protected Object getCachedObjectForFactoryBean(String beanName) {
		Object object = this.factoryBeanObjectCache.get(beanName);
		return (object != NULL_OBJECT ? object : null);
	}

	/**
	 * Obtain an object to expose from the given FactoryBean.获取一个对象从给定FactoryBean暴露。
	 * 
	 * @param factory the FactoryBean instance
	 * @param beanName the name of the bean
	 * @param shouldPostProcess whether the bean is subject to post-processing
	 * @return the object obtained from the FactoryBean
	 * @throws BeanCreationException if FactoryBean object creation failed
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	protected Object getObjectFromFactoryBean(FactoryBean<?> factory, String beanName,
			boolean shouldPostProcess) {
		// 工厂是否是单例的以及这个类名称是否存在于单例缓存中
		if (factory.isSingleton() && containsSingleton(beanName)) {
			// 满足上述条件
			synchronized (getSingletonMutex()) {
				// 不知道为啥要通过单例缓存来判断,应该单例缓存和这个类中的factoryBeanObjectCache有关系
				Object object = this.factoryBeanObjectCache.get(beanName);
				if (object == null) {
					// 从传进来的bean工厂中获取
					object = doGetObjectFromFactoryBean(factory, beanName);
					// Only post-process and store if not put there already during
					// getObject() call above
					// (e.g. because of circular reference processing triggered by custom
					// getBean calls)
					Object alreadyThere = this.factoryBeanObjectCache.get(beanName);
					if (alreadyThere != null) {
						object = alreadyThere;
					}
					else {
						if (object != null && shouldPostProcess) {
							try {
								object = postProcessObjectFromFactoryBean(object,
										beanName);
							}
							catch (Throwable ex) {
								throw new BeanCreationException(beanName,
										"Post-processing of FactoryBean's singleton object failed",
										ex);
							}
						}
						this.factoryBeanObjectCache.put(beanName,
								(object != null ? object : NULL_OBJECT));
					}
				}
				return (object != NULL_OBJECT ? object : null);
			}
		}
		else {
			Object object = doGetObjectFromFactoryBean(factory, beanName);
			if (object != null && shouldPostProcess) {
				try {
					object = postProcessObjectFromFactoryBean(object, beanName);
				}
				catch (Throwable ex) {
					throw new BeanCreationException(beanName,
							"Post-processing of FactoryBean's object failed", ex);
				}
			}
			return object;
		}
	}

	/**
	 * Obtain an object to expose from the given FactoryBean.
	 * 
	 * @param factory the FactoryBean instance
	 * @param beanName the name of the bean
	 * @return the object obtained from the FactoryBean
	 * @throws BeanCreationException if FactoryBean object creation failed
	 * @see org.springframework.beans.factory.FactoryBean#getObject()
	 */
	private Object doGetObjectFromFactoryBean(final FactoryBean<?> factory,
			final String beanName) throws BeanCreationException {

		Object object;
		try {
			if (System.getSecurityManager() != null) {
				AccessControlContext acc = getAccessControlContext();
				try {
					object = AccessController.doPrivileged(
							new PrivilegedExceptionAction<Object>() {

								@Override
								public Object run() throws Exception {
									return factory.getObject();
								}
							}, acc);
				}
				catch (PrivilegedActionException pae) {
					throw pae.getException();
				}
			}
			else {
				object = factory.getObject();
			}
		}
		catch (FactoryBeanNotInitializedException ex) {
			throw new BeanCurrentlyInCreationException(beanName, ex.toString());
		}
		catch (Throwable ex) {
			throw new BeanCreationException(beanName,
					"FactoryBean threw exception on object creation", ex);
		}

		// Do not accept a null value for a FactoryBean that's not fully
		// initialized yet: Many FactoryBeans just return null then.
		if (object == null && isSingletonCurrentlyInCreation(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName,
					"FactoryBean which is currently in creation returned null from getObject");
		}
		return object;
	}

	/**
	 * Post-process(后置处理) the given object that has been obtained from the FactoryBean.
	 * 后处理(后置处理)给定的对象已从FactoryBean获得。 The resulting object will get exposed for bean
	 * references. 生成的对象会暴露在bean的引用。
	 * <p>
	 * The default implementation simply returns the given object as-is. Subclasses may
	 * override this, for example, to apply post-processors. * < p >默认实现简单地按原样返回给定的对象。
	 * 例如,*子类会覆盖这个应用后处理器。
	 * 
	 * @param object the object obtained from the FactoryBean.
	 * @param beanName the name of the bean
	 * @return the object to expose
	 * @throws org.springframework.beans.BeansException if any post-processing failed
	 */
	protected Object postProcessObjectFromFactoryBean(Object object, String beanName)
			throws BeansException {
		return object;
	}

	/**
	 * Get a FactoryBean for the given bean if possible. 得到一个给定bean FactoryBean如果可能的话。
	 * 
	 * @param beanName the name of the bean
	 * @param beanInstance the corresponding bean instance
	 * @return the bean instance as FactoryBean
	 * @throws BeansException if the given bean cannot be exposed as a FactoryBean
	 */
	protected FactoryBean<?> getFactoryBean(String beanName, Object beanInstance)
			throws BeansException {
		if (!(beanInstance instanceof FactoryBean)) {
			throw new BeanCreationException(beanName, "Bean instance of type ["
					+ beanInstance.getClass() + "] is not a FactoryBean");
		}
		return (FactoryBean<?>) beanInstance;
	}

	/**
	 * Overridden to clear the FactoryBean object cache as well.
	 */
	@Override
	protected void removeSingleton(String beanName) {
		super.removeSingleton(beanName);
		this.factoryBeanObjectCache.remove(beanName);
	}

	/**
	 * Returns the security context for this bean factory. If a security manager is set,
	 * interaction with the user code will be executed using the privileged of the
	 * security context returned by this method.
	 * 
	 * @see AccessController#getContext() 返回这个bean工厂的安全上下文。如果安全管理器 设置,与用户交互的代码将使用特权执行
	 *      此方法返回的安全上下文。
	 * @see AccessController # getContext ()
	 */
	protected AccessControlContext getAccessControlContext() {
		return AccessController.getContext();
	}

}
