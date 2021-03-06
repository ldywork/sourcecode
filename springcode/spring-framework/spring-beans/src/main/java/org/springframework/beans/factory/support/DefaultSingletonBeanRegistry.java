/*
 * Copyright 2002-2015 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanCreationNotAllowedException;
import org.springframework.beans.factory.BeanCurrentlyInCreationException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.core.SimpleAliasRegistry;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Generic registry for shared bean instances, implementing the
 * {@link org.springframework.beans.factory.config.SingletonBeanRegistry}.
 * Allows for registering singleton instances that should be shared
 * for all callers of the registry, to be obtained via bean name.
 *
 * <p>Also supports registration of
 * {@link org.springframework.beans.factory.DisposableBean} instances,
 * (which might or might not correspond to registered singletons),
 * to be destroyed on shutdown of the registry. Dependencies between
 * beans can be registered to enforce an appropriate shutdown order.
 *
 * <p>This class mainly serves as base class for
 * {@link org.springframework.beans.factory.BeanFactory} implementations,
 * factoring out the common management of singleton bean instances. Note that
 * the {@link org.springframework.beans.factory.config.ConfigurableBeanFactory}
 * interface extends the {@link SingletonBeanRegistry} interface.
 *
 * <p>Note that this class assumes neither a bean definition concept
 * nor a specific creation process for bean instances, in contrast to
 * {@link AbstractBeanFactory} and {@link DefaultListableBeanFactory}
 * (which inherit from it). Can alternatively also be used as a nested
 * helper to delegate to.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #registerSingleton
 * @see #registerDisposableBean
 * @see org.springframework.beans.factory.DisposableBean
 * @see org.springframework.beans.factory.config.ConfigurableBeanFactory
 */
public class DefaultSingletonBeanRegistry extends SimpleAliasRegistry implements SingletonBeanRegistry {

	/**
	 * Internal(内部的) marker for a null singleton object:
	 * used as marker value for concurrent Maps (which don't support null values).
	 */
	protected static final Object NULL_OBJECT = new Object();


	/** //内部标记为一个空的单例对象： 并发 Maps( 不支持空值 )作为标志值。   */
	protected final Log logger = LogFactory.getLog(getClass());

	/** Cache of singleton objects: bean name --> bean instance */
	//是存放singleton对象的缓存  
	private final Map<String, Object> singletonObjects = new ConcurrentHashMap<String, Object>(64);

	/** Cache of singleton factories: bean name --> ObjectFactory */
	// 是存放制造singleton的工厂对象的缓存  
	private final Map<String, ObjectFactory<?>> singletonFactories = new HashMap<String, ObjectFactory<?>>(16);

	/** Cache of early singleton objects: bean name --> bean instance */
	//是存放singletonFactory 制造出来的 singleton 的缓存早期单例对象缓存
	private final Map<String, Object> earlySingletonObjects = new HashMap<String, Object>(16);
	//以上三个缓存是这个类存放单例bean的主要Map 

	/** Set of registered singletons, containing the bean names in registration order */
	//就是单例注册表
	private final Set<String> registeredSingletons = new LinkedHashSet<String>(64);

	/** Names of beans that are currently in creation */
	//目前正在创建中的单例bean的名称的集合
	private final Set<String> singletonsCurrentlyInCreation =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

