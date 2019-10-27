package org.springframework.tests.beans;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class Test1 {
	public static void main(String[] args) {
		new XmlBeanFactory(new ClassPathResource("beanEvents.xml"));
	}
}
