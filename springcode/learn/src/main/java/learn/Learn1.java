package learn;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;

public class Learn1 {
	public static void main(String[] args) {
		BeanFactory bf = new XmlBeanFactory(new ClassPathResource("applicationContext.xml"));
		Object o = bf.getBean("testBean");
		System.out.println(o);
	}

}