	/** Names of beans currently excluded from in creation checks */
	private final Set<String> inCreationCheckExclusions =
			Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>(16));

	/** List of suppressed Exceptions, available for associating related causes */
	//存放异常出现的相关的原因的集合
	private Set<Exception> suppressedExceptions;

	/** Flag that indicates whether we're currently within destroySingletons */
	//标志，指示我们目前是否在销毁单例中
	private boolean singletonsCurrentlyInDestruction = false;

	/** Disposable bean instances: bean name --> disposable instance */
	//存放一次性bean的缓存
	private final Map<String, Object> disposableBeans = new LinkedHashMap<String, Object>();

	/** Map between containing bean names: bean name --> Set of bean names that the bean contains */
	//外部bean与被包含在外部bean的所有内部bean集合包含关系的缓存  
	private final Map<String, Set<String>> containedBeanMap = new ConcurrentHashMap<String, Set<String>>(16);

	/** Map between dependent bean names: bean name --> Set of dependent bean names */
	//指定bean与依赖指定bean的所有bean的依赖关系的缓存  
	private final Map<String, Set<String>> dependentBeanMap = new ConcurrentHashMap<String, Set<String>>(64);

	/** Map between depending bean names: bean name --> Set of bean names for the bean's dependencies */
	//指定bean与创建这个bean所需要依赖的所有bean的依赖关系的缓存
	private final Map<String, Set<String>> dependenciesForBeanMap = new ConcurrentHashMap<String, Set<String>>(64);


	@Override
	public void registerSingleton(String beanName, Object singletonObject) throws IllegalStateException {
		Assert.notNull(beanName, "'beanName' must not be null");
		synchronized (this.singletonObjects) {
			Object oldObject = this.singletonObjects.get(beanName);
			if (oldObject != null) {
				throw new IllegalStateException("Could not register object [" + singletonObject +
						"] under bean name '" + beanName + "': there is already object [" + oldObject + "] bound");
			}
			addSingleton(beanName, singletonObject);
		}
	}

	/**
	 * Add the given singleton object to the singleton cache of this factory.
	 * <p>To be called for eager registration of singletons.
	 * @param beanName the name of the bean
	 * @param singletonObject the singleton object
	 */
	protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			 // 因为singletonObjects类型是ConcurrentHashMap,并发Map不支持空值作为标志值，所以用NULL_OBJECT来代替  
			this.singletonObjects.put(beanName, (singletonObject != null ? singletonObject : NULL_OBJECT));
			// beanName已被注册存放在singletonObjects缓存，那么singletonFactories不应该再持有名称为beanName的工厂  
			this.singletonFactories.remove(beanName);
			// beanName已被注册存放在singletonObjects缓存，那么earlySingletonObjects不应该再持有名称为beanName的bean。
			this.earlySingletonObjects.remove(beanName);
			 // beanName放进单例注册表中 
			this.registeredSingletons.add(beanName);
		}
	}

	/**
	 * Add the given singleton factory for building the specified singleton
	 * if necessary.
	 * <p>To be called for eager registration of singletons, e.g. to be able to
	 * resolve circular references.
	 * @param beanName the name of the bean
	 * @param singletonFactory the factory for the singleton object
	 */
	protected void addSingletonFactory(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(singletonFactory, "Singleton factory must not be null");
		synchronized (this.singletonObjects) {
			 // 判断singletonObjects内名字为beanName是否被占用，若没有，进行注册操作 
			if (!this.singletonObjects.containsKey(beanName)) {
				this.singletonFactories.put(beanName, singletonFactory);
				this.earlySingletonObjects.remove(beanName);
				this.registeredSingletons.add(beanName);
			}
		}
	}

    // SingletonBeanRegistry接口的getSingleton方法的实现  
	@Override
	public Object getSingleton(String beanName) {
		return getSingleton(beanName, true);
	}

	/**
	 * Return the (raw) singleton object registered under the given name.
	 * <p>Checks already instantiated singletons and also allows for an early
	 * reference to a currently created singleton (resolving a circular reference).
	 * @param beanName the name of the bean to look for
	 * @param allowEarlyReference whether early references should be created or not
	 * @return the registered singleton object, or {@code null} if none found
	 */
	protected Object getSingleton(String beanName, boolean allowEarlyReference) {
		//获取单例对象的实体类
		Object singletonObject = this.singletonObjects.get(beanName);
		// 如果singletonObjects指定beanName的对象是不存在的  
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
				//从单例工厂创建的早期的单例对象中获取
				singletonObject = this.earlySingletonObjects.get(beanName);
				// 如果earlySingletonObjects指定的beanName的对象是不存在的且allowEarlyReference是允许的  
				if (singletonObject == null && allowEarlyReference) {
					//获取bean工厂
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
						 // singletonFactory创建指定的单例对象  
						singletonObject = singletonFactory.getObject();
						//从bean工厂获取对象然后放到早期单例缓存
						this.earlySingletonObjects.put(beanName, singletonObject);
						// 这里表示指定的beanName已被占用，所以要在singletonFactories移除该名称  
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return (singletonObject != NULL_OBJECT ? singletonObject : null);
	}

	/**
	 * Return the (raw) singleton object registered under the given name,
	 * creating and registering a new one if none registered yet.
	 * @param beanName the name of the bean
	 * @param singletonFactory the ObjectFactory to lazily create the singleton
	 * with, if necessary
	 * @return the registered singleton object
	 */
	public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
		Assert.notNull(beanName, "'beanName' must not be null");
		synchronized (this.singletonObjects) {
			// 如果singetonObjects缓存不存在名称为beanName的对象  
			Object singletonObject = this.singletonObjects.get(beanName);
			if (singletonObject == null) {
				//是否在销毁的单例中,默认为false
				if (this.singletonsCurrentlyInDestruction) {
					throw new BeanCreationNotAllowedException(beanName,
							"Singleton bean creation not allowed while the singletons of this factory are in destruction " +
							"(Do not request a bean from a BeanFactory in a destroy method implementation!)");
				}
				//如果目前是debug模式,打印日志
				if (logger.isDebugEnabled()) {
					logger.debug("Creating shared instance of singleton bean '" + beanName + "'");
				}
				 // 单例对象创建前的回调,默认正在创建的单例 ,判断当前的单例是否是在正在创建的阶段,如果不是抛出异常 
				beforeSingletonCreation(beanName);
				boolean newSingleton = false;
				 // 判断存储异常相关原因的集合是否已存在  
				boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
				// 若没有，刚创建异常集合的实例  
				if (recordSuppressedExceptions) {
					this.suppressedExceptions = new LinkedHashSet<Exception>();
				}
				try {
					 // 由参数给定的singletonFactory创建singleton对象,getObject方法的具体实现由ObjectFactory的子类决定
					singletonObject = singletonFactory.getObject();
					newSingleton = true;
				}
				catch (IllegalStateException ex) {
					// Has the singleton object implicitly appeared in the meantime ->
					// if yes, proceed with it since the exception indicates that state.
					singletonObject = this.singletonObjects.get(beanName);
					if (singletonObject == null) {
						throw ex;
					}
				}
				catch (BeanCreationException ex) {
					 // 如果异常被抓取，在这里将出现异常的原因抛出  
					if (recordSuppressedExceptions) {
						for (Exception suppressedException : this.suppressedExceptions) {
							ex.addRelatedCause(suppressedException);
						}
					}
					throw ex;
				}
				finally {
					// 结束前，将异常集合销毁掉  
					if (recordSuppressedExceptions) {
						this.suppressedExceptions = null;
					}
					// 单例创建之后的回调,默认的实现标志单例不要在创建了。
					afterSingletonCreation(beanName);
				}
				if (newSingleton) {
					// 注册创建后的单例  
					addSingleton(beanName, singletonObject);
				}
			}
			return (singletonObject != NULL_OBJECT ? singletonObject : null);
		}
	}

	/**
	 * Register an Exception that happened to get suppressed during the creation of a
	 * singleton bean instance, e.g. a temporary circular reference resolution problem.
	 * @param ex the Exception to register
	 * 注册 发生在singeton bean 实例创建之间发生的异常  
	 */
	protected void onSuppressedException(Exception ex) {
		synchronized (this.singletonObjects) {
			if (this.suppressedExceptions != null) {
				this.suppressedExceptions.add(ex);
			}
		}
	}

	/**
	 * Remove the bean with the given name from the singleton cache of this factory,
	 * to be able to clean up eager registration of a singleton if creation failed.
	 * @param beanName the name of the bean
	 * @see #getSingletonMutex()
	  *移除名称为beanName的单例,主要在四个集合中移除， 
	  * 如singletonObjects,singletonFactories,earlySingletonObjects 
     * ,registeredSingletons 
	 */
	protected void removeSingleton(String beanName) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.remove(beanName);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.remove(beanName);
		}
	}

	@Override
	public boolean containsSingleton(String beanName) {
		return this.singletonObjects.containsKey(beanName);
	}

	@Override
	public String[] getSingletonNames() {
		// 对singletonObjects加锁，可能是为了防止registeredSingletons和singletonObjects出现不一致的问题  
		synchronized (this.singletonObjects) {
			return StringUtils.toStringArray(this.registeredSingletons);
		}
	}

	@Override
	public int getSingletonCount() {
		synchronized (this.singletonObjects) {
			return this.registeredSingletons.size();
		}
	}

