/*
 * Copyright 2002-2013 the original author or authors.
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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/**
 * Simple interface for bean definition readers.
 * Specifies load methods with Resource and String location parameters.
 * 指定带有资源和字符串位置参数的加载方法
 * <p>Concrete bean definition readers can of course add additional
 * load and register methods for bean definitions, specific to
 * their bean definition format.
 * 具体的bean定义读者当然可以添加额外的内容
 * 加载和注册bean定义的方法，具体到
 * 他们的bean定义格式
 * <p>Note that a bean definition reader does not have to implement
 * this interface. It only serves as suggestion for bean definition
 * readers that want to follow standard naming conventions.
 * 请注意，bean定义阅读器不必实现
 * 此接口。它只是作为bean定义的建议
 * 希望遵循标准命名约定的读者。
 *
 * @author Juergen Hoeller
 * @see org.springframework.core.io.Resource
 * @since 1.1
 */
public interface BeanDefinitionReader {

    /**
     * Return the bean factory to register the bean definitions with.
     * <p>The factory is exposed through the BeanDefinitionRegistry interface,
     * encapsulating the methods that are relevant for bean definition handling.
     * 返回bean工厂来注册bean定义。
     * 工厂通过BeanDefinitionRegistry接口公开，
     * 封装与bean定义处理相关的方法
     */
    BeanDefinitionRegistry getRegistry();

    /**
     * Return the resource loader to use for resource locations.
     * Can be checked for the <b>ResourcePatternResolver</b> interface and cast
     * accordingly, for loading multiple resources for a given resource pattern.
     * <p>Null suggests that absolute resource loading is not available
     * for this bean definition reader.
     * <p>This is mainly meant to be used for importing further resources
     * from within a bean definition resource, for example via the "import"
     * tag in XML bean definitions. It is recommended, however, to apply
     * such imports relative to the defining resource; only explicit full
     * resource locations will trigger absolute resource loading.
     * <p>There is also a {@code loadBeanDefinitions(String)} method available,
     * for loading bean definitions from a resource location (or location pattern).
     * This is a convenience to avoid explicit ResourceLoader handling.
     * 返回用于资源位置的资源加载程序。
     * 可以检查ResourcePatternResolver接口和强制转换
     * 因此，为一个给定的资源模式加载多个资源。
     * Null表示绝对资源加载不可用
     * 对于这个bean定义阅读器。
     * 这主要是用来导入更多的资源
     * 来自bean定义资源，例如通过“导入”
     * XML bean定义中的标记。但是，建议您使用它
     * 相对于de的此类导入
     *
     * @see #loadBeanDefinitions(String)
     * @see org.springframework.core.io.support.ResourcePatternResolver
     */
    ResourceLoader getResourceLoader();

    /**
     * Return the class loader to use for bean classes.
     * <p>{@code null} suggests to not load bean classes eagerly
     * but rather to just register bean definitions with class names,
     * with the corresponding Classes to be resolved later (or never).
     * 返回用于bean类的类装入器。
     * {@code null}建议不要急于加载bean类
     * 而是用类名注册bean定义，
     * 对应的类将在以后解析(或永远解析)。
     */
    ClassLoader getBeanClassLoader();

    /**
     * Return the BeanNameGenerator to use for anonymous beans
     * (without explicit bean name specified).
     */
    BeanNameGenerator getBeanNameGenerator();


    /**
     * Load bean definitions from the specified resource.
     * 从指定资源加载bean定义。
     *
     * @param resource the resource descriptor
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     */
    int loadBeanDefinitions(Resource resource) throws BeanDefinitionStoreException;

    /**
     * Load bean definitions from the specified resources.
     *
     * @param resources the resource descriptors
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     */
    int loadBeanDefinitions(Resource... resources) throws BeanDefinitionStoreException;

    /**
     * Load bean definitions from the specified resource location.
     * <p>The location can also be a location pattern, provided that the
     * ResourceLoader of this bean definition reader is a ResourcePatternResolver.
     * 从指定的资源位置加载bean定义。
     * 的位置也可以是一个位置模式，前提是
     * 这个bean定义阅读器的ResourceLoader是一个ResourcePatternResolver。
     *
     * @param location the resource location, to be loaded with the ResourceLoader
     *                 (or ResourcePatternResolver) of this bean definition reader
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     * @see #getResourceLoader()
     * @see #loadBeanDefinitions(org.springframework.core.io.Resource)
     * @see #loadBeanDefinitions(org.springframework.core.io.Resource[])
     */
    int loadBeanDefinitions(String location) throws BeanDefinitionStoreException;

    /**
     * Load bean definitions from the specified resource locations.
     *
     * @param locations the resource locations, to be loaded with the ResourceLoader
     *                  (or ResourcePatternResolver) of this bean definition reader
     * @return the number of bean definitions found
     * @throws BeanDefinitionStoreException in case of loading or parsing errors
     */
    int loadBeanDefinitions(String... locations) throws BeanDefinitionStoreException;

}