//删除还是添加当前正在创建的beanName
	public void setCurrentlyInCreation(String beanName, boolean inCreation) {
		Assert.notNull(beanName, "Bean name must not be null");
		if (!inCreation) {
			this.inCreationCheckExclusions.add(beanName);
		}
		else {
			this.inCreationCheckExclusions.remove(beanName);
		}
	}

	public boolean isCurrentlyInCreation(String beanName) {
		Assert.notNull(beanName, "Bean name must not be null");
		return (!this.inCreationCheckExclusions.contains(beanName) && isActuallyInCreation(beanName));
	}

	protected boolean isActuallyInCreation(String beanName) {
		return isSingletonCurrentlyInCreation(beanName);
	}

	/**
	 * Return whether the specified singleton bean is currently in creation
	 * (within the entire factory).
	 * @param beanName the name of the bean
	 */
	public boolean isSingletonCurrentlyInCreation(String beanName) {
		return this.singletonsCurrentlyInCreation.contains(beanName);
	}

	/**
	 * Callback before singleton creation.
	 * <p>The default implementation register the singleton as currently in creation.
	 * @param beanName the name of the singleton about to be created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void beforeSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.add(beanName)) {
			throw new BeanCurrentlyInCreationException(beanName);
		}
	}

	/**
	 * Callback after singleton creation.
	 * <p>The default implementation marks the singleton as not in creation anymore.
	 * @param beanName the name of the singleton that has been created
	 * @see #isSingletonCurrentlyInCreation
	 */
	protected void afterSingletonCreation(String beanName) {
		if (!this.inCreationCheckExclusions.contains(beanName) && !this.singletonsCurrentlyInCreation.remove(beanName)) {
			throw new IllegalStateException("Singleton '" + beanName + "' isn't currently in creation");
		}
	}


	/**
	 * Add the given bean to the list of disposable beans in this registry.
	 * <p>Disposable beans usually correspond to registered singletons,
	 * matching the bean name but potentially being a different instance
	 * (for example, a DisposableBean adapter for a singleton that does not
	 * naturally implement Spring's DisposableBean interface).
	 * @param beanName the name of the bean
	 * @param bean the bean instance
	 * 一次性bean注册，存放在disponsableBeans集合中 
	 */
	
	public void registerDisposableBean(String beanName, DisposableBean bean) {
		synchronized (this.disposableBeans) {
			this.disposableBeans.put(beanName, bean);
		}
	}

	/**
	 * Register a containment relationship between two beans,
	 * e.g. between an inner bean and its containing outer bean.
	 * <p>Also registers the containing bean as dependent on the contained bean
	 * in terms of destruction order.
	 * @param containedBeanName the name of the contained (inner) bean
	 * @param containingBeanName the name of the containing (outer) bean
	 * @see #registerDependentBean
	 * 注册两个bean之间的包含关系,
	 *如之间的一种内在bean及其包含外bean。
	 */
	public void registerContainedBean(String containedBeanName, String containingBeanName) {
		// A quick check for an existing entry upfront, avoiding synchronization...
		// 从containedBeanMap缓存中查找外部bean名为containingBeanName的内部bean集合  
		Set<String> containedBeans = this.containedBeanMap.get(containingBeanName);
		if (containedBeans != null && containedBeans.contains(containedBeanName)) {
			return;
		}
		 // 如果没有，刚新建一个存放内部bean的集合，并且存放在containedBeanMap缓存中  
		// No entry yet -> fully synchronized manipulation of the containedBeans Set
		synchronized (this.containedBeanMap) {
			containedBeans = this.containedBeanMap.get(containingBeanName);
			if (containedBeans == null) {
				containedBeans = new LinkedHashSet<String>(8);
				// 将名为containedBeanName的内部bean存放到内部bean集合  
				this.containedBeanMap.put(containingBeanName, containedBeans);
			}
			//这里先添加集合后用集合添加对象
			containedBeans.add(containedBeanName);
		}
		registerDependentBean(containedBeanName, containingBeanName);
	}

	/**
	 * Register a dependent bean for the given bean,
	 * to be destroyed before the given bean is destroyed.
	 * 注册给定bean的一个依赖bean，给定的bean销毁之前被销毁。
	 * @param beanName the name of the bean
	 * @param dependentBeanName the name of the dependent bean
	 */
	public void registerDependentBean(String beanName, String dependentBeanName) {
		// A quick check for an existing entry upfront, avoiding synchronization...
		 // 调用SimpleAliasRegistry的canonicalName方法，这方法是将参数beanName当做别名寻找到注册名，并依此递归  
		String canonicalName = canonicalName(beanName);
		// 从dependentBeanMap缓存中找到依赖名为canonicalName这个bean的 依赖bean集合  
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans != null && dependentBeans.contains(dependentBeanName)) {
			return;
		}

		// No entry yet -> fully synchronized manipulation of the dependentBeans Set
		synchronized (this.dependentBeanMap) {
			dependentBeans = this.dependentBeanMap.get(canonicalName);
			if (dependentBeans == null) {
				dependentBeans = new LinkedHashSet<String>(8);
				this.dependentBeanMap.put(canonicalName, dependentBeans);
			}
			dependentBeans.add(dependentBeanName);
		}
		synchronized (this.dependenciesForBeanMap) {
			// 从dependenciesForBeanMap缓存中找到dependentBeanName要依赖的所有bean集合  两个刚好相反
			Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(dependentBeanName);
			if (dependenciesForBean == null) {
				dependenciesForBean = new LinkedHashSet<String>(8);
				this.dependenciesForBeanMap.put(dependentBeanName, dependenciesForBean);
			}
			dependenciesForBean.add(canonicalName);
		}
	}

	/**
	 * Determine whether the specified dependent bean has been registered as
	 * dependent on the given bean or on any of its transitive dependencies.
	 * @param beanName the name of the bean to check
	 * @param dependentBeanName the name of the dependent bean
	 * @since 4.0
	 */
	protected boolean isDependent(String beanName, String dependentBeanName) {
		return isDependent(beanName, dependentBeanName, null);
	}
//	确定是否还存在名为beanName的被依赖关系 
	private boolean isDependent(String beanName, String dependentBeanName, Set<String> alreadySeen) {
		String canonicalName = canonicalName(beanName);
		//这里是为了递归调用,判断是否是自己依赖自己
		if (alreadySeen != null && alreadySeen.contains(beanName)) {
			return false;
		}
		Set<String> dependentBeans = this.dependentBeanMap.get(canonicalName);
		if (dependentBeans == null) {
			return false;
		}
		if (dependentBeans.contains(dependentBeanName)) {
			return true;
		}
		for (String transitiveDependency : dependentBeans) {
			if (alreadySeen == null) {
				alreadySeen = new HashSet<String>();
			}
			alreadySeen.add(beanName);
			//递归判断每一个依赖以第一个参数bean的bean是否被第二个bean依赖,从n层依赖的bean中判断有没有到达依赖bean的路径
			if (isDependent(transitiveDependency, dependentBeanName, alreadySeen)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine whether a dependent bean has been registered for the given name.
	 * @param beanName the name of the bean to check
	 */
	protected boolean hasDependentBean(String beanName) {
		return this.dependentBeanMap.containsKey(beanName);
	}

	/**
	 * Return the names of all beans which depend on the specified bean, if any.
	 * @param beanName the name of the bean
	 * @return the array of dependent bean names, or an empty array if none
	 */
	//返回所有依赖这个入参bean的集合
	public String[] getDependentBeans(String beanName) {
		Set<String> dependentBeans = this.dependentBeanMap.get(beanName);
		if (dependentBeans == null) {
			return new String[0];
		}
		return StringUtils.toStringArray(dependentBeans);
	}

	/**
	 * Return the names of all beans that the specified bean depends on, if any.
	 * @param beanName the name of the bean
	 * @return the array of names of beans which the bean depends on,
	 * or an empty array if none
	 */
	public String[] getDependenciesForBean(String beanName) {
		Set<String> dependenciesForBean = this.dependenciesForBeanMap.get(beanName);
		if (dependenciesForBean == null) {
			return new String[0];
		}
		return dependenciesForBean.toArray(new String[dependenciesForBean.size()]);
	}

	public void destroySingletons() {
		if (logger.isDebugEnabled()) {
			logger.debug("Destroying singletons in " + this);
		}
		 // 单例目前销毁标志开始  
		synchronized (this.singletonObjects) {
			this.singletonsCurrentlyInDestruction = true;
		}

		String[] disposableBeanNames;
		  // 销毁disponsableBeans缓存中所有单例bean  
		synchronized (this.disposableBeans) {
			disposableBeanNames = StringUtils.toStringArray(this.disposableBeans.keySet());
		}
		for (int i = disposableBeanNames.length - 1; i >= 0; i--) {
			destroySingleton(disposableBeanNames[i]);
		}

		this.containedBeanMap.clear();
		this.dependentBeanMap.clear();
		this.dependenciesForBeanMap.clear();

		synchronized (this.singletonObjects) {
			this.singletonObjects.clear();
			this.singletonFactories.clear();
			this.earlySingletonObjects.clear();
			this.registeredSingletons.clear();
			this.singletonsCurrentlyInDestruction = false;
		}
	}

	/**
	 * Destroy the given bean. Delegates to {@code destroyBean}
	 * if a corresponding disposable bean instance is found.
	 * @param beanName the name of the bean
	 * @see #destroyBean
	 */
	//销毁bean
	public void destroySingleton(String beanName) {
		// Remove a registered singleton of the given name, if any.
		//从单例缓存/单例bean工厂,早期单例bean工厂创建bean/单例集合中删除这个bean
		removeSingleton(beanName);

		// Destroy the corresponding DisposableBean instance.
		DisposableBean disposableBean;
		synchronized (this.disposableBeans) {
			//一次性bean集合中删除这个bean
			disposableBean = (DisposableBean) this.disposableBeans.remove(beanName);
		}
		destroyBean(beanName, disposableBean);
	}

	/**
	 * Destroy the given bean. Must destroy beans that depend on the given
	 * bean before the bean itself. Should not throw any exceptions.
	 * @param beanName the name of the bean
	 * @param bean the bean instance to destroy
	 */
	protected void destroyBean(String beanName, DisposableBean bean) {
		// Trigger destruction of dependent beans first...
		//删除依赖这个bean的bean
		Set<String> dependencies = this.dependentBeanMap.remove(beanName);
		if (dependencies != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Retrieved dependent beans for bean '" + beanName + "': " + dependencies);
			}
			//递归调用删除依赖bean
			for (String dependentBeanName : dependencies) {
				destroySingleton(dependentBeanName);
			}
		}

		// Actually destroy the bean now...
		if (bean != null) {
			try {
				bean.destroy();
			}
			catch (Throwable ex) {
				logger.error("Destroy method on bean with name '" + beanName + "' threw an exception", ex);
			}
		}

		// Trigger destruction of contained beans...
		//删除bean的包含类的信息
		Set<String> containedBeans = this.containedBeanMap.remove(beanName);
		if (containedBeans != null) {
			for (String containedBeanName : containedBeans) {
				destroySingleton(containedBeanName);
			}
		}

		// Remove destroyed bean from other beans' dependencies.
		//删除依赖关系
		synchronized (this.dependentBeanMap) {
			for (Iterator<Map.Entry<String, Set<String>>> it = this.dependentBeanMap.entrySet().iterator(); it.hasNext();) {
				Map.Entry<String, Set<String>> entry = it.next();
				Set<String> dependenciesToClean = entry.getValue();
				dependenciesToClean.remove(beanName);
				if (dependenciesToClean.isEmpty()) {
					it.remove();
				}
			}
		}

		// Remove destroyed bean's prepared dependency information.
		this.dependenciesForBeanMap.remove(beanName);
	}

	/**
	 * Exposes the singleton mutex to subclasses and external collaborators.
	 * <p>Subclasses should synchronize on the given Object if they perform
	 * any sort of extended singleton creation phase. In particular, subclasses
	 * should <i>not</i> have their own mutexes involved in singleton creation,
	 * to avoid the potential for deadlocks in lazy-init situations.
	 */
	public final Object getSingletonMutex() {
		return this.singletonObjects;
	}

}
